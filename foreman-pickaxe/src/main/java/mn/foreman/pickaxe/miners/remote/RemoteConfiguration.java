package mn.foreman.pickaxe.miners.remote;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.pickaxe.Pickaxe;
import mn.foreman.api.model.ApiType;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.Miner;
import mn.foreman.model.MinerFactory;
import mn.foreman.model.error.MinerException;
import mn.foreman.pickaxe.contraints.IpValidator;
import mn.foreman.pickaxe.miners.MinerConfiguration;
import mn.foreman.pickaxe.util.MinerUtils;
import mn.foreman.util.EnvUtils;
import mn.foreman.util.VersionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A {@link RemoteConfiguration} provides a factory that will create the list of
 * {@link Miner miners} to query based on the results of a query to FOREMAN's
 * configuration API.  This allows users to configure their pickaxe instances
 * from the dashboard.
 */
public class RemoteConfiguration
        implements MinerConfiguration {

    /** The hostname. */
    private static final String HOSTNAME = EnvUtils.getHostname();

    /** The local IP. */
    private static final String IP = EnvUtils.getLanIp();

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(RemoteConfiguration.class);

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /** The Foreman API. */
    private final AtomicReference<ForemanApi> foremanApi;

    /** The IP validator. */
    private final IpValidator ipValidator;

    /**
     * Constructor.
     *
     * @param foremanApi    The Foreman API.
     * @param configuration The configuration.
     */
    public RemoteConfiguration(
            final AtomicReference<ForemanApi> foremanApi,
            final ApplicationConfiguration configuration,
            final IpValidator ipValidator) {
        this.foremanApi = foremanApi;
        this.configuration = configuration;
        this.ipValidator = ipValidator;
    }

    @Override
    public List<Miner> load()
            throws Exception {
        LOG.debug("Querying for miners");

        final ForemanApi foremanApi = this.foremanApi.get();

        final List<Pickaxe.MinerConfig> configs =
                foremanApi
                        .pickaxe()
                        .minerConfigs(
                                VersionUtils.getVersion(),
                                HOSTNAME,
                                IP)
                        .orElseThrow(() -> new MinerException("Failed to download config"));

        LOG.info("Downloaded configuration: {} miners", configs.size());

        return toMiners(
                configs,
                this.configuration);
    }

    /**
     * Adds nicehash candidates to the dest {@link List}.
     *
     * @param config        The config.
     * @param portStart     The port start.
     * @param candidates    The candidates.
     * @param dest          The destination {@link List}.
     * @param configuration The configuration.
     * @param validator     The validator.
     */
    private static void addNiceHashCandidates(
            final Pickaxe.MinerConfig config,
            final int portStart,
            final List<ApiType> candidates,
            final List<Miner> dest,
            final ApplicationConfiguration configuration,
            final IpValidator validator) {
        for (int i = 0; i < 5; i++) {
            final int port = portStart + i;
            dest.addAll(
                    candidates
                            .stream()
                            .map(apiType ->
                                    toMiner(
                                            apiType,
                                            port,
                                            config,
                                            configuration,
                                            validator))
                            .flatMap(List::stream)
                            .collect(Collectors.toList()));
        }
    }

    /**
     * Converts each {@link Pickaxe.MinerConfig} to a {@link Miner}.
     *
     * @param apiType       The {@link ApiType}.
     * @param port          The port.
     * @param config        The {@link Pickaxe.MinerConfig}.
     * @param configuration The configuration.
     * @param validator     The validator.
     *
     * @return The {@link Miner miners}.
     */
    private static List<Miner> toMiner(
            final ApiType apiType,
            final int port,
            final Pickaxe.MinerConfig config,
            final ApplicationConfiguration configuration,
            final IpValidator validator) {
        LOG.debug("Adding miner for {}", config);

        final MinerFactory minerFactory =
                MinerTypeFactory.toFactory(
                        apiType,
                        port,
                        configuration);

        final List<Miner> miners = new LinkedList<>();
        miners.add(MinerUtils.toMiner(port, config, minerFactory, validator));

        return miners;
    }

    /**
     * Creates a {@link Miner} from every miner in the {@link
     * Pickaxe.MinerConfig configs}.
     *
     * @param configs       The configurations.
     * @param configuration The configuration.
     *
     * @return The {@link Miner miners}.
     */
    private List<Miner> toMiners(
            final List<Pickaxe.MinerConfig> configs,
            final ApplicationConfiguration configuration) {
        return configs
                .stream()
                .filter(config -> config.apiType != null)
                .map(config ->
                        toMiner(
                                config.apiType,
                                config.apiPort,
                                config,
                                configuration,
                                this.ipValidator))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /** Provides a supplier for producing related miners. */
    @FunctionalInterface
    public interface MinerSupplier {

        /**
         * Creates the miners that will be used.
         *
         * @param config The config.
         *
         * @return The miners, if any.
         */
        List<Miner> create(Pickaxe.MinerConfig config);
    }
}