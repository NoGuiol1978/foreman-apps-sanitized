package mn.foreman.ssh;

import mn.foreman.model.error.MinerException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

/** Utility for sending SSH commands. */
public class SshUtil {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(SshUtil.class);

    /**
     * Runs a command over SSH.
     *
     * @param ip       The IP.
     * @param username The username.
     * @param password The password.
     * @param command  The command.
     * @param response The response callback.
     *
     * @throws MinerException on failure.
     */
    public static void runMinerCommand(
            final String ip,
            final String username,
            final String password,
            final String command,
            final Consumer<String> response)
            throws MinerException {
        runMinerCommand(
                ip,
                22,
                username,
                password,
                command,
                response);
    }

    /**
     * Runs a command over SSH.
     *
     * @param ip       The IP.
     * @param port     The port.
     * @param username The username.
     * @param password The password.
     * @param command  The command.
     * @param response The response callback.
     *
     * @throws MinerException on failure.
     */
    public static void runMinerCommand(
            final String ip,
            final int port,
            final String username,
            final String password,
            final String command,
            final Consumer<String> response)
            throws MinerException {
        Session session = null;
        Channel channel = null;

        try {
            final Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");

            final JSch jsch = new JSch();
            session =
                    jsch.getSession(
                            username,
                            ip,
                            port);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();

            final ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream();

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(byteArrayOutputStream);

            final InputStream in = channel.getInputStream();
            channel.connect();

            final StringBuilder outBuff = new StringBuilder();

            while (true) {
                for (int c; ((c = in.read()) >= 0); ) {
                    outBuff.append((char) c);
                }

                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
            }

            response.accept(outBuff.toString());

            LOG.info("SSH channel closed (status={})",
                    channel.getExitStatus());
        } catch (final Exception e) {
            throw new MinerException("Failed to connect to miner SSH");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
