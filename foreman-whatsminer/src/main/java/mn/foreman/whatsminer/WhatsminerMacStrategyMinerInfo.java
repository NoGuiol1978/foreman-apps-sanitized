package mn.foreman.whatsminer;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.MacStrategy;
import mn.foreman.whatsminer.latest.Command;
import mn.foreman.whatsminer.latest.WhatsminerApi;
import mn.foreman.whatsminer.latest.error.ApiException;
import mn.foreman.whatsminer.latest.error.PermissionDeniedException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** A strategy for obtaining MAC addresses from the new firmware. */
public class WhatsminerMacStrategyMinerInfo
        implements MacStrategy {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The IP. */
    private final String ip;

    /** The params. */
    private final Map<String, Object> parameters;

    /** The port. */
    private final int port;

    /**
     * Constructor.
     *
     * @param ip                       The IP.
     * @param port                     The port.
     * @param parameters               The parameters.
     * @param applicationConfiguration The configuration.
     */
    public WhatsminerMacStrategyMinerInfo(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final ApplicationConfiguration applicationConfiguration) {
        this.ip = ip;
        this.port = port;
        this.parameters = parameters;
        this.applicationConfiguration = applicationConfiguration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<String> getMacAddress() {
        final AtomicReference<String> mac = new AtomicReference<>();
        try {
            WhatsminerApi.runCommand(
                    this.ip,
                    this.port,
                    this.parameters.getOrDefault("password", "admin").toString(),
                    Command.GET_MINER_INFO,
                    Collections.emptyMap(),
                    this.applicationConfiguration,
                    response -> {
                        final Map<String, String> msg =
                                (Map<String, String>) response;
                        mac.set(msg.getOrDefault("mac", ""));
                    });
        } catch (final ApiException | PermissionDeniedException e) {
            // Ignore
        }
        return Optional.ofNullable(mac.get());
    }
}
