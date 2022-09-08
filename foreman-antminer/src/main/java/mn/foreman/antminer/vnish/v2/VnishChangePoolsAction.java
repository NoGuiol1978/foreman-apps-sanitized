package mn.foreman.antminer.vnish.v2;

import mn.foreman.antminer.FirmwareAwareAction;
import mn.foreman.api.model.Pool;
import mn.foreman.io.Query;
import mn.foreman.model.AbstractChangePoolsAction;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;
import mn.foreman.util.PoolUtils;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link FirmwareAwareAction} provides an {@link AbstractChangePoolsAction}
 * implementation that will change the pools in use by an antminer device.
 */
public class VnishChangePoolsAction
        extends AbstractChangePoolsAction {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * Constructor.
     *
     * @param applicationConfiguration The configuration.
     */
    public VnishChangePoolsAction(
            final ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    protected boolean doChange(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final List<Pool> pools)
            throws MinerException {
        final List<Map<String, Object>> content = new LinkedList<>();
        for (int i = 1; i <= 3; i++) {
            final Pool pool = pools.get(i - 1);
            final String url = pool.getUrl();
            if (url != null && !url.isEmpty()) {
                content.add(
                        ImmutableMap.of(
                                "key",
                                String.format(
                                        "_ant_pool%durl",
                                        i),
                                "value",
                                PoolUtils.sanitizeUrl(url)));
                content.add(
                        ImmutableMap.of(
                                "key",
                                String.format(
                                        "_ant_pool%duser",
                                        i),
                                "value",
                                pool.getUsername()));
                content.add(
                        ImmutableMap.of(
                                "key",
                                String.format(
                                        "_ant_pool%dpw",
                                        i),
                                "value",
                                pool.getPassword()));
            }
        }

        final AtomicBoolean success = new AtomicBoolean(false);
        try {
            Query.digestPost(
                    ip,
                    // Test hook
                    port != 8080 && port != 8081 ? 80 : port,
                    "antMiner Configuration",
                    "/cgi-bin/set_miner_conf.cgi",
                    (String) parameters.getOrDefault("username", ""),
                    (String) parameters.getOrDefault("password", ""),
                    content,
                    null,
                    (integer, s) -> {
                    },
                    this.applicationConfiguration.getWriteSocketTimeout());
            success.set(true);
        } catch (final Exception e) {
            // Ignore
        }

        return success.get();
    }
}
