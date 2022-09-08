package mn.foreman.model;

import mn.foreman.model.error.MinerException;
import mn.foreman.model.error.NotAuthenticatedException;

import java.util.Map;

/** An action that does nothing but fails. */
public class NullCompletableAction
        implements AsicAction.CompletableAction {

    /** The null response. */
    private final boolean result;

    /** Constructor. */
    public NullCompletableAction() {
        this(false);
    }

    /**
     * Constructor.
     *
     * @param result The result.
     */
    public NullCompletableAction(final boolean result) {
        this.result = result;
    }

    @Override
    public boolean run(
            final String ip,
            final int port,
            final Map<String, Object> args)
            throws NotAuthenticatedException, MinerException {
        return this.result;
    }
}
