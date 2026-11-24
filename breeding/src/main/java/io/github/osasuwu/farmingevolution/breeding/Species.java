package io.github.osasuwu.farmingevolution.breeding;

import java.util.Map;

/**
 * A plant or an animal with its own set of chromosomes.
 *
 * @param allele the allele that stands for this species on the species chromosome
 * @param wild   the wild-type allele of every other chromosome, by chromosome type id
 */
public record Species(String id, boolean twoSexed, Allele.Named allele, Map<String, Allele> wild) {
}
