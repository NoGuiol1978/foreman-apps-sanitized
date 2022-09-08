package mn.foreman.pickaxe.command.asic.scan;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static mn.foreman.pickaxe.command.util.CommandUtils.safeGet;

/**
 * An {@link IpSourceStrategy} implementation that will generate a scan range
 * from the provided start and stop positions.
 */
public class StartStopSourceStrategy
        implements IpSourceStrategy {

    @Override
    public List<String> toIps(Map<String, Object> args) {
        final long start = ipToLong(safeGet(args, "start"));
        final long stop = ipToLong(safeGet(args, "stop"));
        return LongStream
                .rangeClosed(start, stop)
                .mapToObj(StartStopSourceStrategy::ipFromLong)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /**
     * Converts the IP as a long to an IP address.
     *
     * @param ip IP to convert.
     *
     * @return The ip.
     */
    private static String ipFromLong(final long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >>> 24) & 0xff,
                (ip >>> 16) & 0xff,
                (ip >>> 8) & 0xff,
                (ip) & 0xff);
    }

    /**
     * Converts the IP as a string to a long.
     *
     * @param ip IP to convert.
     *
     * @return The ip.
     */
    @SuppressWarnings("UnstableApiUsage")
    private static long ipToLong(final String ip) {
        final InetAddress inetAddress = InetAddresses.forString(ip);
        final int ipInt = InetAddresses.coerceToInteger(inetAddress);
        return ipInt & 0xFFFFFFFFL;
    }
}
