package mn.foreman.pickaxe.command.asic.scan;

import mn.foreman.api.model.CommandDone;
import mn.foreman.api.model.CommandStart;
import mn.foreman.api.model.CommandUpdate;
import mn.foreman.api.model.DoneStatus;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.Detection;
import mn.foreman.model.DetectionStrategy;
import mn.foreman.model.MinerType;
import mn.foreman.pickaxe.command.CommandCompletionCallback;
import mn.foreman.pickaxe.command.CommandStrategy;
import mn.foreman.pickaxe.command.asic.Manufacturer;
import mn.foreman.pickaxe.contraints.IpValidator;
import mn.foreman.pickaxe.run.thread.WorkerPool;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static mn.foreman.pickaxe.command.util.CommandUtils.safeGet;

/** Scans the provided subnet and start/stop for miners. */
@SuppressWarnings("UnstableApiUsage")
public class ScanStrategy
        implements CommandStrategy {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(ScanStrategy.class);

    /** The number of scan waiting rounds before the scan is considered done. */
    private static final int SCAN_SIMILAR_ROUNDS_CUTOFF = 6;

    /** The filtering strategy. */
    private final FilteringStrategy filteringStrategy;

    /** The source strategy. */
    private final IpSourceStrategy ipSourceStrategy;

    /** The IP validator. */
    private final IpValidator ipValidator;

    /** A lock to keep counters protected. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** The rate limiter for throttling scans. */
    private final RateLimiter rateLimiter;

    /** The job queue. */
    private final BlockingQueue<Scanner.ScanJob> scanJobs;

    /**
     * Constructor.
     *
     * @param rateLimiter      The rate limiter.
     * @param ipSourceStrategy The source strategy.
     * @param ipValidator      The IP validator.
     * @param scanJobs         The scanner jobs.
     */
    public ScanStrategy(
            final RateLimiter rateLimiter,
            final IpSourceStrategy ipSourceStrategy,
            final IpValidator ipValidator,
            final BlockingQueue<Scanner.ScanJob> scanJobs) {
        this(
                rateLimiter,
                ipSourceStrategy,
                new NullFilteringStrategy(),
                ipValidator,
                scanJobs);
    }

    /**
     * Constructor.
     *
     * @param rateLimiter       The rate limiter.
     * @param ipSourceStrategy  The strategy for generating the IPs to scan.
     * @param filteringStrategy The strategy for filtering.
     * @param ipValidator       The IP validator.
     * @param scanJobs          The scan jobs.
     */
    public ScanStrategy(
            final RateLimiter rateLimiter,
            final IpSourceStrategy ipSourceStrategy,
            final FilteringStrategy filteringStrategy,
            final IpValidator ipValidator,
            final BlockingQueue<Scanner.ScanJob> scanJobs) {
        this.rateLimiter = rateLimiter;
        this.filteringStrategy = filteringStrategy;
        this.ipSourceStrategy = ipSourceStrategy;
        this.ipValidator = ipValidator;
        this.scanJobs = scanJobs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void runCommand(
            final CommandStart command,
            final CommandCompletionCallback commandCompletionCallback,
            final CommandDone.CommandDoneBuilder builder) {
        final Map<String, Object> args = command.args;

        final String type = safeGet(args, "type");
        final int port = Integer.parseInt(safeGet(args, "port"));
        final List<String> targetMacs =
                ((List<String>) args.getOrDefault(
                        "targetMacs",
                        new LinkedList<String>()))
                        .stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
        final List<String> targetWorkers =
                (List<String>) args.getOrDefault(
                        "targetWorkers",
                        new LinkedList<String>());
        final List<String> existingMacs =
                ((List<String>) args.getOrDefault(
                        "existingMacs",
                        new LinkedList<String>()))
                        .stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
        final boolean autoAdd =
                (Boolean) args.getOrDefault(
                        "autoAdd",
                        false);

        if ("asic".equals(type)) {
            runAsicScan(
                    command,
                    commandCompletionCallback,
                    safeGet(args, "manufacturer"),
                    port,
                    targetMacs,
                    targetWorkers,
                    autoAdd,
                    existingMacs,
                    args,
                    builder);
        }
    }

    /**
     * Converts the provided {@link Detection} to a miner.
     *
     * @param detection The {@link Detection}.
     *
     * @return The miner.
     */
    private static Object toMiner(final Detection detection) {
        final MinerType minerType = detection.getMinerType();
        return ImmutableMap.of(
                "apiPort",
                detection.getPort(),
                "parameters",
                detection.getParameters(),
                "key",
                minerType.getSlug(),
                "category",
                minerType.getCategory().getName(),
                "ipAddress",
                detection.getIpAddress());
    }

    /**
     * Creates a {@link CommandUpdate} from the provided parameters.
     *
     * @param found     The number of found miners.
     * @param scanned   The number of IPs scanned.
     * @param remaining The remaining count.
     *
     * @return The update.
     */
    private static CommandUpdate toUpdate(
            final int found,
            final int scanned,
            final int remaining) {
        return CommandUpdate
                .builder()
                .command("scan")
                .update(
                        ImmutableMap.of(
                                "found",
                                found,
                                "scanned",
                                scanned,
                                "remaining",
                                remaining))
                .build();
    }

    /**
     * Callback for the completion of a scan attempt.
     *
     * @param id                        The command ID.
     * @param detection                 The result.
     * @param targetMacs                The MACs being searched.
     * @param targetWorkers             The workers being searched.
     * @param autoAdd                   Whether miners are automatically being
     *                                  added.
     * @param existingMacs              The existing MACs.
     * @param miners                    The resulting miners.
     * @param others                    The unexpected, found miners.
     * @param commandCompletionCallback The callback for command updates.
     * @param scanned                   The scanned running count.
     * @param remaining                 The remaining running count.
     * @param ended                     Whether the scanning was gracefully
     *                                  terminated.
     */
    private void onScanCompletion(
            final String id,
            final Detection detection,
            final List<String> targetMacs,
            final List<String> targetWorkers,
            final boolean autoAdd,
            final List<String> existingMacs,
            final BlockingQueue<Object> miners,
            final BlockingQueue<Object> others,
            final CommandCompletionCallback commandCompletionCallback,
            final AtomicInteger scanned,
            final AtomicInteger remaining,
            final AtomicBoolean ended) {
        this.lock.writeLock().lock();
        try {
            processDetection(
                    detection,
                    targetMacs,
                    targetWorkers,
                    autoAdd,
                    existingMacs,
                    miners,
                    others);
            commandCompletionCallback.update(
                    id,
                    toUpdate(
                            miners.size(),
                            scanned.incrementAndGet(),
                            remaining.decrementAndGet()));
            ended.set(remaining.get() <= 0);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Conditionally adds the {@link Detection} result to either the miners
     * results or the others.
     *
     * @param detection     The result.
     * @param targetMacs    The macs being scanned.
     * @param targetWorkers The workers being scanned.
     * @param autoAdd       Whether the result should get added if it wasn't a
     *                      target.
     * @param existingMacs  The existing macs.
     * @param miners        The resulting miners.
     * @param others        The others.
     */
    private void processDetection(
            final Detection detection,
            final List<String> targetMacs,
            final List<String> targetWorkers,
            final boolean autoAdd,
            final List<String> existingMacs,
            final BlockingQueue<Object> miners,
            final BlockingQueue<Object> others) {
        try {
            if (detection != null) {
                if (this.filteringStrategy.matches(
                        detection,
                        targetMacs,
                        targetWorkers)) {
                    miners.add(
                            toMiner(
                                    detection));
                } else if (autoAdd) {
                    final String mac =
                            (String) detection.getParameters().get("mac");
                    if (mac != null && !existingMacs.contains(mac.toLowerCase())) {
                        others.add(
                                toMiner(
                                        detection));
                    }
                }
            }
        } catch (final Exception e) {
            LOG.warn("Exception occurred while scanning", e);
        }
    }

    /**
     * Scans for an ASIC.
     *
     * @param command                   The command.
     * @param commandCompletionCallback The Foreman API handle.
     * @param manufacturer              The manufacturer.
     * @param port                      The port to inspect.
     * @param targetMacs                The target MACs.
     * @param targetWorkers             The target workers.
     * @param autoAdd                   Whether miners should be auto-added.
     * @param existingMacs              All of the existing MAC addresses.
     * @param args                      The arguments.
     * @param builder                   The builder to use for creating the
     *                                  final result.
     */
    private void runAsicScan(
            final CommandStart command,
            final CommandCompletionCallback commandCompletionCallback,
            final String manufacturer,
            final int port,
            final List<String> targetMacs,
            final List<String> targetWorkers,
            final boolean autoAdd,
            final List<String> existingMacs,
            final Map<String, Object> args,
            final CommandDone.CommandDoneBuilder builder) {
        Manufacturer
                .fromName(manufacturer)
                .ifPresent(value -> scanInRange(
                        commandCompletionCallback,
                        command.id,
                        port,
                        targetMacs,
                        targetWorkers,
                        autoAdd,
                        existingMacs,
                        args,
                        value,
                        builder));
    }

    /**
     * Scans a valid range.
     *
     * @param commandCompletionCallback The Foreman API handle.
     * @param id                        The command ID.
     * @param port                      The port to inspect.
     * @param targetMacs                The target macs.
     * @param targetWorkers             The target workers.
     * @param autoAdd                   Whether miners should be auto-added.
     * @param existingMacs              All of the existing MAC addresses.
     * @param args                      The arguments.
     * @param builder                   The builder to use for creating the
     *                                  final result.
     * @param manufacturer              The manufacturer.
     */
    private void scanInRange(
            final CommandCompletionCallback commandCompletionCallback,
            final String id,
            final int port,
            final List<String> targetMacs,
            final List<String> targetWorkers,
            final boolean autoAdd,
            final List<String> existingMacs,
            final Map<String, Object> args,
            final Manufacturer manufacturer,
            final CommandDone.CommandDoneBuilder builder) {
        final List<String> ipsToScan =
                this.ipSourceStrategy.toIps(
                        args);
        if (ipsToScan.size() < 65_536) {
            final AtomicInteger scanned = new AtomicInteger(0);
            final AtomicInteger remaining = new AtomicInteger(ipsToScan.size());

            final BlockingQueue<Object> miners = new LinkedBlockingQueue<>();
            final BlockingQueue<Object> others = new LinkedBlockingQueue<>();
            final AtomicBoolean ended = new AtomicBoolean(false);

            // Send an initial update to let the dashboard know how much work
            // needs to be done
            commandCompletionCallback.update(
                    id,
                    toUpdate(
                            0,
                            0,
                            remaining.get()));

            ipsToScan
                    .stream()
                    .map(ip ->
                            Scanner.ScanJob
                                    .builder()
                                    .ip(ip)
                                    .port(port)
                                    .args(args)
                                    .rateLimiter(this.rateLimiter)
                                    .manufacturer(manufacturer)
                                    .ipValidator(this.ipValidator)
                                    .queryWorkers(!targetWorkers.isEmpty())
                                    .detectionConsumer(
                                            detection ->
                                                    onScanCompletion(
                                                            id,
                                                            detection,
                                                            targetMacs,
                                                            targetWorkers,
                                                            autoAdd,
                                                            existingMacs,
                                                            miners,
                                                            others,
                                                            commandCompletionCallback,
                                                            scanned,
                                                            remaining,
                                                            ended))
                                    .build())
                    .forEach(this.scanJobs::add);

            // Wait for every IP to be examined
            if (waitForCompletion(scanned, ipsToScan.size(), ended)) {
                LOG.debug("Scan gracefully stopped");
            } else {
                LOG.warn("Scan stopped before all of the IPs were found");
            }

            commandCompletionCallback.done(
                    id,
                    builder
                            .result(
                                    ImmutableMap.of(
                                            "miners",
                                            miners,
                                            "others",
                                            others))
                            .status(
                                    CommandDone.Status
                                            .builder()
                                            .type(DoneStatus.SUCCESS)
                                            .build())
                            .build());
        } else {
            commandCompletionCallback.done(
                    id,
                    builder.status(
                            CommandDone.Status
                                    .builder()
                                    .type(DoneStatus.FAILED)
                                    .message("Subnet range is too large")
                                    .build())
                            .build());
        }
    }

    /**
     * Continuously check to see if the scans completed.
     *
     * @param scanned   The number of IPs scanned.
     * @param ipsToScan The desired number of IPs to scan.
     * @param ended     Whether the scan gracefully completed.
     *
     * @return Whether every IP was checked.
     */
    private boolean waitForCompletion(
            final AtomicInteger scanned,
            final int ipsToScan,
            final AtomicBoolean ended) {
        int lastScanned = -1;
        int scanSimilarRounds = 0;
        do {
            // Go until the scans have stopped
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (final InterruptedException e) {
                // Ignore
            }

            if (lastScanned == scanned.get()) {
                scanSimilarRounds++;
            } else {
                scanSimilarRounds = 0;
            }
            lastScanned = scanned.get();
        } while (!ended.get() && scanSimilarRounds < SCAN_SIMILAR_ROUNDS_CUTOFF);
        return (ended.get() || (scanned.get() >= ipsToScan));
    }

    /**
     * A {@link Scanner} provides a {@link Runnable} implementation that will
     * continuously scan miners until there are no more IPs that need to be
     * evaluated.
     */
    public static class Scanner
            implements WorkerPool.Worker {

        /** The configuration. */
        private final ApplicationConfiguration configuration;

        /** Whether running. */
        private final AtomicBoolean running = new AtomicBoolean(false);

        /** The scan jobs. */
        private final BlockingQueue<ScanJob> scanJobs;

        /**
         * Constructor.
         *
         * @param scanJobs      The job queue.
         * @param configuration The configuration.
         */
        public Scanner(
                final BlockingQueue<ScanJob> scanJobs,
                final ApplicationConfiguration configuration) {
            this.scanJobs = scanJobs;
            this.configuration = configuration;
        }

        @Override
        public void close() throws IOException {
            this.running.set(false);
        }

        @Override
        public void run() {
            this.running.set(true);

            while (this.running.get()) {
                try {
                    final ScanJob scanJob = this.scanJobs.take();

                    final RateLimiter rateLimiter =
                            scanJob.rateLimiter;
                    final Consumer<Detection> detectionConsumer =
                            scanJob.detectionConsumer;
                    final Manufacturer manufacturer =
                            scanJob.manufacturer;
                    final String ip =
                            scanJob.ip;
                    final int port =
                            scanJob.port;
                    final IpValidator ipValidator =
                            scanJob.ipValidator;
                    final boolean queryWorkers =
                            scanJob.queryWorkers;
                    final Map<String, Object> args =
                            new HashMap<>(scanJob.args);
                    if (queryWorkers) {
                        args.put(
                                "workerPreferred",
                                "true");
                    }

                    final DetectionStrategy detectionStrategy =
                            manufacturer.getDetectionStrategy(
                                    args,
                                    ip,
                                    this.configuration);

                    // Throttle the scan rates down, if applicable
                    rateLimiter.acquire();

                    LOG.debug("Scanning {}:{}", ip, port);

                    Optional<Detection> detectionOpt = Optional.empty();
                    try {
                        detectionOpt =
                                detectionStrategy.detect(
                                        ip,
                                        port,
                                        args)
                                        .filter(detection -> {
                                            final String mac =
                                                    (String) detection.getParameters()
                                                            .getOrDefault(
                                                                    "mac",
                                                                    "");
                                            return ipValidator.isAllowed(ip, mac);
                                        })
                                        .map(this::clean);
                    } catch (final Exception e) {
                        LOG.warn("Exception occurred while querying", e);
                    }

                    detectionConsumer.accept(detectionOpt.orElse(null));
                } catch (final Throwable e) {
                    LOG.warn("Scanner was interrupted", e);
                }
            }
        }

        /**
         * Cleans the {@link Detection} to prevent too many args from being
         * published.
         *
         * @param detection The detection.
         *
         * @return The cleaned {@link Detection}.
         */
        private Detection clean(final Detection detection) {
            final Map<String, Object> parameters = detection.getParameters();
            parameters.remove("targetMacs");
            parameters.remove("targetWorkers");
            parameters.remove("existingMacs");
            parameters.remove("ranges");
            parameters.remove("range");
            return Detection
                    .builder()
                    .ipAddress(detection.getIpAddress())
                    .port(detection.getPort())
                    .minerType(detection.getMinerType())
                    .parameters(parameters)
                    .build();
        }

        /** A scan job. */
        @Data
        @Builder
        public static class ScanJob {

            /** The arguments. */
            private final Map<String, Object> args;

            /** The result consumer. */
            private final Consumer<Detection> detectionConsumer;

            /** The ip. */
            private final String ip;

            /** The IP validator. */
            private final IpValidator ipValidator;

            /** The manufacturer. */
            private final Manufacturer manufacturer;

            /** The port. */
            private final int port;

            /** Whether workers should be queried. */
            private final boolean queryWorkers;

            /** The limiter for controlling scan speeds. */
            private final RateLimiter rateLimiter;
        }
    }
}