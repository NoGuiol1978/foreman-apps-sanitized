package mn.foreman.pickaxe.command.asic.scan;

import mn.foreman.model.Detection;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Finds the detection by MAC. */
public class MacFilteringStrategy
        implements FilteringStrategy {

    @Override
    public boolean matches(
            final Detection detection,
            final List<String> macs,
            final List<String> workers) {
        final Map<String, Object> args = detection.getParameters();
        boolean matches =
                macs
                        .stream()
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList())
                        .contains(
                                args
                                        .getOrDefault("mac", "")
                                        .toString()
                                        .toLowerCase());
        if (!matches) {
            matches =
                    workers
                            .stream()
                            .filter(Objects::nonNull)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList())
                            .contains(
                                    args
                                            .getOrDefault("worker", "")
                                            .toString()
                                            .toLowerCase());
        }
        return matches;
    }
}
