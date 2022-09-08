package mn.foreman.antminer.vnish.v3;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.AsicAction;
import mn.foreman.model.error.MinerException;
import mn.foreman.model.error.NotAuthenticatedException;

import java.util.Map;

/** Reboots a vnish miner. */
public class VnishRebootAction
        implements AsicAction.CompletableAction {

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /**
     * Constructor.
     *
     * @param configuration The configuration.
     */
    public VnishRebootAction(final ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean run(
            final String ip,
            final int port,
            final Map<String, Object> args)
            throws NotAuthenticatedException, MinerException {
        boolean success;
        try {
            final VnishV3Api.Context context =
                    VnishV3Api.login(
                            ip,
                            port,
                            args.getOrDefault("password", "root").toString(),
                            this.configuration)
                            .orElseThrow(() -> new MinerException("Failed to authenticate"));
            success = VnishV3Api.reboot(context).orElse(false);
        } catch (final Exception e) {
            throw new MinerException(e);
        }
        return success;
    }
}