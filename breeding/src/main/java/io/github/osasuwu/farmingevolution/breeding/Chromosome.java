package io.github.osasuwu.farmingevolution.breeding;

/** A pair of alleles: the active one is expressed, the inactive one is only passed on. */
public record Chromosome(Allele active, Allele inactive) {
    public static Chromosome pure(Allele allele) {
        return new Chromosome(allele, allele);
    }
}
