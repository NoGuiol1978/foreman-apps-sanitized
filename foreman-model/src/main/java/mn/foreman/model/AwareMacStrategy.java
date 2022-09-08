package mn.foreman.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** A {@link MacStrategy} that will evaluate candidates. */
public class AwareMacStrategy
        implements MacStrategy {

    /** The firmware. */
    private final List<MacStrategy> firmware;

    /**
     * Constructor.
     *
     * @param firmware The firmware.
     */
    public AwareMacStrategy(final MacStrategy... firmware) {
        this.firmware = Arrays.asList(firmware);
    }

    @Override
    public Optional<String> getMacAddress() {
        return this.firmware
                .stream()
                .map(firmware -> {
                    Optional<String> mac = Optional.empty();
                    try {
                        mac = firmware.getMacAddress();
                    } catch (final Exception e) {
                        // Ignore
                    }
                    return mac;
                })
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get);
    }

}
