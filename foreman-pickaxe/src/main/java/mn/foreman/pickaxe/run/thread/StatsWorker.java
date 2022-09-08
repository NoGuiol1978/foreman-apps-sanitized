package mn.foreman.pickaxe.run.thread;

import mn.foreman.model.Miner;
import mn.foreman.model.MinerID;
import mn.foreman.model.cache.StatsCache;

import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** A worker for obtaining metrics. */
public class StatsWorker
        implements WorkerPool.Worker {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(StatsWorker.class);

    /** All of the blacklisted miners. */
    private final Set<MinerID> blacklistMiners;

    /** The work queue. */
    private final BlockingQueue<StatJob> miners;

    /** Whether running. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** The metrics cache. */
    private final StatsCache statsCache;

    /**
     * Constructor.
     *
     * @param blacklistMiners The blacklisted miners.
     * @param statsCache      The metrics cache.
     * @param miners          The work queue.
     */
    public StatsWorker(
            final Set<MinerID> blacklistMiners,
            final StatsCache statsCache,
            final BlockingQueue<StatJob> miners) {
        this.blacklistMiners = blacklistMiners;
        this.statsCache = statsCache;
        this.miners = miners;
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
                    final StatJob job = this.miners.take();

                    final Miner miner = job.miner;
                    final Runnable callback = job.completionCallback;

                    try {
                        final MinerID minerID = miner.getMinerID();
                        if (!this.blacklistMiners.contains(minerID)) {
                            try {
                                this.statsCache.add(
                                        minerID,
                                        miner.getStats());
                                LOG.debug("Cached metrics for {}", miner);
                            } catch (final Exception e) {
                                LOG.info("Failed to obtain metrics for {}",
                                        miner,
                                        e);
                                this.blacklistMiners.add(minerID);
                                this.statsCache.invalidate(minerID);
                            }
                        }
                    } finally {
                        callback.run();
                    }
                } catch (final Throwable t) {
                    LOG.debug("Exception occurred in stats worker", t);
                }
            }
        } catch (final Throwable t) {
            LOG.warn("Stopping stats worker", t);
        }
    }

    /** A task to query one miner's metrics. */
    @Data
    @Builder
    public static class StatJob {

        /** The callback when done. */
        private final Runnable completionCallback;

        /** The miner to query. */
        private final Miner miner;
    }
}