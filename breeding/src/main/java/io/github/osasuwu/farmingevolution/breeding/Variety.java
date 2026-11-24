package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A variety: a species plus a region of trait values. A plant gives the product of a variety
 * when it is of the species and every trait it expresses lies in the region.
 *
 * <p>Bounds are inclusive. A trait named in neither map is not bounded: any value of its range
 * fits.
 *
 * @param thresholds the smallest value the variety accepts, by trait
 * @param ceilings   the largest value the variety accepts, by trait
 */
public record Variety(String id, String species, Map<String, Integer> thresholds, Map<String, Integer> ceilings) {
    public Variety {
        thresholds = Collections.unmodifiableMap(new LinkedHashMap<>(thresholds));
        ceilings = Collections.unmodifiableMap(new LinkedHashMap<>(ceilings));
    }

    /** A variety that only sets smallest values. */
    public Variety(String id, String species, Map<String, Integer> thresholds) {
        this(id, species, thresholds, Map.of());
    }

    /** Whether the plant is of this variety's species and expresses a value inside every bound. */
    public boolean fits(Individual individual) {
        if (!species.equals(individual.species())) {
            return false;
        }
        for (String trait : bounded()) {
            int value = individual.trait(trait);
            if (value < lowest(trait) || value > highest(trait)) {
                return false;
            }
        }
        return true;
    }

    // Regions are compared bound by bound, with a trait that has no bound open on that side.
    // That is the comparison of the sets of values as long as every bound narrows its trait's
    // range, which PackData.parse checks: a bound on the edge of the range would be a second
    // way to write "no bound". The methods below take a variety of the same species.

    /** Whether every plant that fits this variety fits the other one too. */
    boolean inside(Variety other) {
        for (String trait : other.bounded()) {
            if (lowest(trait) < other.lowest(trait) || highest(trait) > other.highest(trait)) {
                return false;
            }
        }
        return true;
    }

    /** Whether some plant fits both varieties. */
    boolean overlaps(Variety other) {
        for (String trait : bounded(other)) {
            if (Math.max(lowest(trait), other.lowest(trait)) > Math.min(highest(trait), other.highest(trait))) {
                return false;
            }
        }
        return true;
    }

    /** Whether the plants that fit this variety are exactly those that fit both of the given ones. */
    boolean isSharedRegionOf(Variety first, Variety second) {
        for (String trait : bounded(first.bounded(second))) {
            if (lowest(trait) != Math.max(first.lowest(trait), second.lowest(trait))
                    || highest(trait) != Math.min(first.highest(trait), second.highest(trait))) {
                return false;
            }
        }
        return true;
    }

    private int lowest(String trait) {
        return thresholds.getOrDefault(trait, Integer.MIN_VALUE);
    }

    private int highest(String trait) {
        return ceilings.getOrDefault(trait, Integer.MAX_VALUE);
    }

    /** The traits this variety bounds on either side. */
    private Set<String> bounded() {
        return bounded(Set.of());
    }

    /** The traits bounded by this variety or named in the given set. */
    private Set<String> bounded(Set<String> others) {
        Set<String> traits = new LinkedHashSet<>(thresholds.keySet());
        traits.addAll(ceilings.keySet());
        traits.addAll(others);
        return traits;
    }

    private Set<String> bounded(Variety other) {
        return bounded(other.bounded());
    }
}
