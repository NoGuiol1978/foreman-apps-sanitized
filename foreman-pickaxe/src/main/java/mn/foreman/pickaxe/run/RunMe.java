package mn.foreman.pickaxe.run;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;
import mn.foreman.api.endpoints.pickaxe.Pickaxe;
import mn.foreman.api.model.CommandStart;
import mn.foreman.api.model.Commands;
import mn.foreman.io.Query;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.Miner;
import mn.foreman.model.MinerID;
import mn.foreman.model.cache.SelfExpiringStatsCache;
import mn.foreman.model.cache.StatsCache;
import mn.foreman.pickaxe.command.*;
import mn.foreman.pickaxe.command.asic.AsicStrategyFactory;
import mn.foreman.pickaxe.command.asic.ManufacturerContext;
import mn.foreman.pickaxe.command.asic.NullPostProcessor;
import mn.foreman.pickaxe.command.asic.scan.ScanStrategy;
import mn.foreman.pickaxe.contraints.*;
import mn.foreman.pickaxe.miners.MinerConfiguration;
import mn.foreman.pickaxe.miners.remote.RemoteConfiguration;
import mn.foreman.pickaxe.process.HttpPostMetricsProcessingStrategy;
import mn.foreman.pickaxe.process.MetricsProcessingStrategy;
import mn.foreman.pickaxe.run.thread.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** {@link RunMe} provides the application context for PICKAXE. */
@SuppressWarnings("UnstableApiUsage")
public class RunMe {

    /** All IPs. */
    private static final List<String> ALL_IPS =
            Collections.singletonList("0-255.0-255.0-255.0-255");

    /** All MACs. */
    private static final List<String> ALL_MACS =
            Collections.singletonList("*");

    /** The number of threads to use for running commands. */
    private static final int COMMAND_THREADS;

    /** The Foreman base URL. */
    private static final String FOREMAN_BASE_URL;

    /** The logger for this class. */
    private final static Logger LOG =
            LoggerFactory.getLogger(RunMe.class);

    /** The number of threads to use for metrics sending. */
    private static final int METRICS_THREADS;

    /** The mapper. */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    /** The path to the options file. */
    private static final String OPTIONS_PATH;

    /** The directory where Pickaxe is installed. */
    private static final String PICKAXE_HOME;

    /** The number of threads to use for scanning. */
    private static final int SCANNER_THREADS;

    /** The number of threads to use for pulling stats. */
    private static final int STATS_THREADS;

    static {
        PICKAXE_HOME = System.getenv("PICKAXE_HOME");
        OPTIONS_PATH = PICKAXE_HOME + File.separator + "conf" + File.separator + "jvm.options";
        FOREMAN_BASE_URL =
                System.getProperty(
                        "FOREMAN_BASE_URL",
                        "https://api.foreman.mn");
        COMMAND_THREADS = Runtime.getRuntime().availableProcessors() * 8;
        STATS_THREADS = Runtime.getRuntime().availableProcessors() * 8;
        METRICS_THREADS = Runtime.getRuntime().availableProcessors() * 4;
        SCANNER_THREADS =
                Math.max(
                        64,
                        Runtime.getRuntime().availableProcessors() * 8);
    }

    /** All of the allowed IP ranges. */
    private final AtomicReference<List<String>> allowedIpRanges =
            new AtomicReference<>(new LinkedList<>());

    /** All of the allowed MACs. */
    private final AtomicReference<List<String>> allowedMacs =
            new AtomicReference<>(new LinkedList<>());

    /** The API key. */
    private final AtomicReference<String> apiKey = new AtomicReference<>();

    /** The app configuration. */
    private final ApplicationConfiguration applicationConfiguration =
            new ApplicationConfiguration();

    /** The blacklisted miners. */
    private final Set<MinerID> blacklistedMiners =
            Sets.newConcurrentHashSet();

    /** Whether or not commands were successfully cancelled. */
    private final AtomicBoolean cancelledCommands = new AtomicBoolean(false);

    /** The client ID. */
    private final AtomicReference<String> clientId = new AtomicReference<>();

    /**
     * A cache for preventing commands from being allowed to run multiple
     * times.
     */
    private final Cache<String, Boolean> commandAckCache =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(
                            10,
                            TimeUnit.MINUTES)
                    .build();

    /** Whether or not Pickaxe is running with control permissions. */
    private final AtomicBoolean control = new AtomicBoolean(true);

    /** The {@link ForemanApi}. */
    private final AtomicReference<ForemanApi> foremanApi =
            new AtomicReference<>();

    /** The initial API key. */
    private final String initialApiKey;

    /** The initial client ID. */
    private final String initialClientId;

    /** The initial control value. */
    private final boolean initialControl;

    /** The IP validator. */
    private final IpValidator ipValidator;

    /** The total metrics iteration time. */
    private final AtomicLong iterationTime = new AtomicLong(0);

    /** The thread pool for querying MACs. */
    private final Executor macThreadPool =
            Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                            .setNameFormat("mac-thread-pool-%d")
                            .build());

    /** The rate limiter for metrics, if applicable. */
    private final com.google.common.util.concurrent.RateLimiter metricsLimiter =
            com.google.common.util.concurrent.RateLimiter.create(Integer.MAX_VALUE);

    /** The factory for creating all of the {@link Miner miners}. */
    private final MinerConfiguration minerConfiguration;

    /** A cache of all of the miners. */
    private final AtomicReference<List<Miner>> miners =
            new AtomicReference<>(new LinkedList<>());

    /** The pickaxe ID. */
    private final String pickaxeId;

    /** The rate limiter for ranges, if applicable. */
    private final com.google.common.util.concurrent.RateLimiter rangesScanLimiter =
            com.google.common.util.concurrent.RateLimiter.create(Integer.MAX_VALUE);

    /** The command rate limiters. */
    private final Map<String, RateLimiter> rateLimiters =
            new ConcurrentHashMap<>();

    /** The thread pool for scheduled services. */
    private final ScheduledExecutorService serviceThreadPool =
            Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors() * 4,
                    new ThreadFactoryBuilder()
                            .setNameFormat("service-thread-pool-%d")
                            .build());

    /** The rate limiter for start-stops, if applicable. */
    private final com.google.common.util.concurrent.RateLimiter startStopScanLimiter =
            com.google.common.util.concurrent.RateLimiter.create(Integer.MAX_VALUE);

    /** The command starts. */
    private final BlockingQueue<CommandStart> starts =
            new LinkedBlockingQueue<>();

    /** An in-memory cache for holding all of the active stats. */
    private final StatsCache statsCache =
            new SelfExpiringStatsCache(
                    120,
                    TimeUnit.SECONDS);

    /** The rate limiter for targeted ranges, if applicable. */
    private final com.google.common.util.concurrent.RateLimiter targetedRangesScanLimiter =
            com.google.common.util.concurrent.RateLimiter.create(Integer.MAX_VALUE);

    /** The rate limiter for targeted start-stops, if applicable. */
    private final com.google.common.util.concurrent.RateLimiter targetedScanLimiter =
            com.google.common.util.concurrent.RateLimiter.create(Integer.MAX_VALUE);

    /** The thread pool for running tasks. */
    private final ScheduledExecutorService threadPool =
            Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors() * 4,
                    new ThreadFactoryBuilder()
                            .setNameFormat("completion-thread-pool-%d")
                            .build());

    /** The callback. */
    private CommandCompletionCallback commandCompletionCallback;

    /** The command processor. */
    private CommandProcessor commandProcessor;

    /** The thread pool for running commands. */
    private WorkerPool commandThreadPool;

    /** The thread pool for metrics sending. */
    private WorkerPool metricsThreadPool;

    /** The thread pool for scanning (non-targeted, ranges). */
    private WorkerPool rangesThreadPool;

    /** The thread pool for scanning (non-targeted, stop/stop). */
    private WorkerPool startStopThreadPool;

    /** The thread pool for running tasks. */
    private WorkerPool statsThreadPool;

    /** The thread pool for scanning (targeted, ranges). */
    private WorkerPool targetedRangesThreadPool;

    /** The thread pool for scanning (targeted, stop/stop). */
    private WorkerPool targetedStartStopThreadPool;

    /**
     * Constructor.
     *
     * @param initialClientId The client ID.
     * @param initialApiKey   The API key.
     * @param pickaxeId       The pickaxe ID.
     * @param initialControl  The control.
     * @param firmwarePath    The firmware path.
     */
    public RunMe(
            final String initialClientId,
            final String initialApiKey,
            final String pickaxeId,
            final boolean initialControl) {
        LOG.debug("Base url: {}", FOREMAN_BASE_URL);
        this.initialClientId = initialClientId;
        this.initialApiKey = initialApiKey;
        this.pickaxeId = pickaxeId;
        this.initialControl = initialControl;

        resetConfiguration(
                initialClientId,
                initialApiKey,
                initialControl,
                ALL_IPS,
                ALL_MACS,
                new HashMap<>());

        this.ipValidator =
                new IpValidatorImpl(
                        this.allowedIpRanges,
                        this.allowedMacs);
        this.minerConfiguration =
                new RemoteConfiguration(
                        this.foremanApi,
                        this.applicationConfiguration,
                        this.ipValidator);
    }

    /**
     * Runs the application.
     *
     * <p>Note: this is the main processing function for PICKAXE.</p>
     */
    public void run() {
        // First, cancel any commands that were running
        final ForemanApi potentialApi = this.foremanApi.get();
        if (potentialApi != null) {
            cancelCommands();
        }

        final MetricsProcessingStrategy metricsProcessingStrategy =
                new HttpPostMetricsProcessingStrategy(
                        FOREMAN_BASE_URL,
                        this.clientId,
                        this.pickaxeId,
                        this.apiKey);

        final MetricsSender metricsSender =
                new MetricsSenderImpl(
                        metricsProcessingStrategy);

        final CommandFinalizer finalizer =
                new CommandFinalizerImpl(
                        this.foremanApi);
        final BlockingQueue<QueuedCommand> queuedCommands =
                new LinkedBlockingQueue<>();
        this.commandCompletionCallback =
                new QueuedCompletionCallback(
                        queuedCommands,
                        finalizer);

        startGuardrailQuerying();
        startConfigQuerying();
        startUpdateMiners();
        startBlacklistFlush();
        startMacQuerying();
        startCommandQuerying();
        startCommandFinishing(
                queuedCommands,
                finalizer);

        final BlockingQueue<MetricsWorker.SendJob> sendQueue =
                new LinkedBlockingQueue<>();

        this.metricsThreadPool =
                new WorkerPool(
                        "metrics",
                        "metrics-%d",
                        () -> new MetricsWorker(
                                sendQueue,
                                metricsSender));
        this.metricsThreadPool.scaleWorkers(METRICS_THREADS);

        //noinspection InfiniteLoopStatement
        while (true) {
            final ApplicationConfiguration.TimeConfig timeConfig =
                    this.applicationConfiguration.getMetricsPushConfig();

            final long deadline =
                    System.currentTimeMillis() +
                            timeConfig.getTimeoutUnits().toMillis(timeConfig.getTimeout());

            final List<StatsBatch> batches =
                    StatsBatch.toBatches(
                            this.statsCache.getMetrics(),
                            this.applicationConfiguration.getMetricsBatchSize());

            final CountDownLatch doneLatch = new CountDownLatch(batches.size());

            batches
                    .stream()
                    .map(statsBatch ->
                            MetricsWorker.SendJob
                                    .builder()
                                    .batch(statsBatch)
                                    .iterationTime(this.iterationTime)
                                    .completionCallback(doneLatch::countDown)
                                    .build())
                    .forEach(sendQueue::add);

            try {
                doneLatch.await();
            } catch (final InterruptedException e) {
                // Ignore
            }

            final long now = System.currentTimeMillis();
            if (now < deadline) {
                try {
                    TimeUnit.MILLISECONDS.sleep(deadline - now);
                } catch (final InterruptedException ie) {
                    // Ignore
                }
            } else {
                LOG.info("Took too long to send metrics - going again");
            }
        }
    }

    /**
     * Conditionally adjusts the rate limiter.
     *
     * @param name        The limiter name.
     * @param rateLimiter The target.
     * @param newLimit    The desired rate.
     */
    private static void adjustLimiter(
            final String name,
            final com.google.common.util.concurrent.RateLimiter rateLimiter,
            final Integer newLimit) {
        if (newLimit == null || newLimit == 0) {
            // No limit is set, so reset the rate, if appropriate
            if (rateLimiter.getRate() != Integer.MAX_VALUE) {
                LOG.debug("Adjusting {} to {}", name, Integer.MAX_VALUE);
                rateLimiter.setRate(Integer.MAX_VALUE);
            }
        } else if (rateLimiter.getRate() != newLimit) {
            LOG.debug("Adjusting {} to {}", name, newLimit);
            rateLimiter.setRate(newLimit);
        }
    }

    /**
     * Returns whether arguments have changed.
     *
     * @param arguments The arguments.
     *
     * @return Whether the arguments have changed.
     */
    private static boolean haveArgumentsChanged(final String arguments) {
        boolean hasChanged = false;
        try {
            boolean foundStart = false;
            final StringBuilder argumentBuilder = new StringBuilder();
            for (final String line : Files.readAllLines(new File(OPTIONS_PATH).toPath())) {
                if (line.contains("## Additional start")) {
                    foundStart = true;
                } else if (line.contains("## Additional end")) {
                    break;
                } else if (foundStart) {
                    argumentBuilder.append(line);
                }
            }
            hasChanged =
                    !argumentBuilder
                            .toString()
                            .trim()
                            .equals(arguments != null ? arguments.trim() : "");
        } catch (final IOException e) {
            // Ignore
        }
        return hasChanged;
    }

    /**
     * Returns the desired worker count safely.
     *
     * @param pickaxeConfiguration The configuration.
     * @param getter               The getter.
     * @param defaultSize          The default size if the API response
     *                             contained no configuration.
     *
     * @return The desired count.
     */
    private static int toDesired(
            final Pickaxe.PickaxeConfiguration pickaxeConfiguration,
            final Function<Pickaxe.PickaxeConfiguration, Integer> getter,
            final int defaultSize) {
        final Integer override = getter.apply(pickaxeConfiguration);
        if (override != null && override > 0) {
            return override;
        }
        return defaultSize;
    }

    /**
     * Writes the new options file.
     *
     * @param arguments The arguments.
     */
    private static void writeNewArguments(final String arguments) {
        try {
            final File optionsFile = new File(OPTIONS_PATH);

            final List<String> newLines = new LinkedList<>();
            boolean adding = true;
            for (final String line :
                    FileUtils.readLines(
                            optionsFile,
                            Charset.defaultCharset())) {
                if (adding) {
                    newLines.add(line);
                }
                if (line.contains("## Additional start")) {
                    adding = false;
                    newLines.add(arguments);
                }
                if (line.contains("## Additional end")) {
                    adding = true;
                    newLines.add(line);
                }
            }

            FileUtils.writeLines(optionsFile, newLines);
        } catch (final IOException e) {
            // Ignore
        }
    }

    /** Cancels any pending commands. */
    private void cancelCommands() {
        final List<Integer> cancelledCommands =
                this.foremanApi
                        .get()
                        .pickaxe()
                        .cancelCommands()
                        .orElse(Collections.emptyList());
        if (!cancelledCommands.isEmpty()) {
            LOG.info("Cancelled the following commands on startup: {}", cancelledCommands);
        }
        this.cancelledCommands.set(true);
    }

    /**
     * Checks to see if the new rate limit configuration has changed.
     *
     * @param original The new configuration.
     *
     * @return Whether the configuration has changed.
     */
    private boolean hasRateLimitingChanged(
            final Map<String, Integer> original) {
        final Map<String, Integer> newConfigs =
                original != null
                        ? original
                        : Collections.emptyMap();

        boolean hasChanged = false;
        if (this.rateLimiters.size() != newConfigs.size()) {
            hasChanged = true;
        } else {
            // Deep compare - the specs might have changed
            for (final Map.Entry<String, Integer> newConfig : newConfigs.entrySet()) {
                final String command = newConfig.getKey();
                if (this.rateLimiters.containsKey(command)) {
                    // Command was limited - see how it's constrained
                    final RateLimiter rateLimiter = this.rateLimiters.get(command);
                    if (rateLimiter.getLimit() != newConfig.getValue()) {
                        hasChanged = true;
                        break;
                    }
                } else {
                    // This command wasn't previously limited
                    hasChanged = true;
                    break;
                }
            }
        }
        return hasChanged;
    }

    /**
     * Resets the configuration back to the file-based config.
     *
     * @param clientId      The client ID.
     * @param apiKey        The API key.
     * @param control       If running with remote control.
     * @param allowedRanges The allowed IP ranges.
     * @param allowedMacs   The allowed MAC addresses.
     * @param rateLimits    The command rate limits.
     */
    private void resetConfiguration(
            final String clientId,
            final String apiKey,
            final boolean control,
            final List<String> allowedRanges,
            final List<String> allowedMacs,
            final Map<String, Integer> rateLimits) {
        boolean changed = false;

        if (!clientId.equals(this.clientId.get())) {
            this.clientId.set(clientId);
            changed = true;
        }

        if (!apiKey.equals(this.apiKey.get())) {
            this.apiKey.set(apiKey);
            changed = true;
        }

        if (control != this.control.get()) {
            this.control.set(control);
            changed = true;
        }

        if (!allowedRanges.equals(this.allowedIpRanges.get())) {
            this.allowedIpRanges.set(allowedRanges);
            changed = true;
        }

        if (!allowedMacs.equals(this.allowedMacs.get())) {
            this.allowedMacs.set(allowedMacs);
            changed = true;
        }

        if (changed || this.foremanApi.get() == null) {
            this.foremanApi.set(
                    new ForemanApiImpl(
                            clientId,
                            this.pickaxeId,
                            OBJECT_MAPPER,
                            new JdkWebUtil(
                                    FOREMAN_BASE_URL,
                                    apiKey,
                                    2,
                                    TimeUnit.MINUTES)));
            if (!this.cancelledCommands.get()) {
                cancelCommands();
            }
        }

        if (hasRateLimitingChanged(rateLimits)) {
            this.rateLimiters.clear();
            rateLimits
                    .entrySet()
                    .stream()
                    .map(entry ->
                            ImmutableMap.of(
                                    entry.getKey(),
                                    new TokenBucketRateLimiter(entry.getValue())))
                    .forEach(this.rateLimiters::putAll);
        }
    }

    /** Clears the {@link #blacklistedMiners} periodically. */
    private void startBlacklistFlush() {
        this.serviceThreadPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        LOG.info("Flushing blacklist");
                        this.blacklistedMiners.clear();
                    } catch (final Throwable t) {
                        LOG.warn("Exception during blacklist flush", t);
                    }
                },
                0,
                2,
                TimeUnit.MINUTES);
    }

    /**
     * Schedules command result processing.
     *
     * @param queuedCommands The commands.
     * @param finalizer      The finalizer.
     */
    @SuppressWarnings("UnstableApiUsage")
    private void startCommandFinishing(
            final BlockingQueue<QueuedCommand> queuedCommands,
            final CommandFinalizer finalizer) {
        this.serviceThreadPool.execute(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                LOG.info("Performing command finishing");
                try {
                    final List<QueuedCommand> reservedCommands =
                            new LinkedList<>();
                    Queues.drain(
                            queuedCommands,
                            reservedCommands,
                            this.applicationConfiguration.getCommandCompletionBatchSize(),
                            10,
                            TimeUnit.SECONDS);
                    if (!reservedCommands.isEmpty()) {
                        finalizer.finish(reservedCommands);
                    } else {
                        LOG.info("No commands to finish");
                    }
                } catch (final Throwable t) {
                    LOG.warn("Exception occurred while processing commands", t);
                }
            }
        });
    }

    /** Schedules command and control querying. */
    private void startCommandQuerying() {
        final BlockingQueue<ScanStrategy.Scanner.ScanJob> startStopJobs =
                new LinkedBlockingQueue<>();
        final BlockingQueue<ScanStrategy.Scanner.ScanJob> rangesJobs =
                new LinkedBlockingQueue<>();
        final BlockingQueue<ScanStrategy.Scanner.ScanJob> targetedStartStopJobs =
                new LinkedBlockingQueue<>();
        final BlockingQueue<ScanStrategy.Scanner.ScanJob> targetedRangesJobs =
                new LinkedBlockingQueue<>();

        this.startStopThreadPool =
                new WorkerPool(
                        "scan-thread-pool-startstop",
                        "scan-thread-pool-startstop-%d",
                        () -> new ScanStrategy.Scanner(
                                startStopJobs,
                                this.applicationConfiguration));
        this.startStopThreadPool.scaleWorkers(SCANNER_THREADS);

        this.rangesThreadPool =
                new WorkerPool(
                        "scan-thread-pool-ranges",
                        "scan-thread-pool-ranges-%d",
                        () -> new ScanStrategy.Scanner(
                                rangesJobs,
                                this.applicationConfiguration));
        this.rangesThreadPool.scaleWorkers(SCANNER_THREADS);

        this.targetedStartStopThreadPool =
                new WorkerPool(
                        "scan-thread-pool-targetedstartstop",
                        "scan-thread-pool-targetedstartstop-%d",
                        () -> new ScanStrategy.Scanner(
                                targetedStartStopJobs,
                                this.applicationConfiguration));
        this.targetedStartStopThreadPool.scaleWorkers(SCANNER_THREADS);

        this.targetedRangesThreadPool =
                new WorkerPool(
                        "scan-thread-pool-targetedranges",
                        "scan-thread-pool-targetedranges-%d",
                        () -> new ScanStrategy.Scanner(
                                targetedRangesJobs,
                                this.applicationConfiguration));
        this.targetedRangesThreadPool.scaleWorkers(SCANNER_THREADS);

        final ManufacturerContext manufacturerContext =
                ManufacturerContext
                        .builder()
                        .objectMapper(OBJECT_MAPPER)
                        .threadPool(this.threadPool)
                        .blacklist(this.blacklistedMiners)
                        .statsCache(this.statsCache)
                        .configuration(this.applicationConfiguration)
                        .build();

        this.commandProcessor =
                new CommandProcessorImpl(
                        this.commandCompletionCallback,
                        new AsicStrategyFactory(
                                new NullPostProcessor(),
                                manufacturerContext,
                                this.control,
                                this.ipValidator,
                                startStopJobs,
                                this.startStopScanLimiter,
                                rangesJobs,
                                this.rangesScanLimiter,
                                targetedStartStopJobs,
                                this.targetedScanLimiter,
                                targetedRangesJobs,
                                this.targetedRangesScanLimiter));

        this.commandThreadPool =
                new WorkerPool(
                        "command-thread-pool",
                        "command-thread-pool-%d",
                        () -> new CommandWorker(
                                this.commandProcessor,
                                this.starts,
                                this.rateLimiters,
                                this.commandCompletionCallback));
        this.commandThreadPool.scaleWorkers(COMMAND_THREADS);

        this.serviceThreadPool.execute(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                final ApplicationConfiguration.TimeConfig delayConfig =
                        this.applicationConfiguration.getCommandQueryConfig();
                int delaySeconds =
                        (int) delayConfig.getTimeoutUnits()
                                .toSeconds(delayConfig.getTimeout());
                try {
                    LOG.info("Querying Foreman API for commands");

                    final Optional<Commands> commandsOpt =
                            this.foremanApi
                                    .get()
                                    .pickaxe()
                                    .getCommands();
                    if (commandsOpt.isPresent()) {
                        final Commands commands = commandsOpt.get();

                        // Mark the commands as started immediately, even if
                        // they're queued internally. Picking them up counts.
                        commands
                                .commands
                                .stream()
                                .filter(commandStart -> this.commandAckCache.getIfPresent(commandStart.id) == null)
                                .forEach(commandStart -> {
                                    this.commandAckCache.put(commandStart.id, true);
                                    this.commandCompletionCallback.start(
                                            commandStart.id,
                                            commandStart);
                                });

                        this.starts.addAll(commands.commands);
                        delaySeconds = commands.delaySeconds;
                    } else {
                        LOG.warn("Failed to obtain commands");
                    }
                } catch (final Throwable t) {
                    LOG.warn("Exception occurred while command querying", t);
                }

                try {
                    TimeUnit.SECONDS.sleep(delaySeconds);
                } catch (final Throwable t) {
                    LOG.warn("Exception occurred while sleeping", t);
                }
            }
        });
    }

    /** Starts the thread that will continuously query for new configurations. */
    private void startConfigQuerying() {
        this.serviceThreadPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        LOG.info("Downloading config");

                        try {
                            final List<Miner> currentMiners =
                                    this.miners.get();
                            final List<Miner> newMiners =
                                    this.minerConfiguration.load();
                            if (!CollectionUtils.isEqualCollection(
                                    currentMiners,
                                    newMiners)) {
                                LOG.debug("A new configuration has been obtained");
                                this.miners.set(newMiners);
                                this.blacklistedMiners.clear();
                            } else {
                                LOG.debug("No configuration changes were observed");
                            }
                        } catch (final Exception e) {
                            LOG.warn("Exception occurred while querying", e);
                        }

                        LOG.info("Downloading appConfig");
                        try {
                            this.foremanApi
                                    .get()
                                    .pickaxe()
                                    .config()
                                    .ifPresent(pickaxeConfiguration -> {
                                        // Check to see if the JVM arguments
                                        // have changed
                                        if (haveArgumentsChanged(pickaxeConfiguration.jvmArguments)) {
                                            LOG.info("Writing new JVM arguments");
                                            writeNewArguments(pickaxeConfiguration.jvmArguments);
                                            System.exit(0);
                                        } else {
                                            LOG.debug("JVM arguments haven't changed");
                                        }

                                        // Modify the application's running
                                        // configuration based on the web config
                                        this.applicationConfiguration.setReadSocketTimeout(
                                                pickaxeConfiguration.readSocketTimeout,
                                                TimeUnit.valueOf(pickaxeConfiguration.readSocketTimeoutUnits));
                                        this.applicationConfiguration.setWriteSocketTimeout(
                                                pickaxeConfiguration.writeSocketTimeout,
                                                TimeUnit.valueOf(pickaxeConfiguration.writeSocketTimeoutUnits));
                                        this.applicationConfiguration.setCommandCompletionBatchSize(
                                                pickaxeConfiguration.commandCompletionBatchSize);
                                        this.applicationConfiguration.setCollectConfig(
                                                pickaxeConfiguration.collectDelay,
                                                TimeUnit.valueOf(pickaxeConfiguration.collectDelayUnits));
                                        this.applicationConfiguration.setMetricsPushConfig(
                                                pickaxeConfiguration.metricsPushDelay,
                                                TimeUnit.valueOf(pickaxeConfiguration.metricsPushDelayUnits));
                                        this.applicationConfiguration.setCommandQueryConfig(
                                                pickaxeConfiguration.commandQueryDelay,
                                                TimeUnit.valueOf(pickaxeConfiguration.commandQueryDelayUnits));
                                        this.applicationConfiguration.setMetricsBatchSize(
                                                pickaxeConfiguration.metricsBatchSize);

                                        // Throttling
                                        adjustLimiter(
                                                "metricsLimiter",
                                                this.metricsLimiter,
                                                pickaxeConfiguration.metricsRateLimit);
                                        adjustLimiter(
                                                "startStopScanLimiter",
                                                this.startStopScanLimiter,
                                                pickaxeConfiguration.startStopRateLimit);
                                        adjustLimiter(
                                                "rangesScanLimiter",
                                                this.rangesScanLimiter,
                                                pickaxeConfiguration.rangesRateLimit);
                                        adjustLimiter(
                                                "targetedScanLimiter",
                                                this.targetedScanLimiter,
                                                pickaxeConfiguration.startStopTargetedRateLimit);
                                        adjustLimiter(
                                                "targetedRangesScanLimiter",
                                                this.targetedRangesScanLimiter,
                                                pickaxeConfiguration.rangesTargetedRateLimit);

                                        this.commandThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.commandThreadsOverride,
                                                        COMMAND_THREADS));
                                        this.statsThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.statsThreadsOverride,
                                                        STATS_THREADS));
                                        this.metricsThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.metricsThreadsOverride,
                                                        METRICS_THREADS));
                                        this.startStopThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.startStopScannerThreadsOverride,
                                                        SCANNER_THREADS));
                                        this.rangesThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.rangesScannerThreadsOverride,
                                                        SCANNER_THREADS));
                                        this.targetedStartStopThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.targetedStartStopScannerThreadsOverride,
                                                        SCANNER_THREADS));
                                        this.targetedRangesThreadPool.scaleWorkers(
                                                toDesired(
                                                        pickaxeConfiguration,
                                                        pickaxeConfiguration1 -> pickaxeConfiguration1.targetedRangesScannerThreadsOverride,
                                                        SCANNER_THREADS));
                                        LOG.debug("New config: {}", this.applicationConfiguration);
                                    });
                        } catch (final Exception e) {
                            LOG.warn("Failed to obtain app config", e);
                        }
                    } catch (final Throwable t) {
                        LOG.warn("Exception during config download", t);
                    }
                },
                0,
                2,
                TimeUnit.MINUTES);
    }

    /**
     * Schedules the periodic querying of guardrail for configuration and
     * constraints.
     */
    private void startGuardrailQuerying() {
        this.serviceThreadPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        final GuardrailConfiguration guardrail =
                                Query.restQuery(
                                        "127.0.0.1",
                                        25452,
                                        "/api/config",
                                        new TypeReference<GuardrailConfiguration>() {
                                        });
                        if (guardrail.clientId != null) {
                            resetConfiguration(
                                    guardrail.clientId.toString(),
                                    guardrail.apiKey,
                                    guardrail.control,
                                    guardrail.ranges,
                                    guardrail.macs,
                                    guardrail.rateLimits);
                        } else {
                            resetConfiguration(
                                    this.initialClientId,
                                    this.initialApiKey,
                                    this.initialControl,
                                    ALL_IPS,
                                    ALL_MACS,
                                    new HashMap<>());
                        }
                    } catch (final Exception e) {
                        LOG.info("Failed to query guardrail", e);
                        resetConfiguration(
                                this.initialClientId,
                                this.initialApiKey,
                                this.initialControl,
                                ALL_IPS,
                                ALL_MACS,
                                new HashMap<>());
                    }
                },
                0,
                30,
                TimeUnit.SECONDS);
    }

    /** Starts the thread that will periodically query for MAC addresses. */
    private void startMacQuerying() {
        this.macThreadPool.execute(
                new MacWorker(
                        this.miners,
                        this.blacklistedMiners,
                        this.foremanApi));
    }

    /** Schedules the job to begin automatically updating miner stats. */
    private void startUpdateMiners() {
        final BlockingQueue<StatsWorker.StatJob> jobQueue =
                new LinkedBlockingQueue<>();
        this.statsThreadPool =
                new WorkerPool(
                        "stats-thread-pool",
                        "stats-thread-pool-%d",
                        () -> new StatsWorker(
                                this.blacklistedMiners,
                                this.statsCache,
                                jobQueue));
        this.statsThreadPool.scaleWorkers(STATS_THREADS);

        this.serviceThreadPool.execute(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                final long startTime = System.currentTimeMillis();

                try {
                    LOG.info("Creating stat jobs");

                    final List<Miner> miners = this.miners.get();
                    final CountDownLatch latch =
                            new CountDownLatch(miners.size());
                    for (final Miner miner : miners) {
                        final StatsWorker.StatJob statJob =
                                StatsWorker.StatJob
                                        .builder()
                                        .miner(miner)
                                        .completionCallback(latch::countDown)
                                        .build();

                        // Rate limit, if applicable
                        try {
                            this.metricsLimiter.acquire();
                        } catch (final Exception e) {
                            // Ignore
                        }
                        jobQueue.add(statJob);
                    }

                    latch.await();
                } catch (final Throwable t) {
                    LOG.warn("Exception occurred while updating", t);
                }

                final long endTime = System.currentTimeMillis();
                final long runTime = endTime - startTime;
                this.iterationTime.set(runTime);

                // Delay
                final ApplicationConfiguration.TimeConfig config =
                        this.applicationConfiguration.getCollectConfig();
                final long delayMillis =
                        config.getTimeoutUnits().toMillis(config.getTimeout());

                if (runTime < delayMillis) {
                    final long sleepTime = delayMillis - runTime;
                    LOG.debug("Waiting {}ms before querying miners again", sleepTime);
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                    } catch (final InterruptedException e) {
                        // Ignore - try again
                    }
                }
            }
        });
    }
}