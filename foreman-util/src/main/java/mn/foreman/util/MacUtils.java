package mn.foreman.util;

/** Utilities for processing MAC addresses. */
public class MacUtils {

    /**
     * Adds colons to a MAC address.
     *
     * @param mac The MAC.
     *
     * @return The MAC.
     */
    public static String addColons(final String mac) {
        String newMac = mac;
        if (!mac.contains(":")) {
            final StringBuilder stringBuilder = new StringBuilder(mac);
            for (int i = 2; i < mac.length() + (i / 3); i += 3) {
                stringBuilder.insert(i, ':');
            }
            newMac = stringBuilder.toString();
        }
        return newMac;
    }
}
