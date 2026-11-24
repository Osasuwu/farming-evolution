package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a cross of one pair may give, before it is rolled.
 *
 * @param chromosomes for every chromosome type, the odds of each active and inactive pair in the
 *                    offspring; each chromosome on its own, not the odds of whole genomes
 * @param sex         the odds of the offspring's sex
 */
public record CrossOdds(Map<String, Distribution<Chromosome>> chromosomes, Distribution<Sex> sex) {
    public CrossOdds {
        chromosomes = Collections.unmodifiableMap(new LinkedHashMap<>(chromosomes));
    }
}
