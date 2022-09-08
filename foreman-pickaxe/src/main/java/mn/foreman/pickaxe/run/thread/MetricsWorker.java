package mn.foreman.pickaxe.run.thread;

import mn.foreman.pickaxe.run.MetricsSender;

import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** A worker for sending a batch of metrics. */
public class MetricsWorker
        implements WorkerPool.Worker {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(StatsWorker.class);

    /** The metrics sender. */
    private final MetricsSender metricsSender;

    /** Whether running. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** The work queue. */
    private final BlockingQueue<MetricsWorker.SendJob> sendQueue;

    /**
     * Constructor.
     *
     * @param sendQueue     The queue to send.
     * @param metricsSender The sender.
     */
    public MetricsWorker(
            final BlockingQueue<MetricsWorker.SendJob> sendQueue,
            final MetricsSender metricsSender) {
        this.sendQueue = sendQueue;
        this.metricsSender = metricsSender;
    }

    @Override
    public void close() throws IOException {
        this.running.set(false);
    }

    @Override
    public void run() {
        this.running.set(true);
        try {
            while (this.running.get()) {
                try {
                    final SendJob job = this.sendQueue.take();
                    try {
                        this.metricsSender.sendMetrics(
                                job.iterationTime,
                                job.batch.getBatchTime(),
                                job.batch.getBatch());
                    } finally {
                        job.completionCallback.run();
                    }
                } catch (final Throwable t) {
                    LOG.debug("Exception occurred in metrics worker", t);
                }
            }
        } catch (final Throwable t) {
            LOG.warn("Stopping metrics worker", t);
        }
    }

    /** A task to send a batch of metrics. */
    @Data
    @Builder
    public static class SendJob {

        /** The batch. */
        private final StatsBatch batch;

        /** The callback when done. */
        private final Runnable completionCallback;

        /** The iteration time. */
        private final AtomicLong iterationTime;
    }
}
