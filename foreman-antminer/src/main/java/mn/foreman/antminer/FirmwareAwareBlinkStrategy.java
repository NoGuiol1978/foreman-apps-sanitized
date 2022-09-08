package mn.foreman.antminer;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.BlinkStrategy;
import mn.foreman.model.error.MinerException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Runs the appropriate blink strategy. */
public class FirmwareAwareBlinkStrategy
        implements BlinkStrategy {

    /** Braiins. */
    private final BlinkStrategy braiins;

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /** Seer. */
    private final BlinkStrategy seer;

    /** Stock. */
    private final BlinkStrategy stock;

    /** Vnish. */
    private final BlinkStrategy vnish;

    /**
     * Constructor.
     *
     * @param stock         Stock.
     * @param braiins       Braiins.
     * @param vnish         Vnish.
     * @param seer          Seer.
     * @param configuration The configuration.
     */
    public FirmwareAwareBlinkStrategy(
            final BlinkStrategy stock,
            final BlinkStrategy braiins,
            final BlinkStrategy vnish,
            final BlinkStrategy seer,
            final ApplicationConfiguration configuration) {
        this.stock = stock;
        this.braiins = braiins;
        this.vnish = vnish;
        this.seer = seer;
        this.configuration = configuration;
    }

    @Override
    public boolean startBlink(
            final String ip,
            final int port,
            final Map<String, Object> parameters)
            throws MinerException {
        return runForType(
                ip,
                port,
                parameters,
                BlinkStrategy::startBlink);
    }

    @Override
    public boolean stopBlink(
            final String ip,
            final int port,
            final Map<String, Object> parameters)
            throws MinerException {
        return runForType(
                ip,
                port,
                parameters,
                BlinkStrategy::stopBlink);
    }

    private boolean runForType(
            final String ip,
            final int port,
            final Map<String, Object> args,
            final BlinkLedAction blinkLedAction) throws MinerException {
        boolean success;

        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicReference<AntminerType> typeReference =
                new AtomicReference<>();

        AntminerUtils.getType(
                ip,
                Integer.parseInt(args.getOrDefault("apiPort", "4028").toString()),
                port,
                "antMiner Configuration",
                args.getOrDefault("username", "root").toString(),
                args.getOrDefault("password", "root").toString(),
                (s1, s2, s3) -> completed.set(true),
                this.configuration)
                .ifPresent(typeReference::set);

        if (completed.get()) {
            final AntminerType type = typeReference.get();
            if (type != null) {
                try {
                    if (type.isBraiins()) {
                        success =
                                blinkLedAction.run(
                                        this.braiins,
                                        ip,
                                        port,
                                        args);
                    } else if (type.isVnish()) {
                        success =
                                blinkLedAction.run(
                                        this.vnish,
                                        ip,
                                        port,
                                        args);
                    } else {
                        success =
                                blinkLedAction.run(
                                        this.stock,
                                        ip,
                                        port,
                                        args);
                    }
                } catch (final Exception e) {
                    throw new MinerException(e);
                }
            } else {
                throw new MinerException("Unexpected Antminer type");
            }
        } else {
            throw new MinerException("Miner is unreachable");
        }

        return success;
    }

    /**
     * Provides a functional interface for abstracting away the version
     * checking.
     */
    @FunctionalInterface
    private interface BlinkLedAction {

        /**
         * Runs the action.
         *
         * @param ip   The ip.
         * @param port The port.
         * @param args The args.
         *
         * @return The action result.
         *
         * @throws Exception on failure.
         */
        boolean run(
                BlinkStrategy blinkStrategy,
                String ip,
                int port,
                Map<String, Object> args)
                throws Exception;
    }
}
