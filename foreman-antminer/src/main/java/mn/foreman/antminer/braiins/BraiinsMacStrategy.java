package mn.foreman.antminer.braiins;

import mn.foreman.model.MacStrategy;
import mn.foreman.ssh.SshUtil;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Obtains the MAC from a BraiinsOS miner. */
public class BraiinsMacStrategy
        implements MacStrategy {

    /** The pattern for finding a MAC address. */
    private static final Pattern MAC_PATTERN =
            Pattern.compile("HWaddr (.{17})?");

    /** The IP. */
    private final String ip;

    /** The password. */
    private final String password;

    /** The username. */
    private final String username;

    /**
     * Constructor.
     *
     * @param ip       The IP.
     * @param username The username.
     * @param password The password.
     */
    public BraiinsMacStrategy(
            final String ip,
            final String username,
            final String password) {
        this.ip = ip;
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<String> getMacAddress() {
        final AtomicReference<String> mac = new AtomicReference<>();
        try {
            SshUtil.runMinerCommand(
                    ip,
                    this.username,
                    this.password,
                    "ifconfig",
                    s -> {
                        final Matcher matcher = MAC_PATTERN.matcher(s);
                        if (matcher.find()) {
                            mac.set(matcher.group(1));
                        }
                    });
        } catch (final Exception e) {
            // Ignore if we can't get the MAC
        }
        return Optional.ofNullable(mac.get());
    }
}