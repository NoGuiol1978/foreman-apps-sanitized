package mn.foreman.antminer.braiins;

import mn.foreman.antminer.AntminerType;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** All of the known bOS types. */
public enum BraiinsType {

    /** S9. */
    AM1_S9("am1-s9", AntminerType.BRAIINS_S9),

    /** S17. */
    AM2_S17("am2-s17", AntminerType.BRAIINS_S17),

    /** X17. */
    AM2_X17("am2-x17", AntminerType.BRAIINS_X17),

    /** S17 Pro. */
    S17PRO("Antminer S17 Pro", AntminerType.BRAIINS_S17_PRO),

    /** S17+. */
    S17P("Antminer S17+", AntminerType.BRAIINS_S17P),

    /** T19. */
    T19("Antminer T19", AntminerType.BRAIINS_T19),

    /** S19. */
    S19("Antminer S19", AntminerType.BRAIINS_S19),

    /** S19. */
    S19PT("BOSminer bosminer-plus-tuner", AntminerType.BRAIINS_S19),

    /** S19 PRO. */
    S19PRO("Antminer S19 PRO", AntminerType.BRAIINS_S19PRO),

    /** S19J PRO. */
    S19JPRO("Antminer S19J PRO", AntminerType.BRAIINS_S19JPRO);

    /** All of the known types. */
    private static final Map<String, BraiinsType> TYPES =
            new ConcurrentHashMap<>();

    static {
        for (final BraiinsType braiinsType : values()) {
            TYPES.put(braiinsType.slug, braiinsType);
        }
    }

    /** The type. */
    private final AntminerType antminerType;

    /** The slug. */
    private final String slug;

    /**
     * Constructor.
     *
     * @param slug         The slug.
     * @param antminerType The type.
     */
    BraiinsType(
            final String slug,
            final AntminerType antminerType) {
        this.slug = slug;
        this.antminerType = antminerType;
    }

    /**
     * Returns the type, if known.
     *
     * @param slug The type.
     *
     * @return The type.
     */
    public static Optional<BraiinsType> toType(final String slug) {
        return TYPES
                .values()
                .stream()
                .filter(braiinsType -> slug != null && slug.toLowerCase().contains(braiinsType.slug.toLowerCase()))
                .max(Comparator.comparing(type -> type.slug.length()));
    }

    /**
     * Returns the type.
     *
     * @return The type.
     */
    public AntminerType getType() {
        return this.antminerType;
    }
}
