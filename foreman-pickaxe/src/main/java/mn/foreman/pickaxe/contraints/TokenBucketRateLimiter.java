package mn.foreman.pickaxe.contraints;

import java.util.concurrent.TimeUnit;

/**
 * A {@link TokenBucketRateLimiter} provides a {@link RateLimiter}
 * implementation that leverages the token bucket shaping algorithm to limit how
 * many commands can run at any point in time.
 */
@SuppressWarnings("UnstableApiUsage")
public class TokenBucketRateLimiter
        implements RateLimiter {

    /** The rate per second. */
    private final int limit;

    /** The shaping strategy. */
    private com.google.common.util.concurrent.RateLimiter tokenBucket;

    /**
     * Constructor.
     *
     * @param limit The rate per second.
     */
    public TokenBucketRateLimiter(final int limit) {
        this.limit = limit;
        if (this.limit > 0) {
            this.tokenBucket = com.google.common.util.concurrent.RateLimiter.create(this.limit);
        }
    }

    @Override
    public boolean consume(final int amount) {
        if (this.limit > 0) {
            return this.tokenBucket.tryAcquire(
                    amount,
                    1,
                    TimeUnit.SECONDS);
        }
        return false;
    }

    @Override
    public int getLimit() {
        return this.limit;
    }
}
