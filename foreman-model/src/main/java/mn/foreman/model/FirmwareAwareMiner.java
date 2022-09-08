package mn.foreman.model;

import mn.foreman.model.error.MinerException;
import mn.foreman.model.miners.MinerStats;
import mn.foreman.model.miners.asic.Asic;

import com.google.common.collect.Iterables;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A firmware-aware miner is a miner that will try to obtain stats from two
 * different versions of a miner.
 */
public class FirmwareAwareMiner
        implements Miner {

    /** The firmware. */
    private final List<Miner> firmwares;

    /**
     * Constructor.
     *
     * @param firmwares The firmware to try.
     */
    public FirmwareAwareMiner(final Miner... firmwares) {
        this.firmwares = Arrays.asList(firmwares);
    }

    @Override
    public int getApiPort() {
        final Miner firmware = Iterables.get(this.firmwares, 0);
        if (firmware != null) {
            return firmware.getApiPort();
        }
        return 0;
    }

    @Override
    public String getIp() {
        final Miner firmware = Iterables.get(this.firmwares, 0);
        if (firmware != null) {
            return firmware.getIp();
        }
        return null;
    }

    @Override
    public Optional<String> getMacAddress() {
        for (final Miner miner : this.firmwares) {
            try {
                final Optional<String> macAddress = miner.getMacAddress();
                if (macAddress.isPresent()) {
                    return macAddress;
                }
            } catch (final Exception e) {
                // Ignore and try the next
            }
        }
        return Optional.empty();
    }

    @Override
    public MinerID getMinerID() {
        final Miner firmware = Iterables.get(this.firmwares, 0);
        if (firmware != null) {
            return firmware.getMinerID();
        }
        return null;
    }

    @Override
    public MinerStats getStats()
            throws MinerException {
        for (final Miner miner : this.firmwares) {
            try {
                final MinerStats stats = miner.getStats();
                final List<Asic> asics = stats.getAsics();
                if (asics != null && !asics.isEmpty()) {
                    return stats;
                }
            } catch (final Exception e) {
                // Ignore
            }
        }

        throw new MinerException("Miner didn't respond");
    }
}
