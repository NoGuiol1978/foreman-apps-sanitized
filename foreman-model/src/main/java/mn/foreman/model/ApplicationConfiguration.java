package mn.foreman.model;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** The current Pickaxe configuration. */
public class ApplicationConfiguration {

    /** How often to collect stats. */
    private final AtomicReference<TimeConfig> collectConfig =
            new AtomicReference<>(
                    new TimeConfig(
                            30,
                            TimeUnit.SECONDS));

    /** How often to query for commands. */
    private final AtomicReference<TimeConfig> commandQueryConfig =
            new AtomicReference<>(
                    new TimeConfig(
                            30,
                            TimeUnit.SECONDS));

    /** How often to push metrics. */
    private final AtomicReference<TimeConfig> metricsPushConfig =
            new AtomicReference<>(
                    new TimeConfig(
                            1,
                            TimeUnit.MINUTES));

    /** The read socket configuration. */
    private final AtomicReference<TimeConfig> readSocketConfig =
            new AtomicReference<>(
                    new TimeConfig(
                            1,
                            TimeUnit.SECONDS));

    /** The write socket configuration. */
    private final AtomicReference<TimeConfig> writeSocketConfig =
            new AtomicReference<>(
                    new TimeConfig(
                            1,
                            TimeUnit.SECONDS));

    /** The maximum number of commands to POST at a time. */
    private int commandCompletionBatchSize = 500;

    /** How many metrics to send in a single batch. */
    private int metricsBatchSize = 200;

    /**
     * Returns the collect config.
     *
     * @return The collect config.
     */
    public TimeConfig getCollectConfig() {
        return this.collectConfig.get();
    }

    /**
     * Returns the completion batch size.
     *
     * @return The completion batch size.
     */
    public int getCommandCompletionBatchSize() {
        return this.commandCompletionBatchSize;
    }

    /**
     * Sets the command completion batch size.
     *
     * @param commandCompletionBatchSize The command completion batch size.
     */
    public void setCommandCompletionBatchSize(
            final int commandCompletionBatchSize) {
        this.commandCompletionBatchSize = commandCompletionBatchSize;
    }

    /**
     * Returns the command query config.
     *
     * @return The command query config.
     */
    public TimeConfig getCommandQueryConfig() {
        return this.commandQueryConfig.get();
    }

    /**
     * Returns the metrics batch size.
     *
     * @return The metrics batch size.
     */
    public int getMetricsBatchSize() {
        return this.metricsBatchSize;
    }

    /**
     * Sets the metrics batch size.
     *
     * @param metricsBatchSize The metrics batch size.
     */
    public void setMetricsBatchSize(final int metricsBatchSize) {
        this.metricsBatchSize = metricsBatchSize;
    }

    /**
     * Returns the push config.
     *
     * @return The push config.
     */
    public TimeConfig getMetricsPushConfig() {
        return this.metricsPushConfig.get();
    }

    /**
     * Returns the socket configuration.
     *
     * @return The socket configuration.
     */
    public TimeConfig getReadSocketTimeout() {
        return this.readSocketConfig.get();
    }

    /**
     * Returns the write socket configuration.
     *
     * @return The write socket configuration.
     */
    public TimeConfig getWriteSocketTimeout() {
        return this.writeSocketConfig.get();
    }

    /**
     * Sets the collect config.
     *
     * @param timeout The timeout.
     * @param units   The units.
     */
    public void setCollectConfig(
            final int timeout,
            final TimeUnit units) {
        this.collectConfig.set(
                new TimeConfig(
                        timeout,
                        units));
    }

    /**
     * Sets the command query config.
     *
     * @param timeout The timeout.
     * @param units   The units.
     */
    public void setCommandQueryConfig(
            final int timeout,
            final TimeUnit units) {
        this.commandQueryConfig.set(
                new TimeConfig(
                        timeout,
                        units));
    }

    /**
     * Sets the push config.
     *
     * @param timeout The timeout.
     * @param units   The units.
     */
    public void setMetricsPushConfig(
            final int timeout,
            final TimeUnit units) {
        this.metricsPushConfig.set(
                new TimeConfig(
                        timeout,
                        units));
    }

    /**
     * Sets the read socket timeout.
     *
     * @param socketTimeout      The socket timeout.
     * @param socketTimeoutUnits The socket timeout (units).
     */
    public void setReadSocketTimeout(
            final int socketTimeout,
            final TimeUnit socketTimeoutUnits) {
        this.readSocketConfig.set(
                new TimeConfig(
                        socketTimeout,
                        socketTimeoutUnits));
    }

    /**
     * Sets the write socket timeout.
     *
     * @param socketTimeout      The socket timeout.
     * @param socketTimeoutUnits The socket timeout (units).
     */
    public void setWriteSocketTimeout(
            final int socketTimeout,
            final TimeUnit socketTimeoutUnits) {
        this.writeSocketConfig.set(
                new TimeConfig(
                        socketTimeout,
                        socketTimeoutUnits));
    }

    @Override
    public String toString() {
        return String.format(
                "%s [ " +
                        "commandCompletionBatchSize=%d, " +
                        "readConfig=%s, " +
                        "writeConfig=%s" +
                        " ]",
                getClass().getSimpleName(),
                this.commandCompletionBatchSize,
                this.readSocketConfig,
                this.writeSocketConfig);
    }

    /** A time configuration. */
    public static class TimeConfig {

        /** The timeout. */
        private final int timeout;

        /** The timeout (units). */
        private final TimeUnit timeoutUnits;

        /**
         * Constructor.
         *
         * @param timeout      The socket timeout.
         * @param timeoutUnits The socket timeout (units).
         */
        public TimeConfig(
                final int timeout,
                final TimeUnit timeoutUnits) {
            this.timeout = timeout;
            this.timeoutUnits = timeoutUnits;
        }

        /**
         * Returns the timeout.
         *
         * @return The timeout.
         */
        public int getTimeout() {
            return this.timeout;
        }

        /**
         * Returns the timeout (units).
         *
         * @return The timeout (units).
         */
        public TimeUnit getTimeoutUnits() {
            return this.timeoutUnits;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s [ " +
                            "timeout=%s, " +
                            "timeoutUnits=%s" +
                            " ]",
                    getClass().getSimpleName(),
                    this.timeout,
                    this.timeoutUnits);
        }
    }
}
