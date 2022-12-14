package mn.foreman.whatsminer.latest;

/** A command that can be sent to a whatsminer. */
public enum Command {

    /** status. */
    STATUS("status", false),

    /** update_pools. */
    UPDATE_POOLS("update_pools", true),

    /** reboot. */
    REBOOT("reboot", true),

    /** factory_reset. */
    FACTORY_RESET("factory_reset", true),

    /** get_miner_info. */
    GET_MINER_INFO("get_miner_info", false),

    /** get_psu. */
    GET_PSU("get_psu", false),

    /** get_error_code. */
    GET_ERROR_CODE("get_error_code", false),

    /** get_token. */
    GET_TOKEN("get_token", false);

    /** The command. */
    private final String command;

    /** Whether or not the command is a write. */
    private final boolean isWrite;

    /**
     * Constructor.
     *
     * @param command The command.
     * @param isWrite Whether or not the command is behind the write API.
     */
    Command(
            final String command,
            final boolean isWrite) {
        this.command = command;
        this.isWrite = isWrite;
    }

    /**
     * Returns the command.
     *
     * @return The command.
     */
    public String getCommand() {
        return this.command;
    }

    /**
     * Returns whether or not the command is a write.
     *
     * @return Whether or not the command is a write.
     */
    public boolean isWrite() {
        return this.isWrite;
    }
}
