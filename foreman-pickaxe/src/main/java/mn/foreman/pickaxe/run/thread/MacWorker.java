package mn.foreman.pickaxe.run.thread;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.miners.Miners;
import mn.foreman.model.Miner;
import mn.foreman.model.MinerID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** A worker for continuously querying and updating MACs. */
public class MacWorker
        implements WorkerPool.Worker {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(MacWorker.class);

    /** The blacklisted miners. */
    private final Set<MinerID> blacklistMiners;

    /** The API handler. */
    private final AtomicReference<ForemanApi> foremanApi;

    /** The current miners. */
    private final AtomicReference<List<Miner>> miners;

    /** Whether running. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructor.
     *
     * @param miners          The miners.
     * @param blacklistMiners The blacklisted miners.
     * @param foremanApi      The API.
     */
    public MacWorker(
            final AtomicReference<List<Miner>> miners,
            final Set<MinerID> blacklistMiners,
            final AtomicReference<ForemanApi> foremanApi) {
        this.miners = miners;
        this.blacklistMiners = blacklistMiners;
        this.foremanApi = foremanApi;
    }

    @Override
    public void close() throws IOException {
        this.running.set(false);
    }

    @Override
    public void run() {
        this.running.set(true);
        try {
            while (this.running.get()) {
                LOG.info("Starting MAC querying...");
                try {
                    final Map<Miners.Miner, String> newMacs = new HashMap<>();
                    this.miners
                            .get()
                            .stream()
                            .filter(miner -> !this.blacklistMiners.contains(miner.getMinerID()))
                            .forEach(miner -> {
                                try {
                                    LOG.info("Attempting to obtain MAC for {}", miner);
                                    miner
                                            .getMacAddress()
                                            .map(String::toLowerCase)
                                            .ifPresent(mac -> {
                                                final Miners.Miner apiMiner =
                                                        new Miners.Miner();
                                                apiMiner.apiIp = miner.getIp();
                                                apiMiner.apiPort = miner.getApiPort();
                                                newMacs.put(
                                                        apiMiner,
                                                        mac);
                                            });
                                } catch (final Exception e) {
                                    LOG.warn("Exception occurred while querying for MAC: {}",
                                            miner,
                                            e);
                                }
                            });
                    this.foremanApi
                            .get()
                            .pickaxe()
                            .updateMacs(newMacs);
                } catch (final Throwable t) {
                    LOG.warn("Exception occurred while querying MACs", t);
                }

                try {
                    TimeUnit.MINUTES.sleep(10);
                } catch (final InterruptedException e) {
                    // Ignore
                }
            }
        } catch (final Exception e) {
            LOG.warn("Exception occurred", e);
        }
    }
}
