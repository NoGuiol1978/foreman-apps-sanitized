package mn.foreman.pickaxe.contraints;

/**
 * An {@link IpValidator} provides a mechanism for verifying that an IP address
 * or MAC address can be examined.
 */
public interface IpValidator {

    /**
     * Verifies the the provided IP or MAC are allowed to be processed.
     *
     * @param ip  The IP.
     * @param mac The MAC.
     *
     * @return Whether the IP or MAC is allowed.
     */
    boolean isAllowed(
            String ip,
            String mac);

    /**
     * Returns whether limiting is in place.
     *
     * @return Whether limiting is in place.
     */
    boolean isLimited();
}
