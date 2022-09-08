package mn.foreman.util;

/** A model object representation of a power unit. */
public enum PowerUnit {

    /** W. */
    W {
        @Override
        public double toW(final double value) {
            return value;
        }

        @Override
        public double toKW(final double value) {
            return value / 1_000;
        }

        @Override
        public double toMW(final double value) {
            return value / 1_000_000;
        }
    },

    /** kW. */
    kW {
        @Override
        public double toW(final double value) {
            return value * 1000;
        }

        @Override
        public double toKW(final double value) {
            return value;
        }

        @Override
        public double toMW(final double value) {
            return value / 1_000;
        }
    },

    /** MW. */
    MW {
        @Override
        public double toW(final double value) {
            return value * 1_000_000;
        }

        @Override
        public double toKW(final double value) {
            return value * 1_000;
        }

        @Override
        public double toMW(final double value) {
            return value;
        }
    };

    /** Constructor. */
    PowerUnit() {
        // Do nothing.
    }

    /**
     * Converts the provided value to kW.
     *
     * @param value The value to convert.
     *
     * @return The value in kW.
     */
    public double toKW(final double value) {
        throw new AbstractMethodError();
    }

    /**
     * Converts the provided value to MW.
     *
     * @param value The value to convert.
     *
     * @return The value in MW.
     */
    public double toMW(final double value) {
        throw new AbstractMethodError();
    }

    /**
     * Converts the provided value to W.
     *
     * @param value The value to convert.
     *
     * @return The value in W.
     */
    public double toW(final double value) {
        throw new AbstractMethodError();
    }
}
