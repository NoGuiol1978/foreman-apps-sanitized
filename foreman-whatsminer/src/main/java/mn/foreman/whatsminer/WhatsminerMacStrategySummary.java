package mn.foreman.whatsminer;

import mn.foreman.cgminer.CgMiner;
import mn.foreman.cgminer.Context;
import mn.foreman.cgminer.ContextKey;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.cgminer.request.CgMinerRequest;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.MacStrategy;
import mn.foreman.model.miners.asic.Asic;

import java.util.Collections;
import java.util.Optional;

/** A strategy for obtaining MAC addresses from the new firmware. */
public class WhatsminerMacStrategySummary
        implements MacStrategy {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The IP. */
    private final String ip;

    /** The password. */
    private final String password;

    /** The port. */
    private final int port;

    /**
     * Constructor.
     *
     * @param ip                       The IP.
     * @param port                     The port.
     * @param password                 The password.
     * @param applicationConfiguration The configuration.
     */
    public WhatsminerMacStrategySummary(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration applicationConfiguration) {
        this.ip = ip;
        this.port = port;
        this.password = password;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public Optional<String> getMacAddress() {
        final Context context = new Context();
        final CgMiner miner =
                new CgMiner.Builder(context, Collections.emptyList())
                        .setApiIp(this.ip)
                        .setApiPort(this.port)
                        .setConnectTimeout(this.applicationConfiguration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.SUMMARY)
                                        .build(),
                                (builder, response) ->
                                        WhatsminerUtils.updateSummary(
                                                this.ip,
                                                this.port,
                                                this.password,
                                                this.applicationConfiguration,
                                                response.getValues(),
                                                new Asic.Builder(),
                                                context))
                        .build();

        try {
            miner.getStats();
        } catch (final Exception e) {
            // Ignore
        }

        return context.getSimple(ContextKey.MAC);
    }
}
