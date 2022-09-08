package mn.foreman.model;

import mn.foreman.api.model.Pool;
import mn.foreman.model.error.MinerException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A {@link AsicAction.CompletableAction} implementation that parses pools. */
public abstract class AbstractChangePoolsAction
        implements AsicAction.CompletableAction {

    /** The required pools. */
    private final int requiredPools;

    /** Constructor. */
    protected AbstractChangePoolsAction() {
        this(3);
    }

    /**
     * Constructor.
     *
     * @param requiredPools The required pools.
     */
    protected AbstractChangePoolsAction(final int requiredPools) {
        this.requiredPools = requiredPools;
    }

    @Override
    public boolean run(
            final String ip,
            final int port,
            final Map<String, Object> parameters)
            throws MinerException {
        if (parameters.containsKey("pools")) {
            return runChange(
                    ip,
                    port,
                    parameters);
        }
        return true;
    }

    /**
     * Performs the change.
     *
     * @param ip         The ip.
     * @param port       The port.
     * @param parameters The parameters.
     * @param pools      The pools.
     *
     * @return Whether or not the pools were changed.
     *
     * @throws MinerException on failure.
     */
    protected abstract boolean doChange(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final List<Pool> pools) throws MinerException;

    /**
     * Converts the provided command args to a list of new pools.
     *
     * @param args The arguments to inspect.
     *
     * @return The new pools.
     */
    @SuppressWarnings("unchecked")
    private static List<Pool> toPools(final Map<String, Object> args) {
        final List<Map<String, String>> pools =
                (List<Map<String, String>>) args.get("pools");
        return pools
                .stream()
                .map(pool -> Pool
                        .builder()
                        .url(pool.getOrDefault("url", ""))
                        .username(pool.getOrDefault("user", ""))
                        .password(pool.getOrDefault("pass", ""))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Runs a pool change.
     *
     * @param ip         The ip.
     * @param port       The port.
     * @param parameters The parameters.
     *
     * @return The result.
     *
     * @throws MinerException on failure.
     */
    private boolean runChange(
            final String ip,
            final int port,
            final Map<String, Object> parameters) throws MinerException {
        final List<Pool> pools = toPools(parameters);
        if (pools.size() < this.requiredPools) {
            throw new MinerException("3 pools are required");
        }
        return doChange(
                ip,
                port,
                parameters,
                pools);
    }
}
