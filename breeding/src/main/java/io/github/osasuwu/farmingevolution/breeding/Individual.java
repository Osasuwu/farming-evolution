package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One plant or animal.
 *
 * @param genome chromosomes by chromosome type id, the species chromosome included
 */
public record Individual(Sex sex, Map<String, Chromosome> genome) {
    public Individual {
        genome = Collections.unmodifiableMap(new LinkedHashMap<>(genome));
    }

    /** The id of the active species. */
    public String species() {
        return ((Allele.Named) genome.get(Breeding.SPECIES).active()).id();
    }

    /** The expressed value of a numeric trait. */
    public int trait(String chromosomeType) {
        return ((Allele.Numeric) genome.get(chromosomeType).active()).value();
    }

    public Individual with(String chromosomeType, Chromosome chromosome) {
        Map<String, Chromosome> changed = new LinkedHashMap<>(genome);
        changed.put(chromosomeType, chromosome);
        return new Individual(sex, changed);
    }
}
