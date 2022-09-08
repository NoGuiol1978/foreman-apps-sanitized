package mn.foreman.antminer.vnish;

import mn.foreman.model.AsicAction;
import mn.foreman.model.error.MinerException;
import mn.foreman.model.error.NotAuthenticatedException;

import java.util.Map;

/** Runs the appropriate action based on the miner model. */
public class VnishAwareAction
        implements AsicAction.CompletableAction {

    /** The new firmware. */
    private final AsicAction.CompletableAction newFirmware;

    /** The old firmware. */
    private final AsicAction.CompletableAction oldFirmware;

    /**
     * Constructor.
     *
     * @param oldFirmware The old firmware.
     * @param newFirmware The new firmware.
     */
    public VnishAwareAction(
            final AsicAction.CompletableAction oldFirmware,
            final AsicAction.CompletableAction newFirmware) {
        this.oldFirmware = oldFirmware;
        this.newFirmware = newFirmware;
    }

    @Override
    public boolean run(
            final String ip,
            final int port,
            final Map<String, Object> args)
            throws NotAuthenticatedException, MinerException {
        final String slug = args.getOrDefault("slug", "").toString();
        if (slug.contains("19")) {
            return this.newFirmware.run(
                    ip,
                    port,
                    args);
        }
        return this.oldFirmware.run(
                ip,
                port,
                args);
    }
}
