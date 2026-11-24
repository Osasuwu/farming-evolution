package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A cross of two species that may give a third. When a pair qualifies, each parent's gamete
 * independently turns, with the given chance, into the wild type of the result species.
 */
public record Mutation(String firstParent, String secondParent, String result, double chance, Condition condition) {

    /** Whether a pair with these active species, in either order, qualifies in this environment. */
    public boolean applies(String species, String otherSpecies, Environment environment) {
        boolean parents = (species.equals(firstParent) && otherSpecies.equals(secondParent))
                || (species.equals(secondParent) && otherSpecies.equals(firstParent));
        return parents && condition.holds(environment);
    }

    /**
     * Where the mutation can happen.
     *
     * @param soils   the soils it needs one of; empty for any soil or none
     * @param climate the range each named climate reading must lie in, bounds included
     */
    public record Condition(Set<String> soils, Map<String, Range> climate) {
        public Condition {
            soils = Collections.unmodifiableSet(new LinkedHashSet<>(soils));
            climate = Collections.unmodifiableMap(new LinkedHashMap<>(climate));
        }

        public boolean holds(Environment environment) {
            if (!soils.isEmpty() && (environment.soil() == null || !soils.contains(environment.soil()))) {
                return false;
            }
            for (Map.Entry<String, Range> entry : climate.entrySet()) {
                // A reading the environment does not have cannot be in range.
                Double reading = environment.climate().get(entry.getKey());
                if (reading == null || reading < entry.getValue().min() || reading > entry.getValue().max()) {
                    return false;
                }
            }
            return true;
        }
    }

    public record Range(double min, double max) {
    }
}
