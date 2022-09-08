package mn.foreman.model;

import mn.foreman.http.HttpRequestBuilder;
import mn.foreman.model.error.MinerException;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** A {@link AsicAction.CompletableAction} implementation that processes logs. */
public abstract class AbstractLogsAction
        implements AsicAction.CompletableAction {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractLogsAction.class);

    /** The configuration. */
    protected final ApplicationConfiguration configuration;

    /**
     * Constructor.
     *
     * @param configuration The configurations.
     */
    public AbstractLogsAction(final ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean run(
            final String ip,
            final int port,
            final Map<String, Object> parameters)
            throws MinerException {
        boolean anyFound = false;
        final Map<String, String> destinations =
                (Map<String, String>) parameters.get("destinations");
        for (final Map.Entry<String, String> entry : destinations.entrySet()) {
            final Optional<LogType> logTypeOptional =
                    LogType.forType(entry.getKey());
            if (logTypeOptional.isPresent()) {
                final LogType logType = logTypeOptional.get();
                final String url = entry.getValue();
                final Optional<String> logBody =
                        getLog(
                                ip,
                                port,
                                parameters,
                                logType);
                if (logBody.isPresent()) {
                    anyFound = true;
                    publishLog(
                            logType,
                            url,
                            logBody.get());
                }
            }
        }
        return anyFound;
    }

    /**
     * Runs a pool change.
     *
     * @param ip         The ip.
     * @param port       The port.
     * @param parameters The parameters.
     * @param logType    The log type.
     *
     * @return The result.
     */
    protected abstract Optional<String> getLog(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final LogType logType);

    /**
     * Publishes the content to the destination URL.
     *
     * @param logType The log type.
     * @param url     The destination.
     * @param body    The content.
     */
    private void publishLog(
            final LogType logType,
            final String url,
            final String body) {
        final ApplicationConfiguration.TimeConfig timeConfig =
                this.configuration.getWriteSocketTimeout();
        final boolean result =
                new HttpRequestBuilder<>()
                        .url(url)
                        .socketTimeout(
                                timeConfig.getTimeout(),
                                timeConfig.getTimeoutUnits())
                        .validator((code, s) -> code == HttpStatus.SC_OK)
                        .rawCallback((code, s) -> {
                        })
                        .putNoResponse(body);
        if (result) {
            LOG.info("{} file uploaded to {}", logType, url);
        } else {
            LOG.warn("Failed to upload {} log to {}", logType, url);
        }
    }

    /** The known log types. */
    public enum LogType {

        /** Miner logs. */
        MINER("miner"),

        /** Autotuner logs. */
        AUTOTUNE("autotune"),

        /** Kernel logs. */
        KERNEL("kernel");

        /** The known types. */
        private static final Map<String, LogType> TYPES =
                new ConcurrentHashMap<>();

        static {
            for (final LogType logType : LogType.values()) {
                TYPES.put(logType.type, logType);
            }
        }

        /** The type. */
        private final String type;

        /**
         * Constructor.
         *
         * @param type The type.
         */
        LogType(final String type) {
            this.type = type;
        }

        /**
         * Returns the type, if known.
         *
         * @param type The type.
         *
         * @return The known type.
         */
        public static Optional<LogType> forType(final String type) {
            return Optional.ofNullable(TYPES.get(type));
        }
    }
}
