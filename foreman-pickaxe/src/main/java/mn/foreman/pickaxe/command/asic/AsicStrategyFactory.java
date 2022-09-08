package mn.foreman.pickaxe.command.asic;

import mn.foreman.pickaxe.command.CommandStrategy;
import mn.foreman.pickaxe.command.PostCommandProcessor;
import mn.foreman.pickaxe.command.StrategyFactory;
import mn.foreman.pickaxe.command.asic.discover.DiscoverStrategy;
import mn.foreman.pickaxe.command.asic.rawstats.RawStatsStrategy;
import mn.foreman.pickaxe.command.asic.scan.MacFilteringStrategy;
import mn.foreman.pickaxe.command.asic.scan.RangesSourceStrategy;
import mn.foreman.pickaxe.command.asic.scan.ScanStrategy;
import mn.foreman.pickaxe.command.asic.scan.StartStopSourceStrategy;
import mn.foreman.pickaxe.command.asic.terminate.TerminateStrategy;
import mn.foreman.pickaxe.contraints.IpValidatingCommandStrategyDecorator;
import mn.foreman.pickaxe.contraints.IpValidator;

import com.google.common.util.concurrent.RateLimiter;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link AsicStrategyFactory} provides a {@link StrategyFactory}
 * implementation that's capable of producing {@link CommandStrategy strategies}
 * that works with ASICs.
 */
public class AsicStrategyFactory
        implements StrategyFactory {

    /** The context. */
    private final ManufacturerContext context;

    /** The validator. */
    private final IpValidator ipValidator;

    /** Whether or not command and control is enabled. */
    private final AtomicBoolean isControl;

    /** The post processor for rebooting. */
    private final PostCommandProcessor postRebootProcessor;

    /** The range-based scan strategy. */
    private final ScanStrategy rangesScanStrategy;

    /** The start/stop-based scan strategy. */
    private final ScanStrategy startStopScanStrategy;

    /** The strategy to use for range-based targeted scans. */
    private final ScanStrategy targetedScanRangesStrategy;

    /** The targeted MAC scan strategy. */
    private final ScanStrategy targetedScanStrategy;

    /**
     * Constructor.
     *
     * @param postRebootProcessor      The post processor for rebooting.
     * @param context                  The context.
     * @param isControl                Whether command and control is enabled.
     * @param startStopJobs            The scanner job queue for start/stop
     *                                 jobs.
     * @param startStopLimiter         The limiter for start-stop scans.
     * @param rangesJobs               The scanner job queue for range-based
     *                                 jobs.
     * @param rangesLimiter            The limiter for range scans.
     * @param targetedStartStopJobs    The scanner job queue for targeted,
     *                                 start/stop jobs.
     * @param targetedStartStopLimiter The limiter for start-stop targeted
     *                                 scans.
     * @param targetedRangesJobs       The scanner job queue for targeted,
     *                                 ranges jobs.
     * @param targetedRangesLimiter    The limiter for range targeted scans.
     */
    @SuppressWarnings("UnstableApiUsage")
    public AsicStrategyFactory(
            final PostCommandProcessor postRebootProcessor,
            final ManufacturerContext context,
            final AtomicBoolean isControl,
            final IpValidator ipValidator,
            final BlockingQueue<ScanStrategy.Scanner.ScanJob> startStopJobs,
            final RateLimiter startStopLimiter,
            final BlockingQueue<ScanStrategy.Scanner.ScanJob> rangesJobs,
            final RateLimiter rangesLimiter,
            final BlockingQueue<ScanStrategy.Scanner.ScanJob> targetedStartStopJobs,
            final RateLimiter targetedStartStopLimiter,
            final BlockingQueue<ScanStrategy.Scanner.ScanJob> targetedRangesJobs,
            final RateLimiter targetedRangesLimiter) {
        this.postRebootProcessor = postRebootProcessor;
        this.context = context;
        this.isControl = isControl;
        this.ipValidator = ipValidator;

        this.startStopScanStrategy =
                new ScanStrategy(
                        startStopLimiter,
                        new StartStopSourceStrategy(),
                        this.ipValidator,
                        startStopJobs);
        this.rangesScanStrategy =
                new ScanStrategy(
                        rangesLimiter,
                        new RangesSourceStrategy(),
                        this.ipValidator,
                        rangesJobs);
        this.targetedScanStrategy =
                new ScanStrategy(
                        targetedStartStopLimiter,
                        new StartStopSourceStrategy(),
                        new MacFilteringStrategy(),
                        this.ipValidator,
                        targetedStartStopJobs);
        this.targetedScanRangesStrategy =
                new ScanStrategy(
                        targetedRangesLimiter,
                        new RangesSourceStrategy(),
                        new MacFilteringStrategy(),
                        this.ipValidator,
                        targetedRangesJobs);
    }

    @Override
    public Optional<CommandStrategy> forType(final String type) {
        return RemoteCommand
                .forType(type)
                .flatMap(command -> toStrategy(command)
                        .map(command1 ->
                                new IpValidatingCommandStrategyDecorator(
                                        command1,
                                        this.ipValidator)));
    }

    /**
     * Converts the provided command to a {@link CommandStrategy} that can be
     * invoked.
     *
     * @param remoteCommand The command.
     *
     * @return The strategy to invoke.
     */
    private Optional<CommandStrategy> toStrategy(final RemoteCommand remoteCommand) {
        CommandStrategy commandStrategy = null;
        switch (remoteCommand) {
            case DISCOVER:
                commandStrategy = new DiscoverStrategy();
                break;
            case SCAN_RANGES:
                commandStrategy = this.rangesScanStrategy;
                break;
            case SCAN:
                commandStrategy = this.startStopScanStrategy;
                break;
            case TARGETED_SCAN:
                commandStrategy = this.targetedScanStrategy;
                break;
            case TARGETED_SCAN_RANGES:
                commandStrategy = this.targetedScanRangesStrategy;
                break;
            case CHANGE_POOLS:
                commandStrategy =
                        new RebootingCommandStrategy(
                                this.postRebootProcessor,
                                manufacturer ->
                                        manufacturer.getChangePoolsAction(
                                                this.context));
                break;
            case REBOOT:
                commandStrategy =
                        new RebootingCommandStrategy(
                                this.postRebootProcessor,
                                manufacturer ->
                                        manufacturer.getRebootAction(
                                                this.context));
                break;
            case RAW_STATS:
                commandStrategy =
                        new RawStatsStrategy(
                                this.context.getConfiguration(),
                                this.ipValidator);
                break;
            case FACTORY_RESET:
                commandStrategy =
                        new RebootingCommandStrategy(
                                this.postRebootProcessor,
                                manufacturer ->
                                        manufacturer.getFactoryResetStrategy(
                                                this.context));
                break;
            case TERMINATE:
                commandStrategy = new TerminateStrategy();
                break;
            case FETCH_LOGS:
                commandStrategy =
                        new RebootingCommandStrategy(
                                this.postRebootProcessor,
                                manufacturer ->
                                        manufacturer.getLogStrategy(
                                                this.context));
                break;
            default:
                break;
        }

        return this.isControl.get() || !remoteCommand.isControl()
                ? Optional.ofNullable(commandStrategy)
                : Optional.empty();
    }
}
