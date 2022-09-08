package mn.foreman.pickaxe.command.asic;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.MinerID;
import mn.foreman.model.cache.StatsCache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/** The context for creating manufacturers. */
@Data
@Builder
public class ManufacturerContext {

    /** The blacklist. */
    private final Set<MinerID> blacklist;

    /** The configuration. */
    private final ApplicationConfiguration configuration;

    /** The mapper. */
    private final ObjectMapper objectMapper;

    /** The cache. */
    private final StatsCache statsCache;

    /** The thread pool. */
    private final ScheduledExecutorService threadPool;
}
