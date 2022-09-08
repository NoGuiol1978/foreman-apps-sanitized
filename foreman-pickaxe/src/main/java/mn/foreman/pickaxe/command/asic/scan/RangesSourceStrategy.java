package mn.foreman.pickaxe.command.asic.scan;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An {@link IpSourceStrategy} implementation that will return a list of IPs to
 * scan that matches the provided IP ranges sent in the command.
 */
public class RangesSourceStrategy
        implements IpSourceStrategy {

    @SuppressWarnings("unchecked")
    @Override
    public List<String> toIps(final Map<String, Object> args) {
        return (List<String>) args.getOrDefault(
                        "ranges",
                        new LinkedList<String>());
    }
}
