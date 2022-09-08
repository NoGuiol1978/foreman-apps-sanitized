package mn.foreman.pickaxe.contraints;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * An {@link IpValidator} that verifies that the provided IPs fall within
 * configurable ranges.
 */
public class IpValidatorImpl
        implements IpValidator {

    /** The allowed macs. */
    private final AtomicReference<List<String>> allowedMacs;

    /** The allowed ranges. */
    private final AtomicReference<List<String>> allowedRanges;

    /**
     * Constructor.
     *
     * @param allowedRanges The allowed ranges.
     * @param allowedMacs   The allowed macs.
     */
    public IpValidatorImpl(
            final AtomicReference<List<String>> allowedRanges,
            final AtomicReference<List<String>> allowedMacs) {
        this.allowedRanges = allowedRanges;
        this.allowedMacs = allowedMacs;
    }

    @Override
    public boolean isAllowed(final String ip, final String mac) {
        return isAllowedIp(ip) || isAllowedMac(mac);
    }

    @Override
    public boolean isLimited() {
        return this.allowedMacs.get().stream().anyMatch(s -> !s.equals("*")) ||
                this.allowedRanges.get().stream().anyMatch(s -> !s.equals("0-255.0-255.0-255.0-255"));
    }

    /**
     * Verifies that the provided, split IP is within the provided range.
     *
     * @param ipSplit    The IP, split.
     * @param rangeSplit The candidate range, also split.
     *
     * @return Whether or not the IP fell into the range.
     */
    private static boolean checkRange(
            final String[] ipSplit,
            final String[] rangeSplit) {
        return IntStream
                .range(0, ipSplit.length)
                .allMatch(i -> {
                    boolean matched;
                    if (rangeSplit[i].contains("-")) {
                        // Split on dash, max, min
                        final String[] minMax = rangeSplit[i].split("-");
                        final int min = Integer.parseInt(minMax[0]);
                        final int max = Integer.parseInt(minMax[1]);
                        final int ipNumber = Integer.parseInt(ipSplit[i]);
                        matched = (min <= ipNumber && ipNumber <= max);
                    } else {
                        matched = ipSplit[i].equals(rangeSplit[i]);
                    }
                    return matched;
                });
    }

    /**
     * Checks if the IP is allowed.
     *
     * @param ip The IP.
     *
     * @return Whether allowed.
     */
    private boolean isAllowedIp(final String ip) {
        final List<String> allowedRanges = this.allowedRanges.get();
        final String[] ipSplit = ip.split("\\.");
        return allowedRanges
                .stream()
                .map(range -> range.split("\\."))
                .filter(range -> range.length == ipSplit.length)
                .anyMatch(
                        range ->
                                checkRange(
                                        ipSplit,
                                        range));
    }

    /**
     * Checks if the MAC is allowed.
     *
     * @param mac The MAC.
     *
     * @return Whether allowed.
     */
    private boolean isAllowedMac(final String mac) {
        final List<String> allowed = this.allowedMacs.get();
        return allowed.contains("*") ||
                this.allowedMacs
                        .get()
                        .stream()
                        .map(String::toLowerCase)
                        .anyMatch(s -> mac != null && mac.toLowerCase().equals(s));
    }
}
