package mn.foreman.antminer;

import mn.foreman.cgminer.Context;
import mn.foreman.model.miners.asic.Asic;

import java.util.Map;

/** A strategy for determining the power mode. */
public interface PowerModeStrategy {

    /**
     * Sets the power mode.
     *
     * @param builder    The builder.
     * @param values     The values.
     * @param hashRate   The hash rate.
     * @param boardCount The board count.
     * @param hasErrors  Whether or not hardware errors.
     * @param context    The context.
     */
    void setPowerMode(
            Asic.Builder builder,
            Map<String, String> values,
            double hashRate,
            int boardCount,
            boolean hasErrors,
            Context context);
}
