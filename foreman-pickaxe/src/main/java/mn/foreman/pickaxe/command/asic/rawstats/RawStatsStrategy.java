package mn.foreman.pickaxe.command.asic.rawstats;

import mn.foreman.api.endpoints.pickaxe.Pickaxe;
import mn.foreman.api.model.ApiType;
import mn.foreman.api.model.CommandDone;
import mn.foreman.api.model.CommandStart;
import mn.foreman.api.model.DoneStatus;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.Miner;
import mn.foreman.model.MinerFactory;
import mn.foreman.model.miners.MinerStats;
import mn.foreman.model.miners.asic.Asic;
import mn.foreman.pickaxe.command.CommandCompletionCallback;
import mn.foreman.pickaxe.command.CommandStrategy;
import mn.foreman.pickaxe.contraints.IpValidator;
import mn.foreman.pickaxe.miners.remote.MinerTypeFactory;
import mn.foreman.pickaxe.util.MinerUtils;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.*;

/**
 * A {@link RawStatsStrategy} provides a mechanism to push all of the raw,
 * flattended stats that can be obtained from a miner to the Foreman dashboard
 * so that they can be used by a user to construct a custom trigger.
 */
public class RawStatsStrategy
        implements CommandStrategy {

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /** The validator. */
    private final IpValidator validator;

    /**
     * Constructor.
     *
     * @param configuration The configuration.
     * @param validator     The validator.
     */
    public RawStatsStrategy(
            final ApplicationConfiguration configuration,
            final IpValidator validator) {
        this.configuration = configuration;
        this.validator = validator;
    }

    @Override
    public void runCommand(
            final CommandStart start,
            final CommandCompletionCallback commandCompletionCallback,
            final CommandDone.CommandDoneBuilder builder) {
        try {
            doRunCommand(
                    start,
                    builder,
                    commandCompletionCallback);
        } catch (final Exception e) {
            commandCompletionCallback.done(
                    start.id,
                    builder.status(
                            CommandDone.Status
                                    .builder()
                                    .type(DoneStatus.FAILED)
                                    .message(e.getMessage())
                                    .details(ExceptionUtils.getStackTrace(e))
                                    .build())
                            .build());
        }
    }

    /**
     * Converts the provided configuration to a config.
     *
     * @param apiIp   The API IP.
     * @param apiPort The API port.
     * @param apiType The API type.
     * @param params  The params.
     *
     * @return The config.
     */
    private static Pickaxe.MinerConfig toConfig(
            final String apiIp,
            final int apiPort,
            final ApiType apiType,
            final List<Map<String, Object>> params) {
        final Pickaxe.MinerConfig minerConfig = new Pickaxe.MinerConfig();
        minerConfig.apiIp = apiIp;
        minerConfig.apiPort = apiPort;
        minerConfig.apiType = apiType;
        minerConfig.params = toParams(params);
        return minerConfig;
    }

    /**
     * Converts the provided params to config params.
     *
     * @param params The params.
     *
     * @return The config params.
     */
    private static List<Pickaxe.MinerConfig.Param> toParams(
            final List<Map<String, Object>> params) {
        final List<Pickaxe.MinerConfig.Param> newParams = new LinkedList<>();
        params
                .stream()
                .map(map -> {
                    final Pickaxe.MinerConfig.Param param =
                            new Pickaxe.MinerConfig.Param();
                    param.key = map.get("key").toString();
                    param.value = map.get("value");
                    return param;
                })
                .forEach(newParams::add);

        final Pickaxe.MinerConfig.Param whitelist =
                new Pickaxe.MinerConfig.Param();
        whitelist.key = "statsWhitelist";
        whitelist.value = Collections.singletonList("all");
        newParams.add(whitelist);

        return newParams;
    }

    /**
     * Runs the command.
     *
     * @param start                     The command to run.
     * @param builder                   The done builder.
     * @param commandCompletionCallback The callback for when the command is
     *                                  done.
     *
     * @throws Exception on failure.
     */
    @SuppressWarnings("unchecked")
    private void doRunCommand(
            final CommandStart start,
            final CommandDone.CommandDoneBuilder builder,
            final CommandCompletionCallback commandCompletionCallback) throws Exception {
        final Map<String, Object> args = start.args;

        final String apiIp =
                args.get("apiIp").toString();
        final int apiPort =
                Integer.parseInt(args.get("apiPort").toString());
        final ApiType apiType =
                ApiType.forValue(
                        Integer.parseInt(
                                args.get("apiType").toString()));
        final List<Map<String, Object>> params =
                (List<Map<String, Object>>) args.get("params");

        final Pickaxe.MinerConfig minerConfig =
                toConfig(
                        apiIp,
                        apiPort,
                        apiType,
                        params);

        final MinerFactory minerFactory =
                MinerTypeFactory.toFactory(
                        apiType,
                        apiPort,
                        this.configuration);

        final Miner miner =
                MinerUtils.toMiner(
                        apiPort,
                        minerConfig,
                        minerFactory,
                        this.validator);
        final MinerStats minerStats =
                miner.getStats();

        // For now, only ASICs have stats
        final Map<String, Object> rawStats =
                minerStats
                        .getAsics()
                        .stream()
                        .map(Asic::getRawStats)
                        .collect(HashMap::new, Map::putAll, Map::putAll);
        commandCompletionCallback.done(
                start.id,
                builder
                        .result(
                                ImmutableMap.of(
                                        "stats",
                                        rawStats))
                        .status(
                                CommandDone.Status
                                        .builder()
                                        .type(DoneStatus.SUCCESS)
                                        .build())
                        .build());
    }
}