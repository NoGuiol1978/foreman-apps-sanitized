package mn.foreman.pickaxe.run.thread;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * A {@link WorkerPool} is very similar to an {@link Executor}, with the
 * differing factory being that it can be dynamically scaled up or down
 * internally.
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This class is only thread-safe if {@link #scaleWorkers(int)} is called
 * from the same thread. It wasn't intended to be manipulated by multiple
 * threads at the same time.</p>
 */
public class WorkerPool {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(WorkerPool.class);

    /** The backing thread pool. */
    private final Executor executor;

    /** The thread pool name. */
    private final String threadPoolName;

    /** The factory for creating new workers. */
    private final Supplier<Worker> workerFactory;

    /** The currently running workers. */
    private List<Worker> workers = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param threadPoolName       The thread pool name.
     * @param threadPoolNameFormat The thread pool name format.
     * @param workerFactory        The factory for creating new workers.
     */
    public WorkerPool(
            final String threadPoolName,
            final String threadPoolNameFormat,
            final Supplier<Worker> workerFactory) {
        this.threadPoolName = threadPoolName;
        this.workerFactory = workerFactory;
        this.executor =
                Executors.newCachedThreadPool(
                        new ThreadFactoryBuilder()
                                .setNameFormat(threadPoolNameFormat)
                                .build());
    }

    /**
     * Scales the workers up to the desired count.
     *
     * @param desired The desired number of workers.
     */
    public void scaleWorkers(final int desired) {
        LOG.info("Adjusting {} threads from {} to {}",
                this.threadPoolName,
                this.workers.size(),
                desired);
        if (this.workers.size() > desired) {
            // Trim some down
            final List<List<Worker>> split =
                    ListUtils.partition(
                            this.workers,
                            desired);
            if (split.size() > 0) {
                this.workers = Iterables.getFirst(split, new LinkedList<>());
                if (split.size() > 1) {
                    split
                            .subList(1, split.size())
                            .stream()
                            .flatMap(List::stream)
                            .forEach(worker -> {
                                LOG.info("Stopping {} worker {}",
                                        this.threadPoolName,
                                        worker);
                                try {
                                    worker.close();
                                } catch (final IOException e) {
                                    // Ignore
                                }
                            });
                }
            }
        } else if (desired > this.workers.size()) {
            // Run more
            final int missing = desired - this.workers.size();
            LOG.info("Running {} more {} workers",
                    this.threadPoolName,
                    missing);
            for (int i = 0; i < missing; i++) {
                LOG.info("Starting new {} worker", this.threadPoolName);
                final Worker worker =
                        this.workerFactory.get();
                this.workers.add(worker);
                this.executor.execute(worker);
            }
        }
    }

    /** A worker is a task that be both started and stopped. */
    public interface Worker
            extends Runnable, Closeable {

    }
}
