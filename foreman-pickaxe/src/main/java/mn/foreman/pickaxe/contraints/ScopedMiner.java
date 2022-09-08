package mn.foreman.pickaxe.contraints;

import mn.foreman.model.Miner;
import mn.foreman.model.MinerID;
import mn.foreman.model.error.MinerException;
import mn.foreman.model.miners.MinerStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A {@link ScopedMiner} provides a {@link Miner} implementation that will limit
 * access to a miner based on a GUARDrail configuration.
 */
public class ScopedMiner
        implements Miner {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(ScopedMiner.class);

    /** The real miner. */
    private final Miner real;

    /** The validator. */
    private final IpValidator validator;

    /**
     * Constructor.
     *
     * @param real      The real miner.
     * @param validator The validator.
     */
    public ScopedMiner(
            final Miner real,
            final IpValidator validator) {
        this.real = real;
        this.validator = validator;
    }

    @Override
    public int getApiPort() {
        return this.real.getApiPort();
    }

    @Override
    public String getIp() {
        return this.real.getIp();
    }

    @Override
    public Optional<String> getMacAddress() {
        return this.real.getMacAddress();
    }

    @Override
    public MinerID getMinerID() {
        return this.real.getMinerID();
    }

    @Override
    public MinerStats getStats() throws MinerException {
        if (this.validator.isLimited()) {
            LOG.debug("Miner is scoped - check if it's allowed");
            final String ip = this.real.getIp();
            final String mac = this.real.getMacAddress().orElse(null);
            if (!this.validator.isAllowed(ip, mac)) {
                throw new MinerException("Denied by GUARDrail");
            }
        }
        return this.real.getStats();
    }
}
