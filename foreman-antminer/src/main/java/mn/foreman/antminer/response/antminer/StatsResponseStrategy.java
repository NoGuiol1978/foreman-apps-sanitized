package mn.foreman.antminer.response.antminer;

import mn.foreman.antminer.PowerModeStrategy;
import mn.foreman.cgminer.Context;
import mn.foreman.cgminer.ContextKey;
import mn.foreman.cgminer.PoolsResponseStrategy;
import mn.foreman.cgminer.ResponseStrategy;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.cgminer.response.CgMinerResponse;
import mn.foreman.model.miners.FanInfo;
import mn.foreman.model.miners.MinerStats;
import mn.foreman.model.miners.asic.Asic;
import mn.foreman.util.RateUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link PoolsResponseStrategy} provides a {@link ResponseStrategy}
 * implementation that's capable of parsing a {@link CgMinerCommand#STATS}
 * response from an antminer.
 */
public class StatsResponseStrategy
        implements ResponseStrategy {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(StatsResponseStrategy.class);

    /** The context. */
    private final Context context;

    /** The {@link PowerModeStrategy}. */
    private final PowerModeStrategy powerModeStrategy;

    /**
     * Constructor.
     *
     * @param context           The context.
     * @param powerModeStrategy The {@link PowerModeStrategy}.
     */
    public StatsResponseStrategy(
            final Context context,
            final PowerModeStrategy powerModeStrategy) {
        this.context = context;
        this.powerModeStrategy = powerModeStrategy;
    }

    @Override
    public void processResponse(
            final MinerStats.Builder builder,
            final CgMinerResponse response) {
        if (response.hasValues()) {
            final List<Map<String, String>> data =
                    response.getValues()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getKey().equals("STATS"))
                            .map(Map.Entry::getValue)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
            // Add type
            data
                    .stream()
                    .filter(map -> map.containsKey("Type"))
                    .findFirst()
                    .ifPresent(map -> {
                        this.context.addSimple(
                                ContextKey.MINER_TYPE,
                                map.getOrDefault("Type", ""));
                        this.context.addSimple(
                                ContextKey.COMPILE_TIME,
                                map.getOrDefault("CompileTime", ""));
                        if (map.containsKey("BMMiner")) {
                            this.context.addSimple(
                                    ContextKey.BMMINER,
                                    map.getOrDefault("BMMiner", ""));
                        }
                    });

            // Get stats
            if (data.stream().anyMatch(map -> map.containsKey("GHS 5s"))) {
                data
                        .stream()
                        .filter(value -> value.containsKey("GHS 5s"))
                        .forEach(value ->
                                addAsicStats(
                                        builder,
                                        value,
                                        this.context));
            } else {
                // No stats - maybe sleeping?
                addAsicStats(
                        builder,
                        Collections.emptyMap(),
                        this.context);
            }
        } else {
            LOG.debug("No ACICs founds");
        }
    }

    /**
     * Utility method to convert the provided values, which contain per-ASIC
     * metrics, to a {@link Asic} and adds it to the provided builder.
     *
     * @param builder The builder to update.
     * @param values  The asic values.
     * @param context The context.
     */
    private void addAsicStats(
            final MinerStats.Builder builder,
            final Map<String, String> values,
            final Context context) {
        double hashRate = 0;
        if (values.containsKey("GHS 5s")) {
            hashRate = Double.parseDouble(values.get("GHS 5s")) * Math.pow(1000, 3);
        }

        // Some Z11s have different hash rates
        if (values.getOrDefault("ID", "").contains("ZCASH") &&
                hashRate < 1000) {
            hashRate *= 1000;
        }

        final Asic.Builder asicBuilder =
                new Asic.Builder();

        // Boards
        int boardCount = -1;
        if (values.containsKey("miner_count")) {
            boardCount = Integer.parseInt(values.get("miner_count"));
            asicBuilder.setBoards(boardCount);
        }

        // Fans
        final FanInfo.Builder fanBuilder =
                new FanInfo.Builder()
                        .setCount(values.getOrDefault("fan_num", "0"))
                        .setSpeedUnits("RPM");
        for (int i = 1; i <= 8; i++) {
            if (values.containsKey("fan" + i)) {
                fanBuilder.addSpeed(values.get("fan" + i));
            }
        }
        asicBuilder.setFanInfo(fanBuilder.build());

        // Take chip and PCB temps before defaults
        final List<String> tempPrefixes;
        if (values.containsKey("temp_pcb1")) {
            tempPrefixes =
                    Arrays.asList(
                            "temp_pcb",
                            "temp_chip");
        } else {
            tempPrefixes =
                    Arrays.asList(
                            "temp",
                            "temp2_",
                            "temp3_",
                            "temp4_");
        }
        for (final String prefix : tempPrefixes) {
            for (int i = 1; i <= 32; i++) {
                if (values.containsKey(prefix + i)) {
                    asicBuilder.addTemp(values.get(prefix + i));
                }
            }
        }

        // Errors
        boolean hasErrors = false;
        for (int i = 1; i <= 16; i++) {
            final String chain = values.get("chain_acs" + i);
            if (chain != null) {
                hasErrors = (hasErrors || chain.contains("x"));
            }
        }

        asicBuilder.hasErrors(hasErrors);
        this.powerModeStrategy.setPowerMode(
                asicBuilder,
                values,
                hashRate,
                boardCount,
                hasErrors,
                this.context);

        final String rateIdeal = values.get("total_rateideal");
        final String rateUnit =
                values.getOrDefault(
                        "rate_unit",
                        RateUnit.GHS.getUnit());
        if (rateIdeal != null && !rateIdeal.isEmpty() &&
                rateUnit != null && !rateUnit.isEmpty()) {
            asicBuilder.setHashRateIdeal(
                    RateUnit.toUnit(rateUnit).getMultiplier() *
                            Double.parseDouble(rateIdeal));
        }

        // Might have consumption data
        int power = 0;
        boolean hasPower = false;
        for (int i = 1; i <= 6; i++) {
            if (values.containsKey("chain_consumption" + i) || values.containsKey("consumption" + i)) {
                hasPower = true;
                power += Double.parseDouble(
                        values.getOrDefault(
                                "chain_consumption" + i,
                                values.get("consumption" + i)));
            }
        }
        if (hasPower) {
            asicBuilder.setPower(power);
        }

        // Context data
        this.context.getSimple(ContextKey.MRR_RIG_ID)
                .ifPresent(asicBuilder::setMrrRigId);
        this.context.getMulti(ContextKey.RAW_STATS)
                .ifPresent(asicBuilder::addRawStats);
        this.context.getSimple(ContextKey.MINER_TYPE)
                .ifPresent(asicBuilder::setMinerType);
        this.context.getSimple(ContextKey.COMPILE_TIME)
                .ifPresent(asicBuilder::setCompileTime);
        this.context.getSimple(ContextKey.BMMINER)
                .ifPresent(asicBuilder::setBmminer);

        builder.addAsic(
                asicBuilder
                        .setHashRate(hashRate)
                        .build());

        this.context.clear();
    }
}