package mn.foreman.model;

import mn.foreman.model.metadata.Metadata;
import mn.foreman.model.miners.MinerStats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The following POJO represents a JSON object with the following format:
 *
 * <pre>
 *   {
 *     "metadata": {
 *         ...
 *     },
 *     "miners": [
 *         ...
 *     ],
 *     "loopTimeMs": 100
 *   }
 * </pre>
 */
public class MetricsReport {

    /** The loop time (milliseconds). */
    private final long loopTimeMs;

    /** The {@link Metadata}. */
    private final Metadata metadata;

    /** The {@link MinerStats stats}. */
    private final List<MinerStats> miners;

    /**
     * Constructor.
     *
     * @param metadata   The {@link Metadata}.
     * @param stats      The {@link MinerStats stats}.
     * @param loopTimeMs The loop time (milliseconds).
     */
    private MetricsReport(
            @JsonProperty("metadata") final Metadata metadata,
            @JsonProperty("miners") final List<MinerStats> stats,
            @JsonProperty("loopTimeMs") final long loopTimeMs) {
        Validate.notNull(
                metadata,
                "metadata cannot be null");
        Validate.notEmpty(
                stats,
                "stats cannot be empty");
        this.metadata = metadata;
        this.miners = new ArrayList<>(stats);
        this.loopTimeMs = loopTimeMs;
    }

    @Override
    public boolean equals(final Object other) {
        final boolean isEqual;
        if (other == null) {
            isEqual = false;
        } else if (getClass() != other.getClass()) {
            isEqual = false;
        } else {
            final MetricsReport metricsReport = (MetricsReport) other;
            isEqual =
                    new EqualsBuilder()
                            .append(this.metadata, metricsReport.metadata)
                            .append(this.miners, metricsReport.miners)
                            .append(this.loopTimeMs, metricsReport.loopTimeMs)
                            .isEquals();
        }
        return isEqual;
    }

    /**
     * Returns the loop time.
     *
     * @return The loop time.
     */
    public long getLoopTimeMs() {
        return this.loopTimeMs;
    }

    /**
     * Returns the {@link Metadata}.
     *
     * @return The {@link Metadata}.
     */
    public Metadata getMetadata() {
        return this.metadata;
    }

    /**
     * Returns the {@link MinerStats stats}.
     *
     * @return The {@link MinerStats stats}.
     */
    public List<MinerStats> getMiners() {
        return this.miners;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.metadata)
                .append(this.miners)
                .append(this.loopTimeMs)
                .build();
    }

    @Override
    public String toString() {
        return String.format(
                "%s [ metadata=%s, miners=%s, loopTimeMs=%s ]",
                getClass().getSimpleName(),
                this.metadata,
                this.miners,
                this.loopTimeMs);
    }

    /** A builder for creating {@link MetricsReport reports}. */
    public static class Builder
            extends AbstractBuilder<MetricsReport> {

        /** The {@link MinerStats stats}. */
        private final List<MinerStats> minerStats = new LinkedList<>();

        /** The loop time. */
        private long loopTimeMs;

        /** The {@link Metadata}. */
        private Metadata metadata = new Metadata.Builder().build();

        /**
         * Adds a {@link MinerStats}.
         *
         * @param stats The {@link MinerStats} to add.
         *
         * @return The builder instance.
         */
        public Builder addMinerStats(final MinerStats stats) {
            this.minerStats.add(stats);
            return this;
        }

        @Override
        public MetricsReport build() {
            return new MetricsReport(
                    this.metadata,
                    this.minerStats,
                    this.loopTimeMs);
        }

        /**
         * Sets the loop time.
         *
         * @param loopTimeMs The loop time.
         *
         * @return This builder instance.
         */
        public Builder setLoopTimeMs(final long loopTimeMs) {
            this.loopTimeMs = loopTimeMs;
            return this;
        }

        /**
         * Sets the metadata.
         *
         * @param metadata The metadata.
         *
         * @return The builder instance.
         */
        public Builder setMetadata(final Metadata metadata) {
            this.metadata = metadata;
            return this;
        }
    }
}