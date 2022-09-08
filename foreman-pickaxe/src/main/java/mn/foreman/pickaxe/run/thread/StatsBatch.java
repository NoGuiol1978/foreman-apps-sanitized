package mn.foreman.pickaxe.run.thread;

import mn.foreman.model.miners.MinerStats;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** A batch of metrics to send. */
@Data
@Builder
public class StatsBatch {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(StatsBatch.class);

    /** The batch. */
    private final List<MinerStats> batch;

    /** The batch time. */
    private final ZonedDateTime batchTime;

    /**
     * Creates batches from the provided stats.
     *
     * @param stats     The stats.
     * @param batchSize The batch size.
     *
     * @return The stats.
     */
    public static List<StatsBatch> toBatches(
            final List<MinerStats> stats,
            final int batchSize) {
        LOG.debug("Batching {} stats using a size of {}", stats.size(), batchSize);
        
        final ZonedDateTime batchStartTime = ZonedDateTime.now();
        final List<List<MinerStats>> rawBatches =
                Lists.partition(
                        stats,
                        batchSize);
        final List<StatsBatch> batches = new ArrayList<>(rawBatches.size());
        for (int i = 0; i < rawBatches.size(); i++) {
            batches.add(
                    StatsBatch
                            .builder()
                            .batchTime(batchStartTime.plusSeconds(i))
                            .batch(rawBatches.get(i))
                            .build());
        }
        return batches;
    }
}
