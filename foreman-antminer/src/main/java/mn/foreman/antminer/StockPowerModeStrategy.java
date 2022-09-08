package mn.foreman.antminer;

import mn.foreman.cgminer.Context;
import mn.foreman.cgminer.ContextKey;
import mn.foreman.io.Query;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.miners.asic.Asic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Determines the power mode for a stock Antminer. */
public class StockPowerModeStrategy
        implements PowerModeStrategy {

    /** The mapper for this class. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The ip. */
    private final String ip;

    /** The password. */
    private final String password;

    /** The port. */
    private final int port;

    /** The realm. */
    private final String realm;

    /** The username. */
    private final String username;

    /**
     * Constructor.
     *
     * @param ip                       The ip.
     * @param port                     The port.
     * @param realm                    The realm.
     * @param username                 The username.
     * @param password                 The password.
     * @param applicationConfiguration The configuration.
     */
    public StockPowerModeStrategy(
            final String ip,
            final int port,
            final String realm,
            final String username,
            final String password,
            final ApplicationConfiguration applicationConfiguration) {
        this.ip = ip;
        this.port = port;
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public void setPowerMode(
            final Asic.Builder builder,
            final Map<String, String> values,
            final double hashRate,
            final int boardCount,
            final boolean hasErrors,
            final Context context) {
        boolean reallyHasErrors = hasErrors;
        Asic.PowerMode powerMode = Asic.PowerMode.NORMAL;

        if (hasErrors && hashRate == 0D && boardCount > 0 && isSeer(values)) {
            reallyHasErrors = false;
            powerMode = Asic.PowerMode.SLEEPING;
        } else if (isVnish(values) && values.containsKey("state")) {
            powerMode =
                    "stopped".equals(values.getOrDefault("state", ""))
                            ? Asic.PowerMode.SLEEPING
                            : Asic.PowerMode.NORMAL;
            if (powerMode == Asic.PowerMode.SLEEPING) {
                reallyHasErrors = false;
            }
        } else {
            // Could be an older model and actually sleeping
            final String mode = values.getOrDefault("Mode", "");
            if ("254".equals(mode) || mode.contains("slowly")) {
                powerMode = Asic.PowerMode.SLEEPING;
                reallyHasErrors = false;
            } else {
                if (hashRate == 0) {
                    try {
                        final AtomicBoolean sleeping = new AtomicBoolean(false);
                        Query.digestGet(
                                this.ip,
                                this.port,
                                this.realm,
                                "/cgi-bin/get_miner_conf.cgi",
                                this.username,
                                this.password,
                                (code, s) ->
                                        toSleepingIdentifier(context, s)
                                                .ifPresent(ident -> sleeping.set(s.contains(ident))),
                                this.applicationConfiguration.getReadSocketTimeout());
                        if (sleeping.get()) {
                            powerMode = Asic.PowerMode.SLEEPING;
                            reallyHasErrors = false;
                        }
                    } catch (final Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // Might be stuck
        if (powerMode == Asic.PowerMode.NORMAL && hashRate == 0) {
            powerMode = Asic.PowerMode.IDLE;
        }

        builder
                .setPowerMode(powerMode)
                .hasErrors(reallyHasErrors);
    }

    /**
     * Checks to see if the stats indicate seer.
     *
     * @param stats The stats.
     *
     * @return Whether or not seer.
     */
    private static boolean isSeer(final Map<String, String> stats) {
        return stats.containsKey("asicid") &&
                stats.containsKey("hostname") &&
                stats.containsKey("ip") &&
                stats.containsKey("location");
    }

    /**
     * Checks to see if the stats indicate vnish.
     *
     * @param stats The stats.
     *
     * @return Whether vnish.
     */
    private static boolean isVnish(final Map<String, String> stats) {
        return stats.containsKey("state") &&
                stats.containsKey("chain_consumption1");
    }

    /**
     * Obtains the sleeping identifier from the provided context.
     *
     * @param context The context.
     * @param rawConf The response parameters.
     *
     * @return The sleeping identifier.
     */
    private static Optional<String> toSleepingIdentifier(
            final Context context,
            final String rawConf) {
        final Map<String, Object> conf = new HashMap<>();
        try {
            conf.putAll(
                    OBJECT_MAPPER.readValue(
                            rawConf,
                            new TypeReference<Map<String, Object>>() {
                            }));
        } catch (final JsonProcessingException e) {
            // Ignore
        }
        return context.getSimple(ContextKey.MINER_TYPE)
                .map(s -> {
                    if (AntminerUtils.isNewGen(
                            conf,
                            ImmutableMap.of(
                                    "slug",
                                    s))) {
                        return "\"bitmain-work-mode\" : \"1\"";
                    } else {
                        return "\"bitmain-work-mode\" : \"254\"";
                    }
                });
    }
}