package mn.foreman.pickaxe.command.asic;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** An enum representing all of the commands that can be executed. */
public enum RemoteCommand {

    /** Discover a miner. */
    DISCOVER("discover", false),

    /** Scan for miners. */
    SCAN("scan", false),

    /** Scan for miners over a range of IPs. */
    SCAN_RANGES("scan-ranges", false),

    /** Scan for a specific MAC address. */
    TARGETED_SCAN("targeted-scan", false),

    /** Range-based scan for a specific MAC address. */
    TARGETED_SCAN_RANGES("targeted-scan-ranges", false),

    /** Change miner pools. */
    CHANGE_POOLS("change-pools", true),

    /** Reboot miner. */
    REBOOT("reboot", true),

    /** Obtain raw stats from miner. */
    RAW_STATS("raw-stats", false),

    /** Factory reset. */
    FACTORY_RESET("factory-reset", true),

    /** Terminate agent. */
    TERMINATE("terminate", true),

    /** Fetching logs. */
    FETCH_LOGS("fetch-logs", false);

    /** The known types. */
    private static final Map<String, RemoteCommand> TYPES =
            new ConcurrentHashMap<>();

    static {
        for (final RemoteCommand command : values()) {
            TYPES.put(command.type, command);
        }
    }

    /** Whether or not the command is control. */
    private final boolean isControl;

    /** The type. */
    private final String type;

    /**
     * Constructor.
     *
     * @param type      The type.
     * @param isControl Whether or not control is enabled.
     */
    RemoteCommand(
            final String type,
            final boolean isControl) {
        this.type = type;
        this.isControl = isControl;
    }

    /**
     * Returns the type, if known.
     *
     * @param type The type.
     *
     * @return The known type.
     */
    public static Optional<RemoteCommand> forType(final String type) {
        return Optional.ofNullable(TYPES.get(type));
    }

    /**
     * Returns the type.
     *
     * @return The type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns whether or not the command is a control command.
     *
     * @return Whether or not the command is a control command.
     */
    public boolean isControl() {
        return this.isControl;
    }
}
