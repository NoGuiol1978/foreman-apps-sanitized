package mn.foreman.antminer.vnish.v3;

import mn.foreman.api.model.Pool;
import mn.foreman.model.AbstractChangePoolsAction;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;

import java.util.List;
import java.util.Map;

/** An action that will change the pools in use by a vnish device. */
public class VnishChangePoolsAction
        extends AbstractChangePoolsAction {

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /**
     * Constructor.
     *
     * @param configuration The configuration.
     */
    public VnishChangePoolsAction(final ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected boolean doChange(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final List<Pool> pools)
            throws MinerException {
        boolean success;
        try {
            final VnishV3Api.Context context =
                    VnishV3Api.login(
                            ip,
                            port,
                            parameters.getOrDefault("password", "root").toString(),
                            this.configuration)
                            .orElseThrow(() -> new MinerException("Failed to authenticate"));
            success = VnishV3Api.changePools(context, pools).orElse(false);
        } catch (final Exception e) {
            throw new MinerException(e);
        }
        return success;
    }
}
