package mn.foreman.antminer;

import mn.foreman.api.model.Pool;
import mn.foreman.model.AbstractChangePoolsAction;
import mn.foreman.model.error.MinerException;

import java.util.List;
import java.util.Map;

/**
 * An {@link AbstractChangePoolsAction} implementation that will change the
 * pools on an Antminer running seer.
 */
public class SeerChangePoolsAction
        extends AbstractChangePoolsAction {

    @Override
    protected boolean doChange(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final List<Pool> pools)
            throws MinerException {
        throw new MinerException("seer pool changes not supported");
    }
}
