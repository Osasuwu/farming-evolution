package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A genome saved under other pack data (CONCEPT.md, "Устройство мода"): what pack data no
 * longer describes is replaced with the wild type, and every replacement is reported.
 */
class RestoreTest {
    private static final Allele.Named TALL = new Allele.Named("tall", true);
    private static final Allele.Named DWARF = new Allele.Named("dwarf", false);
    private static final Allele.Named FLAX = new Allele.Named("flax", true);
    private static final Allele.Named HEMP = new Allele.Named("hemp", false);

    /** Flax is the first species; its wild type is strength 3, tall. Hemp: strength 4, dwarf. */
    private static final Breeding BREEDING = new Breeding(PackData.parse(new StringReader("""
            {
              "chromosomes": [
                {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                {"id": "habit", "kind": "alleles", "alleles": [{"id": "tall", "dominant": true}, {"id": "dwarf"}]}
              ],
              "species": [
                {"id": "flax", "dominant": true, "wild": {"fiber_strength": 3, "habit": "tall"}},
                {"id": "hemp", "wild": {"fiber_strength": 4, "habit": "dwarf"}},
                {"id": "moss", "wild": {"habit": "dwarf"}}
              ]
            }
            """)));

    private static Individual genome(Chromosome species, Chromosome strength, Chromosome habit) {
        return new Individual(Sex.NONE, Map.of(Breeding.SPECIES, species, "fiber_strength", strength, "habit", habit));
    }

    private static Chromosome strength(int active, int inactive) {
        return new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive));
    }

    @Test
    void genomePackDataDescribesIsReturnedAsSavedAndNothingIsReported() {
        Individual saved = genome(new Chromosome(HEMP, FLAX), strength(9, 1), new Chromosome(DWARF, TALL));
        List<String> reported = new ArrayList<>();
        assertEquals(saved, BREEDING.restore(saved, reported::add));
        assertEquals(List.of(), reported);
    }

    @Test
    void alleleRemovedFromItsChromosomeIsReplacedWithTheWildOneOfTheActiveSpecies() {
        Individual saved = genome(new Chromosome(HEMP, FLAX), strength(9, 1),
                new Chromosome(new Allele.Named("creeping", false), TALL));
        List<String> reported = new ArrayList<>();
        // Hemp is the active species, and wild hemp is dwarf; the known allele stays where it was.
        assertEquals(genome(new Chromosome(HEMP, FLAX), strength(9, 1), new Chromosome(DWARF, TALL)),
                BREEDING.restore(saved, reported::add));
        assertEquals(List.of("habit: has no allele creeping, replaced with dwarf of wild hemp"), reported);
    }

    @Test
    void traitValueOutsideTheRangeIsReplacedWithTheWildValue() {
        Individual saved = genome(Chromosome.pure(FLAX), strength(5, 12), Chromosome.pure(TALL));
        List<String> reported = new ArrayList<>();
        assertEquals(genome(Chromosome.pure(FLAX), strength(5, 3), Chromosome.pure(TALL)),
                BREEDING.restore(saved, reported::add));
        assertEquals(List.of("fiber_strength: 12 is outside 1..10, replaced with 3 of wild flax"), reported);
    }

    @Test
    void unknownSpeciesAlleleIsReplacedWithTheKnownOneOfThePair() {
        Individual saved = genome(new Chromosome(new Allele.Named("nettle", true), HEMP),
                strength(9, 1), Chromosome.pure(DWARF));
        List<String> reported = new ArrayList<>();
        assertEquals(genome(Chromosome.pure(HEMP), strength(9, 1), Chromosome.pure(DWARF)),
                BREEDING.restore(saved, reported::add));
        assertEquals(List.of("species: unknown species nettle, replaced with hemp"), reported);
    }

    @Test
    void individualOfTwoUnknownSpeciesBecomesTheWildTypeOfTheFirstSpeciesOfPackData() {
        Individual saved = genome(Chromosome.pure(new Allele.Named("nettle", true)),
                strength(9, 1), Chromosome.pure(DWARF));
        List<String> reported = new ArrayList<>();
        assertEquals(BREEDING.wild("flax", Sex.NONE), BREEDING.restore(saved, reported::add));
        assertEquals(List.of("species: unknown species nettle and nettle, replaced with wild flax"), reported);
    }

    @Test
    void chromosomeSetIsBroughtToThatOfTheActiveSpecies() {
        // Saved as moss when moss still had a strength; hemp has since lost nothing, moss has.
        Individual moss = genome(Chromosome.pure(new Allele.Named("moss", false)), strength(2, 2), Chromosome.pure(DWARF));
        Individual hemp = new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(HEMP), "fiber_strength", strength(9, 1)));
        List<String> reported = new ArrayList<>();
        assertEquals(new Individual(Sex.NONE, Map.of(
                        Breeding.SPECIES, Chromosome.pure(new Allele.Named("moss", false)), "habit", Chromosome.pure(DWARF))),
                BREEDING.restore(moss, reported::add));
        assertEquals(genome(Chromosome.pure(HEMP), strength(9, 1), Chromosome.pure(DWARF)),
                BREEDING.restore(hemp, reported::add));
        assertEquals(List.of(
                "fiber_strength: moss has no such chromosome, removed",
                "habit: missing, replaced with dwarf of wild hemp"), reported);
    }

    @Test
    void knownAlleleTakesItsDominanceFromPackData() {
        Individual saved = genome(Chromosome.pure(new Allele.Named("flax", false)), strength(5, 5),
                Chromosome.pure(new Allele.Named("tall", false)));
        assertEquals(genome(Chromosome.pure(FLAX), strength(5, 5), Chromosome.pure(TALL)),
                BREEDING.restore(saved, message -> { }));
    }
}
