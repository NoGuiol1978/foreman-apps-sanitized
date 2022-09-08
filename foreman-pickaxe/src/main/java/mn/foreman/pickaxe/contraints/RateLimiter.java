package mn.foreman.pickaxe.contraints;

/**
 * A {@link RateLimiter} provides a mechanism to control the rate at which
 * commands can run against miners.
 */
public interface RateLimiter {

    /**
     * Attempts to run a command immediately, if it can be permitted.
     *
     * @param count The number of tokens the command is worth.
     *
     * @return Whether the command can run.
     */
    boolean consume(int count);

    /**
     * Returns the limit.
     *
     * @return The limit.
     */
    int getLimit();
}
