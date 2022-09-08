package mn.foreman.antminer;

import mn.foreman.antminer.braiins.BraiinsMacStrategy;
import mn.foreman.antminer.response.antminer.StatsResponseStrategy;
import mn.foreman.antminer.response.braiins.BraiinsResponseStrategy;
import mn.foreman.antminer.vnish.v2.VnishSleepModeInspectionCallback;
import mn.foreman.antminer.vnish.v3.Vnish;
import mn.foreman.antminer.vnish.v3.VnishMacStrategy;
import mn.foreman.cgminer.*;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.cgminer.request.CgMinerRequest;
import mn.foreman.model.*;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link MinerFactory} implementation that parses a configuration and creates
 * a {@link Miner} that will query an Antminer running either stock firmware or
 * braiins.
 */
public class AntminerFactory
        extends CgMinerFactory {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The hash rate multiplier. */
    private final double multiplier;

    /**
     * Constructor.
     *
     * @param multiplier               The hash rate multiplier.
     * @param applicationConfiguration The configuration.
     */
    public AntminerFactory(
            final double multiplier,
            final ApplicationConfiguration applicationConfiguration) {
        this.multiplier = multiplier;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    protected Miner create(
            final String apiIp,
            final String apiPort,
            final List<String> statsWhitelist,
            final Map<String, Object> config) {
        final int port = Integer.parseInt(config.getOrDefault("port", "80").toString());
        final String username = config.getOrDefault("username", "").toString();
        final String password = config.getOrDefault("password", "").toString();

        final Context context = new Context();
        final Miner antminer =
                toMiner(
                        apiIp,
                        apiPort,
                        context,
                        statsWhitelist,
                        Arrays.asList(
                                ImmutableMap.of(
                                        CgMinerCommand.POOLS,
                                        new Request(
                                                new PoolsResponseStrategy(
                                                        new MrrRigIdCallback(context),
                                                        new LastShareTimeCallback(context)),
                                                true)),
                                ImmutableMap.of(
                                        CgMinerCommand.STATS,
                                        new Request(
                                                new RateMultiplyingDecorator(
                                                        "STATS",
                                                        "GHS 5s",
                                                        this.multiplier,
                                                        new StatsResponseStrategy(
                                                                context,
                                                                new StockPowerModeStrategy(
                                                                        apiIp,
                                                                        port,
                                                                        "antMiner Configuration",
                                                                        username,
                                                                        password,
                                                                        this.applicationConfiguration))),
                                                true))),
                        new StockMacStrategy(
                                apiIp,
                                port,
                                "antMiner Configuration",
                                username,
                                password,
                                this.applicationConfiguration),
                        this.applicationConfiguration,
                        null);

        final ResponseStrategy braiinsStrategy =
                new BraiinsResponseStrategy(
                        context);
        final Miner braiins =
                toMiner(
                        apiIp,
                        apiPort,
                        context,
                        statsWhitelist,
                        Arrays.asList(
                                ImmutableMap.of(
                                        CgMinerCommand.POOLS,
                                        new Request(
                                                new PoolsResponseStrategy(
                                                        new MrrRigIdCallback(context)),
                                                true)),
                                ImmutableMap.of(
                                        CgMinerCommand.SUMMARY,
                                        new Request(
                                                braiinsStrategy,
                                                true)),
                                ImmutableMap.of(
                                        CgMinerCommand.FANS,
                                        new Request(
                                                braiinsStrategy,
                                                true)),
                                ImmutableMap.of(
                                        CgMinerCommand.TEMPS,
                                        new Request(
                                                braiinsStrategy,
                                                true)),
                                ImmutableMap.of(
                                        CgMinerCommand.TUNERSTATUS,
                                        new Request(
                                                braiinsStrategy,
                                                false)),
                                ImmutableMap.of(
                                        CgMinerCommand.DEVS,
                                        new Request(
                                                braiinsStrategy,
                                                true))),
                        new BraiinsMacStrategy(
                                apiIp,
                                username,
                                password),
                        this.applicationConfiguration,
                        null);

        final Miner vnish =
                new AlternatingMiner(
                        apiIp,
                        Integer.parseInt(apiPort),
                        new Vnish(
                                apiIp,
                                Integer.parseInt(apiPort),
                                password,
                                this.applicationConfiguration,
                                config.containsKey("test")),
                        toMiner(
                                apiIp,
                                apiPort,
                                context,
                                statsWhitelist,
                                Arrays.asList(
                                        ImmutableMap.of(
                                                CgMinerCommand.POOLS,
                                                new Request(
                                                        new PoolsResponseStrategy(
                                                                new MrrRigIdCallback(context),
                                                                new LastShareTimeCallback(context)),
                                                        false)),
                                        ImmutableMap.of(
                                                CgMinerCommand.STATS,
                                                new Request(
                                                        new RateMultiplyingDecorator(
                                                                "STATS",
                                                                "GHS 5s",
                                                                this.multiplier,
                                                                new StatsResponseStrategy(
                                                                        context,
                                                                        new StockPowerModeStrategy(
                                                                                apiIp,
                                                                                port,
                                                                                "antMiner Configuration",
                                                                                username,
                                                                                password,
                                                                                this.applicationConfiguration))),
                                                        true))),
                                new AwareMacStrategy(
                                        new VnishMacStrategy(
                                                apiIp,
                                                port,
                                                password,
                                                this.applicationConfiguration),
                                        new StockMacStrategy(
                                                apiIp,
                                                port,
                                                "antMiner Configuration",
                                                username,
                                                password,
                                                this.applicationConfiguration)),
                                this.applicationConfiguration,
                                new VnishSleepModeInspectionCallback(
                                        apiIp,
                                        port,
                                        config,
                                        this.applicationConfiguration)));

        return new VersionDecorator(
                apiIp,
                apiPort,
                Integer.toString(port),
                "antMiner Configuration",
                username,
                password,
                context,
                antminer,
                braiins,
                vnish,
                this.applicationConfiguration);
    }

    /**
     * Creates a miner with the provided configuration.
     *
     * @param apiIp           The API IP.
     * @param apiPort         The API port.
     * @param context         The context.
     * @param statsWhitelist  The whitelist.
     * @param requests        The requests.
     * @param macStrategy     The MAC strategy.
     * @param failureCallback The failure callback.
     *
     * @return The new miner.
     */
    private static Miner toMiner(
            final String apiIp,
            final String apiPort,
            final Context context,
            final List<String> statsWhitelist,
            final List<Map<CgMinerCommand, Request>> requests,
            final MacStrategy macStrategy,
            final ApplicationConfiguration applicationConfiguration,
            final RequestFailureCallback failureCallback) {
        final CgMiner.Builder builder =
                new CgMiner.Builder(context,
                        statsWhitelist)
                        .setApiIp(apiIp)
                        .setApiPort(apiPort)
                        .setConnectTimeout(applicationConfiguration.getReadSocketTimeout())
                        .setMacStrategy(macStrategy);
        requests
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .forEach(entry ->
                        builder.addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(entry.getKey())
                                        .build(),
                                entry.getValue().responseStrategy,
                                entry.getValue().required));
        if (failureCallback != null) {
            builder.setFailureCallback(failureCallback);
        }
        return builder.build();
    }

    /** A request and whether or not the response is required. */
    private static class Request {

        /** Whether a response is required. */
        private final boolean required;

        /** The strategy for responses. */
        private final ResponseStrategy responseStrategy;

        /**
         * A pairing of response logic and whether a response is required.
         *
         * @param responseStrategy The response processor.
         * @param required         Whether required.
         */
        public Request(ResponseStrategy responseStrategy, boolean required) {
            this.responseStrategy = responseStrategy;
            this.required = required;
        }
    }
}