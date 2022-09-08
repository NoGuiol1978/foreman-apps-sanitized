package mn.foreman.antminer;

import mn.foreman.api.model.Pool;
import mn.foreman.io.Query;
import mn.foreman.model.AbstractChangePoolsAction;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * An {@link FirmwareAwareAction} provides an {@link AbstractChangePoolsAction}
 * implementation that will change the pools in use by an antminer device.
 */
public class StockChangePoolsAction
        extends AbstractChangePoolsAction {

    /** The mapper for json processing. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The props. */
    private final List<ConfValue> props;

    /** The digest realm. */
    private final String realm;

    /**
     * Constructor.
     *
     * @param realm                    The realm.
     * @param props                    The props.
     * @param applicationConfiguration The configuration.
     */
    public StockChangePoolsAction(
            final String realm,
            final List<ConfValue> props,
            final ApplicationConfiguration applicationConfiguration) {
        this.realm = realm;
        this.props = new ArrayList<>(props);
        this.applicationConfiguration = applicationConfiguration;
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
            final String username =
                    (String) parameters.getOrDefault("username", "");
            final String password =
                    (String) parameters.getOrDefault("password", "");

            final ApplicationConfiguration.TimeConfig writeConfig =
                    this.applicationConfiguration.getWriteSocketTimeout();
            final int adjustedWriteTimeout =
                    Integer.parseInt(
                            parameters.getOrDefault(
                                    "writeTimeout",
                                    writeConfig.getTimeout()).toString());
            final TimeUnit adjustedWriteTimeoutUnits =
                    TimeUnit.valueOf(
                            parameters.getOrDefault(
                                    "writeTimeoutUnits",
                                    writeConfig.getTimeoutUnits().name()).toString());

            final Map<String, Object> minerConf =
                    AntminerUtils.getConf(
                            ip,
                            port,
                            this.realm,
                            "/cgi-bin/get_miner_conf.cgi",
                            username,
                            password,
                            this.applicationConfiguration.getReadSocketTimeout());
            if (minerConf != null) {
                success =
                        changeConf(
                                parameters,
                                minerConf,
                                ip,
                                port,
                                username,
                                password,
                                pools,
                                adjustedWriteTimeout,
                                adjustedWriteTimeoutUnits);
            } else {
                throw new MinerException(
                        String.format(
                                "Failed to obtain a response from miner at %s:%d",
                                ip,
                                port));
            }
        } catch (final Exception e) {
            throw new MinerException(e);
        }

        return success;
    }

    /**
     * Creates a pool map from the provided pool.
     *
     * @param pool The pool.
     *
     * @return The pool config.
     */
    private static Map<String, String> toPool(final Pool pool) {
        return ImmutableMap.of(
                "url",
                pool.getUrl(),
                "user",
                pool.getUsername(),
                "pass",
                pool.getPassword());
    }

    /**
     * Creates pools from the pools.
     *
     * @param pools The source pools.
     *
     * @return The new pools.
     */
    private static List<Map<String, String>> toPools(final List<Pool> pools) {
        return pools
                .stream()
                .map(StockChangePoolsAction::toPool)
                .collect(Collectors.toList());
    }

    /**
     * Obtains a valid conf value.
     *
     * @param conf         The conf.
     * @param key          The key.
     * @param defaultValue The default value.
     *
     * @return The valid value.
     */
    private static String toValid(
            final Map<String, Object> conf,
            final String key,
            final String defaultValue) {
        String currentValue =
                conf.getOrDefault(
                        key,
                        defaultValue).toString();
        if (currentValue.isEmpty()) {
            currentValue = defaultValue;
        }
        return currentValue;
    }

    /**
     * Changes the configuration.
     *
     * @param parameters                The parameters.
     * @param minerConf                 The conf.
     * @param ip                        The ip.
     * @param port                      The port.
     * @param username                  The username.
     * @param password                  The password.
     * @param pools                     The new pools.
     * @param adjustedWriteTimeout      The adjusted write timeout.
     * @param adjustedWriteTimeoutUnits The adjusted write timeout (units).
     *
     * @return Whether or not the change was successful.
     *
     * @throws Exception on failure to communicate.
     */
    private boolean changeConf(
            final Map<String, Object> parameters,
            final Map<String, Object> minerConf,
            final String ip,
            final int port,
            final String username,
            final String password,
            final List<Pool> pools,
            final int adjustedWriteTimeout,
            final TimeUnit adjustedWriteTimeoutUnits)
            throws Exception {
        boolean evalResponse = false;
        List<Map<String, Object>> contentList = null;
        String payload = null;
        if (AntminerUtils.isNewGen(minerConf, parameters)) {
            final Map<String, Object> json =
                    ImmutableMap.of(
                            "bitmain-fan-ctrl",
                            Boolean.valueOf(
                                    toValid(
                                            minerConf,
                                            "bitmain-fan-ctrl",
                                            "false")),
                            "bitmain-fan-pwm",
                            toValid(
                                    minerConf,
                                    "bitmain-fan-pwm",
                                    "100"),
                            "miner-mode",
                            Integer.parseInt(
                                    toValid(
                                            minerConf,
                                            "bitmain-work-mode",
                                            "0")),
                            "freq-level",
                            Integer.parseInt(
                                    toValid(
                                            minerConf,
                                            "bitmain-freq-level",
                                            "100")),
                            "pools",
                            toPools(pools));
            payload = MAPPER.writeValueAsString(json);
            evalResponse = true;
        } else {
            final List<Map<String, Object>> content = new LinkedList<>();
            this.props.forEach(
                    confValue ->
                            confValue.getAndSet(
                                    parameters,
                                    minerConf,
                                    pools,
                                    content));
            contentList = content;
        }

        final AtomicBoolean status = new AtomicBoolean(false);
        Query.digestPost(
                ip,
                port,
                this.realm,
                "/cgi-bin/set_miner_conf.cgi",
                username,
                password,
                contentList,
                payload,
                (code, s) -> status.set(code == HttpStatus.SC_OK),
                evalResponse
                        ? new ApplicationConfiguration.TimeConfig(adjustedWriteTimeout, adjustedWriteTimeoutUnits)
                        : this.applicationConfiguration.getWriteSocketTimeout());

        return !evalResponse || status.get();
    }
}
