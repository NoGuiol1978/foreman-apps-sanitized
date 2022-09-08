package mn.foreman.pickaxe.run;

import mn.foreman.model.miners.MinerStats;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Sends metrics to the dashboard. */
public interface MetricsSender {

    /**
     * Sends the provided metrics to the dashboard.
     *
     * @param queryTimeMs The query time (milliseconds).
     * @param publishTime The publish time.
     * @param stats       The metrics to send.
     */
    void sendMetrics(
            AtomicLong queryTimeMs,
            ZonedDateTime publishTime,
            List<MinerStats> stats);
}
