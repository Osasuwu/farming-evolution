package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A raw-material requirement: a name a recipe refers to, for a species plus a region of trait
 * values. The region is written like the region of a variety. A requirement is passed by a
 * variety, not by a plant.
 *
 * @param thresholds the smallest value the requirement accepts, by trait
 * @param ceilings   the largest value the requirement accepts, by trait
 */
public record Requirement(String id, String species, Map<String, Integer> thresholds, Map<String, Integer> ceilings) {
    public Requirement {
        thresholds = Collections.unmodifiableMap(new LinkedHashMap<>(thresholds));
        ceilings = Collections.unmodifiableMap(new LinkedHashMap<>(ceilings));
    }

    /**
     * Whether the variety passes: it is of this species and its whole region lies inside the
     * region of the requirement, so every plant that gives its product is inside. A variety
     * that leaves a trait unbounded on a side the requirement bounds does not pass.
     */
    public boolean passedBy(Variety variety) {
        return species.equals(variety.species()) && variety.inside(new Variety(id, species, thresholds, ceilings));
    }
}
