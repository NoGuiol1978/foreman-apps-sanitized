package mn.foreman.antminer.vnish.v2;

import mn.foreman.antminer.AntminerUtils;
import mn.foreman.cgminer.RequestFailureCallback;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;
import mn.foreman.model.miners.FanInfo;
import mn.foreman.model.miners.MinerStats;
import mn.foreman.model.miners.asic.Asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link VnishSleepModeInspectionCallback} provides a {@link
 * RequestFailureCallback} that enables an Antminer to be inspected for sleep
 * mode prior to aborting metrics detection against it.
 */
public class VnishSleepModeInspectionCallback
        implements RequestFailureCallback {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(VnishSleepModeInspectionCallback.class);

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The IP. */
    private final String ip;

    /**
     * How long until a missing miner needs to be considered no longer
     * sleeping.
     */
    private final AtomicLong missingCutoff = new AtomicLong(0);

    /** The parameters. */
    private final Map<String, Object> parameters;

    /** The port. */
    private final int port;

    /**
     * Constructor.
     *
     * @param ip                       The IP.
     * @param port                     The port.
     * @param parameters               The parameters.
     * @param applicationConfiguration The configuration.
     */
    public VnishSleepModeInspectionCallback(
            final String ip,
            final int port,
            final Map<String, Object> parameters,
            final ApplicationConfiguration applicationConfiguration) {
        this.ip = ip;
        this.port = port == 8081 || port == 8080 ? port : 80;
        this.parameters = parameters;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public boolean failed(
            final MinerStats.Builder builder,
            final MinerException minerException)
            throws MinerException {
        final AtomicBoolean isSleeping = new AtomicBoolean(false);
        try {
            final Optional<String> raw =
                    AntminerUtils.getRaw(
                            this.ip,
                            this.port,
                            "antMiner Configuration",
                            "/cgi-bin/monitor.cgi",
                            this.parameters.getOrDefault("username", "root").toString(),
                            this.parameters.getOrDefault("password", "root").toString(),
                            this.applicationConfiguration.getReadSocketTimeout());
            if (raw.isPresent()) {
                final String rawData = raw.get();
                if (!rawData.contains("bmminer")) {
                    isSleeping.set(true);

                    // Not sleeping if not reachable for more than 20 mins
                    this.missingCutoff.set(
                            System.currentTimeMillis() +
                                    TimeUnit.MINUTES.toMillis(20));

                }
            }
        } catch (final Exception e) {
            if (this.missingCutoff.get() > System.currentTimeMillis()) {
                isSleeping.set(true);
            } else {
                throw new MinerException(
                        "Missing for too long since sleeping",
                        e);
            }
        }

        if (isSleeping.get()) {
            LOG.debug("Sleep mode detected");
            builder.addAsic(
                    new Asic.Builder()
                            .setHashRate(BigDecimal.ZERO)
                            .setFanInfo(
                                    new FanInfo.Builder()
                                            .setCount(0)
                                            .setSpeedUnits("RPM")
                                            .build())
                            .setPowerMode(Asic.PowerMode.SLEEPING)
                            .build());
        } else {
            throw new MinerException("Miner not sleeping");
        }

        return false;
    }
}