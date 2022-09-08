package mn.foreman.antminer.vnish.v3;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.MacStrategy;

import java.util.Optional;

/** Obtains the MAC from vnish antminer firmware. */
public class VnishMacStrategy
        implements MacStrategy {

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The IP. */
    private final String ip;

    /** The password. */
    private final String password;

    /** The port. */
    private final int port;

    /**
     * Constructor.
     *
     * @param ip                       The IP.
     * @param port                     The port.
     * @param password                 The password.
     * @param applicationConfiguration The configuration.
     */
    public VnishMacStrategy(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration applicationConfiguration) {
        this.ip = ip;
        this.port = port;
        this.password = password;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public Optional<String> getMacAddress() {
        Optional<String> mac = Optional.empty();
        try {
            final Optional<VnishV3Api.Context> contextOptional =
                    VnishV3Api.login(
                            this.ip,
                            this.port,
                            this.password,
                            this.applicationConfiguration);
            if (contextOptional.isPresent()) {
                mac = VnishV3Api
                        .summary(contextOptional.get())
                        .filter(summary -> summary.system != null && summary.system.network != null)
                        .map(summary -> summary.system.network.mac);
            }
        } catch (final Exception e) {
            // Ignore
        }
        return mac;
    }
}