package mn.foreman.model;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A {@link DetectionStrategy} decorator that verifies connectivity to a
 * particular IP and port before running the actual {@link #detectionStrategy}.
 */
public class SocketCheckingDetectionStrategy
        implements DetectionStrategy {

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /** The real strategy. */
    private final DetectionStrategy detectionStrategy;

    /** The port to check. */
    private final int port;

    /**
     * Constructor.
     *
     * @param port              The port to check.
     * @param detectionStrategy The real strategy.
     * @param configuration     The configuration.
     */
    public SocketCheckingDetectionStrategy(
            final int port,
            final DetectionStrategy detectionStrategy,
            final ApplicationConfiguration configuration) {
        this.port = port;
        this.detectionStrategy = detectionStrategy;
        this.configuration = configuration;
    }

    @Override
    public Optional<Detection> detect(
            final String ip,
            final int port,
            final Map<String, Object> args) {
        final ApplicationConfiguration.TimeConfig readConfig =
                this.configuration.getReadSocketTimeout();
        if (isListening(
                ip,
                this.port,
                readConfig.getTimeout(),
                readConfig.getTimeoutUnits())) {
            return this.detectionStrategy.detect(
                    ip,
                    port,
                    args);
        }
        return Optional.empty();
    }

    /**
     * Returns whether or not a server is listening on the provided IP and
     * port.
     *
     * @param ip                 The IP.
     * @param port               The port.
     * @param socketTimeout      The socket timeout.
     * @param socketTimeoutUnits The socket timeout (units).
     *
     * @return Whether or not a server is listening on the provided IP and
     *         port.
     */
    private static boolean isListening(
            final String ip,
            final int port,
            final int socketTimeout,
            final TimeUnit socketTimeoutUnits) {
        boolean listening = false;
        final InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
        try (final Socket socket = new Socket()) {
            socket.connect(
                    socketAddress,
                    (int) socketTimeoutUnits.toMillis(socketTimeout));
            listening = true;
        } catch (final Exception e) {
            // Ignore
        }
        return listening;
    }
}
