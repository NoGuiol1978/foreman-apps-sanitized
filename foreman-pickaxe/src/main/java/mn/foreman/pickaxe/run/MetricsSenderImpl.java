package mn.foreman.pickaxe.run;

import mn.foreman.model.MetricsReport;
import mn.foreman.model.metadata.ApiVersion;
import mn.foreman.model.metadata.Metadata;
import mn.foreman.model.miners.MinerStats;
import mn.foreman.pickaxe.process.MetricsProcessingStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link MetricsSenderImpl} provides a sender that will push metrics to the
 * Foreman dashboard.
 */
public class MetricsSenderImpl
        implements MetricsSender {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(MetricsSenderImpl.class);

    /** The strategy to use for post-processing metrics. */
    private final MetricsProcessingStrategy strategy;

    /**
     * Constructor.
     *
     * @param strategy The strategy to use for post-processing metrics.
     */
    MetricsSenderImpl(final MetricsProcessingStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void sendMetrics(
            final AtomicLong queryTimeMs,
            final ZonedDateTime publishTime,
            final List<MinerStats> stats) {
        final MetricsReport.Builder builder =
                new MetricsReport.Builder()
                        .setMetadata(
                                new Metadata.Builder()
                                        .setApiVersion(ApiVersion.V1_0_0)
                                        .setTimestamp(publishTime)
                                        .build())
                        .setLoopTimeMs(queryTimeMs.get());
        if (!stats.isEmpty()) {
            stats.forEach(builder::addMinerStats);
            try {
                // Metrics could be empty if everything was down
                final MetricsReport metricsReport =
                        builder.build();

                LOG.debug("Generated report: {}", metricsReport);

                this.strategy.process(metricsReport);
            } catch (final Exception e) {
                LOG.warn("Exception occurred while generating report", e);
            }
        } else {
            LOG.info("No miner stats to report");
        }
    }
}
