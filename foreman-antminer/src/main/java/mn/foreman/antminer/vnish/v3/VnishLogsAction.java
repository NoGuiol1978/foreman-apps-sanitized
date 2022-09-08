package mn.foreman.antminer.vnish.v3;

import mn.foreman.model.AbstractLogsAction;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/** An action for obtaining logs from an antminer running stock firmware. */
public class VnishLogsAction
        extends AbstractLogsAction {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(VnishLogsAction.class);

    /**
     * Constructor.
     *
     * @param configuration The configuration.
     */
    public VnishLogsAction(
            final ApplicationConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected Optional<String> getLog(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final LogType logType) {
        Optional<String> result = Optional.empty();
        try {
            final VnishV3Api.Context context =
                    VnishV3Api.login(
                            ip,
                            port,
                            parameters.getOrDefault("password", "root").toString(),
                            this.configuration)
                            .orElseThrow(() -> new MinerException("Failed to authenticate"));

            switch (logType) {
                case KERNEL:
                    result = VnishV3Api.systemLogs(context);
                    break;
                case MINER:
                    result = VnishV3Api.minerLogs(context);
                    break;
                case AUTOTUNE:
                    result = VnishV3Api.autotuneLogs(context);
                    break;
            }
        } catch (final Exception e) {
            LOG.warn("Exception occurred while querying for logs", e);
        }
        return result;
    }
}
