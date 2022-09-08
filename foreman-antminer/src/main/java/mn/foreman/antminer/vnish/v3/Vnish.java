package mn.foreman.antminer.vnish.v3;

import mn.foreman.model.AbstractMiner;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;
import mn.foreman.model.miners.FanInfo;
import mn.foreman.model.miners.MinerStats;
import mn.foreman.model.miners.Pool;
import mn.foreman.model.miners.asic.Asic;
import mn.foreman.util.PoolUtils;

import java.math.BigDecimal;
import java.util.List;

/** Utility for querying vnish miners. */
public class Vnish
        extends AbstractMiner {

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /** The password. */
    private final String password;

    /** Whether a test. */
    private final boolean test;

    /**
     * Constructor.
     *
     * @param apiIp         The API ip.
     * @param apiPort       The API port.
     * @param password      The password.
     * @param configuration The configuration.
     * @param test          Whether a test.
     */
    public Vnish(
            final String apiIp,
            final int apiPort,
            final String password,
            final ApplicationConfiguration configuration,
            final boolean test) {
        super(
                apiIp,
                apiPort,
                new VnishMacStrategy(
                        apiIp,
                        apiPort,
                        password,
                        configuration));
        this.password = password;
        this.configuration = configuration;
        this.test = test;
    }

    @Override
    protected void addStats(final MinerStats.Builder statsBuilder)
            throws MinerException {
        try {
            final VnishV3Api.Context context =
                    VnishV3Api.login(
                            this.apiIp,
                            this.test ? 8080 : 80,
                            this.password,
                            this.configuration)
                            .orElseThrow(() -> new MinerException("Failed to authenticate"));
            final VnishV3Api.Summary summary =
                    VnishV3Api.summary(context)
                            .orElseThrow(() -> new MinerException("Failed to obtain stats"));

            final FanInfo.Builder fanBuilder = new FanInfo.Builder();
            if (!"immers".equals(summary.miner.cooling.settings.mode.name)) {
                fanBuilder.setCount(summary.miner.cooling.fanNum);
                if (summary.miner.cooling.fanNum > 0) {
                    summary.miner.cooling.fans
                            .stream()
                            .mapToInt(fan -> fan.rpm)
                            .forEach(fanBuilder::addSpeed);
                }
            } else {
                fanBuilder.setCount(0);
            }
            fanBuilder.setSpeedUnits("RPM");

            final Asic.Builder builder =
                    new Asic.Builder()
                            .setHashRate(
                                    new BigDecimal(summary.miner.instantHashrate).multiply(
                                            BigDecimal.valueOf(1000L * 1000 * 1000 * 1000)))
                            .setBoards(
                                    summary
                                            .miner
                                            .chains
                                            .stream()
                                            .filter(chain -> chain.hashrateRt > 0)
                                            .count())
                            .setPowerMode(
                                    "mining".equals(summary
                                            .miner
                                            .status
                                            .state)
                                            ? Asic.PowerMode.NORMAL
                                            : Asic.PowerMode.SLEEPING)
                            .setFanInfo(fanBuilder.build())
                            .setMinerType(summary.miner.minerType)
                            .setCompileTime(summary.miner.compileTime)
                            .setLiquidCooling(Boolean.toString("immers".equals(summary.miner.cooling.settings.mode.name)))
                            .setPower(Integer.toString((int) (summary.miner.powerUsage * 1000)))
                            .setControlBoard(toControlBoard(summary.miner.minerType));

            summary
                    .miner
                    .chains
                    .stream()
                    .map(chain -> chain.pcbTempSensor)
                    .flatMap(List::stream)
                    .map(sensor -> sensor.temp)
                    .forEach(builder::addTemp);
            summary
                    .miner
                    .chains
                    .stream()
                    .map(chain -> chain.chipTempSensor)
                    .flatMap(List::stream)
                    .map(sensor -> sensor.temp)
                    .forEach(builder::addTemp);

            statsBuilder.addAsic(builder.build());

            summary
                    .miner
                    .pools
                    .stream()
                    .map(pool ->
                            new Pool.Builder()
                                    .setName(PoolUtils.sanitizeUrl(pool.url))
                                    .setWorker(pool.worker)
                                    .setStatus(
                                            true,
                                            "working".equals(pool.status) || "active".equals(pool.status))
                                    .setPriority(pool.priority)
                                    .setCounts(
                                            pool.accepted,
                                            pool.rejected,
                                            pool.stale)
                                    .build())
                    .forEach(statsBuilder::addPool);
        } catch (final MinerException me) {
            throw me;
        } catch (final Exception e) {
            throw new MinerException(e);
        }
    }

    /**
     * Returns the control board version.
     *
     * @param type The type.
     *
     * @return The control board version.
     */
    private static String toControlBoard(final String type) {
        if (type.contains("BB")) {
            return "bb";
        } else if (type.contains("AML")) {
            return "amlogic";
        } else {
            return "xilinx";
        }
    }
}
