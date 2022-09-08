package mn.foreman.pickaxe.run.thread;

import mn.foreman.api.model.CommandDone;
import mn.foreman.api.model.CommandStart;
import mn.foreman.api.model.DoneStatus;
import mn.foreman.model.error.MinerException;
import mn.foreman.pickaxe.command.CommandCompletionCallback;
import mn.foreman.pickaxe.command.CommandProcessor;
import mn.foreman.pickaxe.contraints.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** A worker that will run commands. */
public class CommandWorker
        implements WorkerPool.Worker {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(CommandWorker.class);

    /** The completion callback. */
    private final CommandCompletionCallback commandCompletionCallback;

    /** The processor for commands. */
    private final CommandProcessor processor;

    /** The rate limiters for each command. */
    private final Map<String, RateLimiter> rateLimiters;

    /** Whether the worker is running. */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /** The work queue. */
    private final BlockingQueue<CommandStart> starts;

    /**
     * Constructor.
     *
     * @param processor                 The processor.
     * @param starts                    The work queue.
     * @param rateLimiters              The rate limiters.
     * @param commandCompletionCallback The completion callback.
     */
    public CommandWorker(
            final CommandProcessor processor,
            final BlockingQueue<CommandStart> starts,
            final Map<String, RateLimiter> rateLimiters,
            final CommandCompletionCallback commandCompletionCallback) {
        this.processor = processor;
        this.starts = starts;
        this.rateLimiters = rateLimiters;
        this.commandCompletionCallback = commandCompletionCallback;
    }

    @Override
    public void close() throws IOException {
        this.running.set(false);
    }

    @Override
    public void run() {
        try {
            while (this.running.get()) {
                try {
                    final CommandStart start = this.starts.take();
                    if (this.rateLimiters.containsKey(start.command)) {
                        runRateLimited(start);
                    } else {
                        runOpen(start);
                    }
                } catch (final Throwable t) {
                    LOG.warn("Exception occurred while processing command", t);
                }
            }
        } catch (final Exception e) {
            LOG.warn("Stopping command worker", e);
        }
    }

    /**
     * Runs a command immediately (not rate-limited).
     *
     * @param start The command to run.
     *
     * @throws MinerException on failure.
     */
    private void runOpen(final CommandStart start) throws MinerException {
        this.processor.runCommand(start);
    }

    /**
     * Runs a command if it falls under the rate limiting threshold.
     *
     * @param start The command to run.
     *
     * @throws MinerException on failure.
     */
    private void runRateLimited(final CommandStart start) throws MinerException {
        final RateLimiter rateLimiter = this.rateLimiters.get(start.command);
        if (rateLimiter.getLimit() > 0) {
            runRateLimitedAndAllowed(
                    start,
                    rateLimiter);
        } else {
            // This command isn't allowed at all
            this.commandCompletionCallback.done(
                    start.id,
                    CommandDone
                            .builder()
                            .command(start.command)
                            .status(
                                    CommandDone.Status
                                            .builder()
                                            .type(DoneStatus.FAILED)
                                            .message("Command denied by GUARDrail")
                                            .details("Command denied by GUARDrail")
                                            .build())
                            .build());
        }
    }

    /**
     * Runs a command against a limiter that is allowed (the limiter's limit is
     * non-0).
     *
     * @param start       The command to run.
     * @param rateLimiter The limiter.
     *
     * @throws MinerException on failure.
     */
    private void runRateLimitedAndAllowed(
            final CommandStart start,
            final RateLimiter rateLimiter) throws MinerException {
        if (rateLimiter.consume(1)) {
            // This command fell under the threshold, so it
            // can run immediately
            this.processor.runCommand(start);
        } else {
            // Re-queue the command - we're over the threshold
            this.starts.add(start);
        }
    }
}
