package mn.foreman.antminer.stock;

import mn.foreman.antminer.AntminerUtils;
import mn.foreman.io.Query;
import mn.foreman.model.AbstractLogsAction;
import mn.foreman.model.ApplicationConfiguration;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** An action for obtaining logs from an antminer running stock firmware. */
public class StockLogsAction
        extends AbstractLogsAction {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(StockLogsAction.class);

    /** The realm. */
    private final String realm;

    /**
     * Constructor.
     *
     * @param configuration The configuration.
     * @param realm         The realm.
     */
    public StockLogsAction(
            final ApplicationConfiguration configuration,
            final String realm) {
        super(configuration);
        this.realm = realm;
    }

    @Override
    protected Optional<String> getLog(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final LogType logType) {
        try {
            if (logType == LogType.KERNEL) {
                return getKernelLog(
                        ip,
                        port,
                        parameters);
            }
        } catch (final Exception e) {
            LOG.warn("Exception occurred while querying for logs", e);
        }
        return Optional.empty();
    }

    /**
     * Obtains the kernel log.
     *
     * @param ip         The IP.
     * @param port       The port.
     * @param parameters The parameters.
     *
     * @return The log contents.
     *
     * @throws Exception on failure.
     */
    private Optional<String> getKernelLog(
            final String ip,
            final int port,
            final Map<String, Object> parameters) throws Exception {
        final String username =
                parameters.getOrDefault("username", "root").toString();
        final String password =
                parameters.getOrDefault("password", "root").toString();

        final ApplicationConfiguration.TimeConfig readConfig =
                this.configuration.getReadSocketTimeout();

        final Map<String, Object> minerConf =
                AntminerUtils.getConf(
                        ip,
                        port,
                        this.realm,
                        "/cgi-bin/get_miner_conf.cgi",
                        username,
                        password,
                        readConfig);

        final AtomicReference<String> log = new AtomicReference<>();

        final String uri;
        if (AntminerUtils.isNewGen(minerConf, parameters)) {
            uri = "/cgi-bin/log.cgi";
        } else {
            uri = "/cgi-bin/get_kernel_log.cgi";
        }

        Query.digestGet(
                ip,
                port,
                this.realm,
                uri,
                username,
                password,
                (integer, s) -> {
                    if (integer == HttpStatus.SC_OK) {
                        log.set(s);
                    }
                },
                readConfig.getTimeout(),
                readConfig.getTimeoutUnits());

        return Optional.ofNullable(log.get());
    }
}
