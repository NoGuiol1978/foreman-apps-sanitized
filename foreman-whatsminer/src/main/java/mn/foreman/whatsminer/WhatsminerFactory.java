package mn.foreman.whatsminer;

import mn.foreman.cgminer.*;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.cgminer.request.CgMinerRequest;
import mn.foreman.model.*;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A {@link MinerFactory} implementation that parses a configuration and creates
 * a {@link Miner} that will query a Whatsminer miner.
 */
public class WhatsminerFactory
        extends CgMinerFactory {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * Constructor.
     *
     * @param applicationConfiguration The configuration.
     */
    public WhatsminerFactory(
            final ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    protected Miner create(
            final String apiIp,
            final String apiPort,
            final List<String> statsWhitelist,
            final Map<String, Object> config) {
        final String username =
                config.getOrDefault("username", "admin").toString();
        final String password =
                config.getOrDefault("password", "admin").toString();
        final int apiPortInt = Integer.parseInt(apiPort);

        final Context cgContext = new Context();
        final ResponseStrategy oldFirmwareStrategy =
                new AggregatingResponseStrategy<>(
                        ImmutableMap.of(
                                "SUMMARY",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateSummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration,
                                                values,
                                                builder,
                                                cgContext),
                                "STATS",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateStats(
                                                values,
                                                builder)),
                        () -> null,
                        cgContext);

        final ResponseStrategy firmwareStrategy202009Details =
                new AggregatingResponseStrategy<>(
                        ImmutableMap.of(
                                "SUMMARY",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateSummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration,
                                                values,
                                                builder,
                                                cgContext),
                                "DEVDETAILS",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateDevDetails(
                                                values,
                                                builder),
                                "DEVS",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateDevs(
                                                values,
                                                builder,
                                                false)),
                        () -> null,
                        cgContext);

        final ResponseStrategy firmwareStrategy202009NoDetails =
                new AggregatingResponseStrategy<>(
                        ImmutableMap.of(
                                "SUMMARY",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateSummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration,
                                                values,
                                                builder,
                                                cgContext),
                                "DEVS",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateDevs(
                                                values,
                                                builder,
                                                false)),
                        () -> null,
                        cgContext);

        final ResponseStrategy firmwareStrategy202008FirmwareError =
                new AggregatingResponseStrategy<>(
                        ImmutableMap.of(
                                "DEVS",
                                (values, builder, context) ->
                                        WhatsminerUtils.updateDevs(
                                                values,
                                                builder,
                                                true)),
                        () -> null,
                        cgContext);

        return new FirmwareAwareMiner(
                // 202009
                new CgMiner.Builder(cgContext, statsWhitelist)
                        .setApiIp(apiIp)
                        .setApiPort(apiPort)
                        .setConnectTimeout(
                                this.applicationConfiguration.getReadSocketTimeout())
                        .setCommandKey("cmd")
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .addCommand(CgMinerCommand.POOLS)
                                        .addCommand(CgMinerCommand.SUMMARY)
                                        .addCommand(CgMinerCommand.DEVDETAILS)
                                        .addCommand(CgMinerCommand.EDEVS)
                                        .build(),
                                new MultiResponseStrategy(
                                        Arrays.asList(
                                                new PoolsResponseStrategy(
                                                        new MrrRigIdCallback(
                                                                cgContext)),
                                                firmwareStrategy202009Details)))
                        .setMacStrategy(
                                new AwareMacStrategy(
                                        new WhatsminerMacStrategyMinerInfo(
                                                apiIp,
                                                apiPortInt,
                                                config,
                                                this.applicationConfiguration),
                                        new WhatsminerMacStrategySummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration),
                                        new WhatsminerMacStrategyOld(
                                                apiIp,
                                                "127.0.0.1".equals(apiIp) ? 8080 : 80,
                                                username,
                                                password,
                                                this.applicationConfiguration)))
                        .setFailureCallback(
                                new SleepModeInspectionCallback(
                                        apiIp,
                                        apiPort,
                                        config,
                                        this.applicationConfiguration))
                        .build(),
                // 202008
                new CgMiner.Builder(cgContext, statsWhitelist)
                        .setApiIp(apiIp)
                        .setApiPort(apiPort)
                        .setConnectTimeout(
                                this.applicationConfiguration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.POOLS)
                                        .build(),
                                new PoolsResponseStrategy(
                                        new MrrRigIdCallback(cgContext)))
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.SUMMARY)
                                        .build(),
                                firmwareStrategy202009NoDetails)
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.EDEVS)
                                        .build(),
                                firmwareStrategy202009NoDetails)
                        .setMacStrategy(
                                new AwareMacStrategy(
                                        new WhatsminerMacStrategySummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration),
                                        new WhatsminerMacStrategyOld(
                                                apiIp,
                                                "127.0.0.1".equals(apiIp) ? 8080 : 80,
                                                username,
                                                password,
                                                this.applicationConfiguration)))
                        .setFailureCallback(
                                new SleepModeInspectionCallback(
                                        apiIp,
                                        apiPort,
                                        config,
                                        this.applicationConfiguration))
                        .build(),
                // 202007
                new CgMiner.Builder(cgContext, statsWhitelist)
                        .setApiIp(apiIp)
                        .setApiPort(apiPort)
                        .setConnectTimeout(
                                this.applicationConfiguration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.POOLS)
                                        .build(),
                                new PoolsResponseStrategy(
                                        new MrrRigIdCallback(cgContext)))
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.SUMMARY)
                                        .build(),
                                firmwareStrategy202009NoDetails)
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.DEVS)
                                        .build(),
                                firmwareStrategy202009NoDetails)
                        .setMacStrategy(
                                new AwareMacStrategy(
                                        new WhatsminerMacStrategySummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration),
                                        new WhatsminerMacStrategyOld(
                                                apiIp,
                                                "127.0.0.1".equals(apiIp) ? 8080 : 80,
                                                username,
                                                password,
                                                this.applicationConfiguration)))
                        .build(),
                // 202008 - broken API
                new CgMiner.Builder(cgContext, statsWhitelist)
                        .setApiIp(apiIp)
                        .setApiPort(apiPort)
                        .setConnectTimeout(
                                this.applicationConfiguration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.POOLS)
                                        .build(),
                                new PoolsResponseStrategy(
                                        new MrrRigIdCallback(cgContext)))
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.DEVS)
                                        .build(),
                                firmwareStrategy202008FirmwareError)
                        .setMacStrategy(
                                new AwareMacStrategy(
                                        new WhatsminerMacStrategySummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration),
                                        new WhatsminerMacStrategyOld(
                                                apiIp,
                                                "127.0.0.1".equals(apiIp) ? 8080 : 80,
                                                username,
                                                password,
                                                this.applicationConfiguration)))
                        .setFailureCallback(
                                new SleepModeInspectionCallback(
                                        apiIp,
                                        apiPort,
                                        config,
                                        this.applicationConfiguration))
                        .build(),
                // Old firmware
                new CgMiner.Builder(cgContext, statsWhitelist)
                        .setApiIp(apiIp)
                        .setApiPort(apiPort)
                        .setConnectTimeout(
                                this.applicationConfiguration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.POOLS)
                                        .build(),
                                new PoolsResponseStrategy(
                                        new MrrRigIdCallback(cgContext)))
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.SUMMARY)
                                        .build(),
                                oldFirmwareStrategy)
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.STATS)
                                        .build(),
                                oldFirmwareStrategy)
                        .setMacStrategy(
                                new AwareMacStrategy(
                                        new WhatsminerMacStrategySummary(
                                                apiIp,
                                                apiPortInt,
                                                password,
                                                this.applicationConfiguration),
                                        new WhatsminerMacStrategyOld(
                                                apiIp,
                                                "127.0.0.1".equals(apiIp) ? 8080 : 80,
                                                username,
                                                password,
                                                this.applicationConfiguration)))
                        .build());
    }
}