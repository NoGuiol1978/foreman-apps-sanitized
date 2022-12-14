package mn.foreman.antminer;

import mn.foreman.model.*;
import mn.foreman.util.ArgUtils;

import java.util.*;

/** A strategy for detecting Antminers. */
public class AntminerDetectionStrategy
        implements DetectionStrategy {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The hostname strategies. */
    private final List<HostnameStrategy> hostnameStrategies;

    /** The strategies for detecting MACs. */
    private final List<MacStrategy> macStrategies;

    /** The miner for obtaining stats. */
    private final Miner miner;

    /** The realm. */
    private final String realm;

    /**
     * Constructor.
     *
     * @param realm                    The realm.
     * @param macStrategies            The mac strategies.
     * @param hostnameStrategies       The hostname strategies.
     * @param miner                    The miner for obtaining stats.
     * @param applicationConfiguration The configuration.
     */
    public AntminerDetectionStrategy(
            final String realm,
            final List<MacStrategy> macStrategies,
            final List<HostnameStrategy> hostnameStrategies,
            final Miner miner,
            final ApplicationConfiguration applicationConfiguration) {
        this.realm = realm;
        this.macStrategies = new ArrayList<>(macStrategies);
        this.hostnameStrategies = new ArrayList<>(hostnameStrategies);
        this.miner = miner;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public Optional<Detection> detect(
            final String ip,
            final int port,
            final Map<String, Object> args) {
        Detection detection = null;
        try {
            int webPort =
                    Integer.parseInt(args.getOrDefault("webPort", "80").toString());
            if (port == 4029) {
                // Test hook
                webPort = 8081;
            }
            final String username =
                    args.getOrDefault("username", "root").toString();
            final String password =
                    args.getOrDefault("password", "root").toString();

            final Optional<AntminerType> type =
                    AntminerUtils.getType(
                            ip,
                            port,
                            webPort,
                            this.realm,
                            username,
                            password,
                            (s1, s2, s3) -> {
                            },
                            this.applicationConfiguration);
            if (type.isPresent()) {
                final AntminerType realType = type.get();

                final Map<String, Object> newArgs = new HashMap<>(args);
                if (ArgUtils.isHostnamePreferred(args)) {
                    this.hostnameStrategies
                            .stream()
                            .map(hostnameStrategy -> {
                                try {
                                    return hostnameStrategy.getHostname(
                                            ip,
                                            port,
                                            args);
                                } catch (final Exception e) {
                                    return Optional.empty();
                                }
                            })
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .findFirst()
                            .ifPresent(
                                    hostname ->
                                            newArgs.put(
                                                    "hostname",
                                                    hostname));
                } else if (ArgUtils.isWorkerPreferred(args)) {
                    DetectionUtils.addWorkerFromStats(
                            this.miner,
                            newArgs);
                }

                // MAC detection is a must
                final String mac =
                        this.macStrategies
                                .stream()
                                .map(MacStrategy::getMacAddress)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .findFirst()
                                .orElse(null);
                if (mac != null) {
                    newArgs.put("mac", mac);
                    detection =
                            Detection
                                    .builder()
                                    .ipAddress(ip)
                                    .port(port)
                                    .minerType(realType)
                                    .parameters(newArgs)
                                    .build();
                }
            }
        } catch (final Exception e) {
            // Failed to find
        }
        return Optional.ofNullable(detection);
    }
}