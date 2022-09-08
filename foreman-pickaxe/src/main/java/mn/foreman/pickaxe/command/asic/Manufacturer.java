package mn.foreman.pickaxe.command.asic;

import mn.foreman.antminer.*;
import mn.foreman.antminer.braiins.*;
import mn.foreman.antminer.stock.StockLogsAction;
import mn.foreman.antminer.vnish.VnishAwareAction;
import mn.foreman.antminer.vnish.v2.VnishChangePoolsAction;
import mn.foreman.antminer.vnish.v3.VnishLogsAction;
import mn.foreman.antminer.vnish.v3.VnishMacStrategy;
import mn.foreman.antminer.vnish.v3.VnishRebootAction;
import mn.foreman.cgminer.CgMinerDetectionStrategy;
import mn.foreman.cgminer.NullPatchingStrategy;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.model.*;
import mn.foreman.whatsminer.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.EntryStream;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/** An enumeration containing all of the known manufacturers. */
public enum Manufacturer {

    /** Antminer. */
    ANTMINER(
            "antminer",
            (args, ip, configuration, objectMapper) ->
                    new AntminerDetectionStrategy(
                            "antMiner Configuration",
                            Arrays.asList(
                                    new StockMacStrategy(
                                            ip,
                                            80,
                                            "antMiner Configuration",
                                            args.getOrDefault("username", "").toString(),
                                            args.getOrDefault("password", "").toString(),
                                            configuration),
                                    new VnishMacStrategy(
                                            ip,
                                            80,
                                            args.getOrDefault("password", "").toString(),
                                            configuration),
                                    new BraiinsMacStrategy(
                                            ip,
                                            args.getOrDefault("username", "").toString(),
                                            args.getOrDefault("password", "").toString())),
                            Arrays.asList(
                                    new StockHostnameStrategy(
                                            "antMiner Configuration",
                                            configuration),
                                    new BraiinsHostnameStrategy()),
                            new AntminerFactory(
                                    1,
                                    configuration).create(
                                    EntryStream
                                            .of(args)
                                            .append(
                                                    "apiIp",
                                                    ip)
                                            .append(
                                                    "apiPort",
                                                    "4028")
                                            .toMap()),
                            configuration),
            context ->
                    new ChainedAsicAction(
                            AsicActionFactory.toAsync(
                                    context.getThreadPool(),
                                    context.getBlacklist(),
                                    context.getStatsCache(),
                                    new AntminerFactory(
                                            1,
                                            context.getConfiguration()),
                                    new mn.foreman.antminer.FirmwareAwareAction(
                                            "antMiner Configuration",
                                            new StockChangePoolsAction(
                                                    "antMiner Configuration",
                                                    Arrays.asList(
                                                            AntminerConfValue.POOL_1_URL,
                                                            AntminerConfValue.POOL_1_USER,
                                                            AntminerConfValue.POOL_1_PASS,
                                                            AntminerConfValue.POOL_2_URL,
                                                            AntminerConfValue.POOL_2_USER,
                                                            AntminerConfValue.POOL_2_PASS,
                                                            AntminerConfValue.POOL_3_URL,
                                                            AntminerConfValue.POOL_3_USER,
                                                            AntminerConfValue.POOL_3_PASS,
                                                            AntminerConfValue.NO_BEEPER,
                                                            AntminerConfValue.NO_TEMP_OVER_CTRL,
                                                            AntminerConfValue.FAN_CTRL,
                                                            AntminerConfValue.FAN_PWM,
                                                            AntminerConfValue.FREQ),
                                                    context.getConfiguration()),
                                            new BraiinsChangePoolsAction(
                                                    context.getConfiguration(),
                                                    context.getObjectMapper()),
                                            new VnishAwareAction(
                                                    new VnishChangePoolsAction(
                                                            context.getConfiguration()),
                                                    new mn.foreman.antminer.vnish.v3.VnishChangePoolsAction(
                                                            context.getConfiguration())),
                                            new SeerChangePoolsAction(),
                                            context.getConfiguration())),
                            AsicActionFactory.toAsync(
                                    context.getThreadPool(),
                                    context.getBlacklist(),
                                    context.getStatsCache(),
                                    new AntminerFactory(
                                            1,
                                            context.getConfiguration()),
                                    new mn.foreman.antminer.FirmwareAwareAction(
                                            "antMiner Configuration",
                                            new StockRebootAction(
                                                    "antMiner Configuration",
                                                    context.getConfiguration()),
                                            new BraiinsRebootAction(),
                                            new VnishAwareAction(
                                                    new StockRebootAction(
                                                            "antMiner Configuration",
                                                            context.getConfiguration()),
                                                    new NullCompletableAction(true)),
                                            new StockRebootAction(
                                                    "antMiner Configuration",
                                                    context.getConfiguration()),
                                            context.getConfiguration()))),
            context ->
                    AsicActionFactory.toAsync(
                            context.getThreadPool(),
                            context.getBlacklist(),
                            context.getStatsCache(),
                            new AntminerFactory(
                                    1,
                                    context.getConfiguration()),
                            new mn.foreman.antminer.FirmwareAwareAction(
                                    "antMiner Configuration",
                                    new StockRebootAction(
                                            "antMiner Configuration",
                                            context.getConfiguration()),
                                    new BraiinsRebootAction(),
                                    new VnishAwareAction(
                                            new StockRebootAction(
                                                    "antMiner Configuration",
                                                    context.getConfiguration()),
                                            new VnishRebootAction(
                                                    context.getConfiguration())),
                                    new StockRebootAction(
                                            "antMiner Configuration",
                                            context.getConfiguration()),
                                    context.getConfiguration())),
            context ->
                    new ChainedAsicAction(
                            AsicActionFactory.toSync(
                                    new mn.foreman.antminer.FirmwareAwareAction(
                                            "antMiner Configuration",
                                            new StockFactoryResetAction(
                                                    "antMiner Configuration",
                                                    context.getConfiguration()),
                                            new BraiinsFactoryResetAction(),
                                            new VnishAwareAction(
                                                    new StockFactoryResetAction(
                                                            "antMiner Configuration",
                                                            context.getConfiguration()),
                                                    new NullCompletableAction()),
                                            new SeerFactoryResetAction(),
                                            context.getConfiguration()),
                                    60,
                                    TimeUnit.SECONDS),
                            AsicActionFactory.toAsync(
                                    context.getThreadPool(),
                                    context.getBlacklist(),
                                    context.getStatsCache(),
                                    new AntminerFactory(
                                            1,
                                            context.getConfiguration()),
                                    new mn.foreman.antminer.FirmwareAwareAction(
                                            "antMiner Configuration",
                                            new StockChangePoolsAction(
                                                    "antMiner Configuration",
                                                    Arrays.asList(
                                                            AntminerConfValue.POOL_1_URL,
                                                            AntminerConfValue.POOL_1_USER,
                                                            AntminerConfValue.POOL_1_PASS,
                                                            AntminerConfValue.POOL_2_URL,
                                                            AntminerConfValue.POOL_2_USER,
                                                            AntminerConfValue.POOL_2_PASS,
                                                            AntminerConfValue.POOL_3_URL,
                                                            AntminerConfValue.POOL_3_USER,
                                                            AntminerConfValue.POOL_3_PASS,
                                                            AntminerConfValue.NO_BEEPER,
                                                            AntminerConfValue.NO_TEMP_OVER_CTRL,
                                                            AntminerConfValue.FAN_CTRL,
                                                            AntminerConfValue.FAN_PWM,
                                                            AntminerConfValue.FREQ),
                                                    context.getConfiguration()),
                                            new BraiinsChangePoolsAction(
                                                    context.getConfiguration(),
                                                    context.getObjectMapper()),
                                            new VnishAwareAction(
                                                    new VnishChangePoolsAction(
                                                            context.getConfiguration()),
                                                    new mn.foreman.antminer.vnish.v3.VnishChangePoolsAction(
                                                            context.getConfiguration())),
                                            new SeerChangePoolsAction(),
                                            context.getConfiguration()))),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context ->
                    new SyncAsicAction(
                            new mn.foreman.antminer.FirmwareAwareAction(
                                    "antMiner Configuration",
                                    new StockLogsAction(
                                            context.getConfiguration(),
                                            "antMiner Configuration"),
                                    new NullCompletableAction(),
                                    new VnishAwareAction(
                                            new NullCompletableAction(),
                                            new VnishLogsAction(context.getConfiguration())),
                                    new NullCompletableAction(),
                                    context.getConfiguration()))),

    /** Whatsminer. */
    WHATSMINER(
            "whatsminer",
            (args, ip, configuration, objectMapper) ->
                    new FirmwareAwareDetectionStrategy(
                            new WhatsminerDetectionStrategy(
                                    new AwareMacStrategy(
                                            new WhatsminerMacStrategyMinerInfo(
                                                    ip,
                                                    4028,
                                                    args,
                                                    configuration),
                                            new WhatsminerMacStrategySummary(
                                                    ip,
                                                    4028,
                                                    args.get("password").toString(),
                                                    configuration),
                                            new WhatsminerMacStrategyOld(
                                                    ip,
                                                    80,
                                                    args.get("username").toString(),
                                                    args.get("password").toString(),
                                                    configuration)),
                                    new WhatsminerFactory(
                                            configuration).create(
                                            EntryStream
                                                    .of(args)
                                                    .append(
                                                            "apiIp",
                                                            ip)
                                                    .append(
                                                            "apiPort",
                                                            "4028")
                                                    .toMap()),
                                    configuration),
                            new CgMinerDetectionStrategy(
                                    CgMinerCommand.STATS,
                                    new WhatsminerTypeFactory(),
                                    new AwareMacStrategy(
                                            new WhatsminerMacStrategySummary(
                                                    ip,
                                                    4028,
                                                    args.get("password").toString(),
                                                    configuration),
                                            new WhatsminerMacStrategyOld(
                                                    ip,
                                                    80,
                                                    args.get("username").toString(),
                                                    args.get("password").toString(),
                                                    configuration)),
                                    new NullPatchingStrategy(),
                                    configuration)),
            context ->
                    new ChainedAsicAction(
                            AsicActionFactory.toSync(
                                    new WhatsminerFirmwareAwareAction(
                                            new WhatsminerChangePoolsActionOld(
                                                    context.getConfiguration()),
                                            new WhatsminerChangePoolsActionNew(
                                                    context.getConfiguration())),
                                    5,
                                    TimeUnit.SECONDS),
                            AsicActionFactory.toAsync(
                                    context.getThreadPool(),
                                    context.getBlacklist(),
                                    context.getStatsCache(),
                                    new WhatsminerFactory(context.getConfiguration()),
                                    new WhatsminerFirmwareAwareAction(
                                            new WhatsminerRebootActionOld(
                                                    context.getConfiguration()),
                                            new WhatsminerRebootActionNew(
                                                    context.getConfiguration())))),
            context ->
                    AsicActionFactory.toAsync(
                            context.getThreadPool(),
                            context.getBlacklist(),
                            context.getStatsCache(),
                            new WhatsminerFactory(context.getConfiguration()),
                            new WhatsminerFirmwareAwareAction(
                                    new WhatsminerRebootActionOld(
                                            context.getConfiguration()),
                                    new WhatsminerRebootActionNew(
                                            context.getConfiguration()))),
            context ->
                    AsicActionFactory.toAsync(
                            context.getThreadPool(),
                            context.getBlacklist(),
                            context.getStatsCache(),
                            new WhatsminerFactory(
                                    context.getConfiguration()),
                            new WhatsminerFactoryResetStrategy(
                                    context.getConfiguration())),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction(),
            context -> new NullAsicAction());

    /** The mapper. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** All of the known manufacturers. */
    private static final ConcurrentMap<String, Manufacturer> TYPES =
            new ConcurrentHashMap<>();

    static {
        for (final Manufacturer manufacturer : values()) {
            TYPES.put(
                    manufacturer.name,
                    manufacturer);
        }
    }

    /** The strategy for blinking LEDs. */
    private final ActionSupplier blinkStrategy;

    /** The strategy for changing pools. */
    private final ActionSupplier changePoolsStrategy;

    /** The strategy for adjusting the cooling mode. */
    private final ActionSupplier coolingModeStrategy;

    /** The strategy for detecting. */
    private final DetectionSupplier detectionStrategy;

    /** The strategy for performing a factory reset. */
    private final ActionSupplier factoryResetStrategy;

    /** The strategy for upgrading firmware. */
    private final ActionSupplier firmwareUpgradeStrategy;

    /** The strategy for obtaining logs. */
    private final ActionSupplier logStrategy;

    /** The name. */
    private final String name;

    /** The network strategy. */
    private final ActionSupplier networkStrategy;

    /** The strategy for opening APIs. */
    private final ActionSupplier openApiStrategy;

    /** The strategy for changing passwords. */
    private final ActionSupplier passwordStrategy;

    /** The strategy for configuring power modes. */
    private final ActionSupplier powerModeStrategy;

    /** The strategy for rebooting. */
    private final ActionSupplier rebootStrategy;

    /** The strategy for vnish overclocking. */
    private final ActionSupplier vnishOverclockStrategy;

    /**
     * Constructor.
     *
     * @param name                    The name.
     * @param detectionStrategy       The strategy for detecting.
     * @param changePoolsStrategy     The strategy for changing pools.
     * @param rebootStrategy          The strategy for rebooting.
     * @param factoryResetStrategy    The strategy for factory resets.
     * @param networkStrategy         The strategy for configuring the network.
     * @param powerModeStrategy       The strategy for configuring power modes.
     * @param passwordStrategy        The strategy for changing passwords.
     * @param blinkStrategy           The strategy for blinking LEDs.
     * @param firmwareUpgradeStrategy The strategy for upgrading firmware.
     * @param openApiStrategy         The strategy for opening APIs.
     * @param vnishOverclockStrategy  The vnish overclock strategy.
     * @param coolingModeStrategy     The cooling mode strategy.
     * @param logStrategy             The log strategy.
     */
    Manufacturer(
            final String name,
            final DetectionSupplier detectionStrategy,
            final ActionSupplier changePoolsStrategy,
            final ActionSupplier rebootStrategy,
            final ActionSupplier factoryResetStrategy,
            final ActionSupplier networkStrategy,
            final ActionSupplier powerModeStrategy,
            final ActionSupplier passwordStrategy,
            final ActionSupplier blinkStrategy,
            final ActionSupplier firmwareUpgradeStrategy,
            final ActionSupplier openApiStrategy,
            final ActionSupplier vnishOverclockStrategy,
            final ActionSupplier coolingModeStrategy,
            final ActionSupplier logStrategy) {
        this.name = name;
        this.detectionStrategy = detectionStrategy;
        this.changePoolsStrategy = changePoolsStrategy;
        this.rebootStrategy = rebootStrategy;
        this.factoryResetStrategy = factoryResetStrategy;
        this.networkStrategy = networkStrategy;
        this.powerModeStrategy = powerModeStrategy;
        this.passwordStrategy = passwordStrategy;
        this.blinkStrategy = blinkStrategy;
        this.firmwareUpgradeStrategy = firmwareUpgradeStrategy;
        this.openApiStrategy = openApiStrategy;
        this.vnishOverclockStrategy = vnishOverclockStrategy;
        this.coolingModeStrategy = coolingModeStrategy;
        this.logStrategy = logStrategy;
    }

    /**
     * Converts the provided name to a {@link Manufacturer}.
     *
     * @param name The name.
     *
     * @return The {@link Manufacturer}.
     */
    public static Optional<Manufacturer> fromName(final String name) {
        return Optional.ofNullable(TYPES.get(name.toLowerCase()));
    }

    /**
     * Returns the strategy for blinking LEDs.
     *
     * @param context The context.
     *
     * @return The strategy for blinking LEDs.
     */
    public AsicAction getBlinkStrategy(final ManufacturerContext context) {
        return this.blinkStrategy.create(context);
    }

    /**
     * Returns the strategy for changing pools.
     *
     * @param context The context.
     *
     * @return The strategy for changing pools.
     */
    public AsicAction getChangePoolsAction(final ManufacturerContext context) {
        return this.changePoolsStrategy.create(context);
    }

    /**
     * Returns the strategy for changing the cooling mode.
     *
     * @param context The context.
     *
     * @return The strategy for changing pools.
     */
    public AsicAction getCoolingMode(final ManufacturerContext context) {
        return this.coolingModeStrategy.create(context);
    }

    /**
     * Returns the strategy.
     *
     * @param args          The args.
     * @param ip            The IP.
     * @param configuration The configuration.
     *
     * @return The strategy.
     */
    public DetectionStrategy getDetectionStrategy(
            final Map<String, Object> args,
            final String ip,
            final ApplicationConfiguration configuration) {
        return this.detectionStrategy.create(
                args,
                ip,
                configuration,
                OBJECT_MAPPER);
    }

    /**
     * Returns the strategy for factory resets.
     *
     * @param context The context.
     *
     * @return The action for factory resets.
     */
    public AsicAction getFactoryResetStrategy(final ManufacturerContext context) {
        return this.factoryResetStrategy.create(context);
    }

    /**
     * Returns the strategy for upgrading firmware.
     *
     * @param context The context.
     *
     * @return The strategy for upgrading firmware.
     */
    public AsicAction getFirmwareUpgradeStrategy(final ManufacturerContext context) {
        return this.firmwareUpgradeStrategy.create(context);
    }

    /**
     * Returns the log strategy.
     *
     * @param context The content.
     *
     * @return The log strategy.
     */
    public AsicAction getLogStrategy(final ManufacturerContext context) {
        return this.logStrategy.create(context);
    }

    /**
     * Returns the name.
     *
     * @return The name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the strategy for configuring networks.
     *
     * @param context The context.
     *
     * @return The action for factory resets.
     */
    public AsicAction getNetworkStrategy(final ManufacturerContext context) {
        return this.networkStrategy.create(context);
    }

    /**
     * Returns the strategy for opening APIs.
     *
     * @param context The context.
     *
     * @return The action for factory resets.
     */
    public AsicAction getOpenApiStrategy(final ManufacturerContext context) {
        return this.openApiStrategy.create(context);
    }

    /**
     * Returns the strategy for changing passwords.
     *
     * @param context The context.
     *
     * @return The strategy for changing pools.
     */
    public AsicAction getPasswordStrategy(final ManufacturerContext context) {
        return this.passwordStrategy.create(context);
    }

    /**
     * Returns the strategy for changing power modes.
     *
     * @param context The context.
     *
     * @return The strategy for changing power modes.
     */
    public AsicAction getPowerModeStrategy(final ManufacturerContext context) {
        return this.powerModeStrategy.create(context);
    }

    /**
     * Returns the action for rebooting.
     *
     * @param context The context.
     *
     * @return The action for rebooting.
     */
    public AsicAction getRebootAction(final ManufacturerContext context) {
        return this.rebootStrategy.create(context);
    }

    /**
     * Returns the vnish overclock strategy.
     *
     * @param context The context.
     *
     * @return The strategy for overclocking vnish miners.
     */
    public AsicAction getVnishOverclockStrategy(final ManufacturerContext context) {
        return this.vnishOverclockStrategy.create(context);
    }

    /** Provides a supplier for creating {@link AsicAction actions}. */
    @FunctionalInterface
    private interface ActionSupplier {

        /**
         * Creates an {@link AsicAction}.
         *
         * @param context The context.
         *
         * @return The action.
         */
        AsicAction create(ManufacturerContext context);
    }

    /** A supplier for making new {@link DetectionStrategy detectors}. */
    private interface DetectionSupplier {

        /**
         * Creates a new detector.
         *
         * @param args          The args.
         * @param ip            The ip.
         * @param configuration The configuration.
         * @param objectMapper  The mapper.
         *
         * @return The strategy.
         */
        DetectionStrategy create(
                Map<String, Object> args,
                String ip,
                ApplicationConfiguration configuration,
                ObjectMapper objectMapper);
    }
}