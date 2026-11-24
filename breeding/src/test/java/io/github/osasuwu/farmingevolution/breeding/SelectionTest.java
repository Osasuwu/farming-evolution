package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The conditions a seed store selects seeds by (issue #30): the engine decides them with the
 * same code that picks the variety of a harvest, so the store holds no thresholds of its own.
 */
class SelectionTest {
    /**
     * Flax with the nested varieties of CONCEPT.md (strong, long, select), the requirement
     * "strong fibre", and a second species, linen, that has no fiber_length chromosome.
     */
    private static final PackData DATA = PackData.parse(new StringReader("""
            {
              "chromosomes": [
                {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                {"id": "fiber_length", "kind": "trait", "min": 1, "max": 4, "shift_chance": 0.2}
              ],
              "species": [
                {"id": "flax", "wild": {"fiber_strength": 5, "fiber_length": 2}},
                {"id": "linen", "wild": {"fiber_strength": 5}}
              ],
              "varieties": [
                {"id": "strong", "species": "flax", "thresholds": {"fiber_strength": 7}},
                {"id": "long", "species": "flax", "thresholds": {"fiber_length": 3}},
                {"id": "select", "species": "flax", "thresholds": {"fiber_strength": 7, "fiber_length": 3}}
              ],
              "requirements": [
                {"id": "strong_fiber", "species": "flax", "thresholds": {"fiber_strength": 7}}
              ]
            }
            """));
    private static final Breeding BREEDING = new Breeding(DATA);

    private static Individual flax(int strength, int length) {
        return BREEDING.wild("flax", Sex.NONE)
                .with("fiber_strength", Chromosome.pure(new Allele.Numeric(strength)))
                .with("fiber_length", Chromosome.pure(new Allele.Numeric(length)));
    }

    private static Individual linen(int strength) {
        return BREEDING.wild("linen", Sex.NONE).with("fiber_strength", Chromosome.pure(new Allele.Numeric(strength)));
    }

    private static Selection selection(String id) {
        return BREEDING.selection(id).orElseThrow(() -> new AssertionError("no selection " + id));
    }

    @Test
    void listHoldsEveryConditionInAFixedOrder() {
        assertEquals(List.of(
                        "hold",
                        "variety/strong", "variety/long", "variety/select",
                        "requirement/strong_fiber",
                        "trait/fiber_strength/2", "trait/fiber_strength/3", "trait/fiber_strength/4",
                        "trait/fiber_strength/5", "trait/fiber_strength/6", "trait/fiber_strength/7",
                        "trait/fiber_strength/8", "trait/fiber_strength/9", "trait/fiber_strength/10",
                        "trait/fiber_length/2", "trait/fiber_length/3", "trait/fiber_length/4",
                        "all"),
                BREEDING.selections().stream().map(Selection::id).toList());
    }

    @Test
    void holdPassesNothingAndAllPassesEverything() {
        for (Individual seed : List.of(flax(9, 4), flax(1, 1), linen(10))) {
            assertFalse(selection("hold").passes(seed), "hold passed " + seed);
            assertTrue(selection("all").passes(seed), "all refused " + seed);
        }
    }

    /** A variety condition passes the seeds whose plant gives that variety's product: the narrowest it fits. */
    @Test
    void varietyPassesSeedsThatGiveItsProduct() {
        Selection strong = selection("variety/strong");
        assertTrue(strong.passes(flax(7, 2)));
        assertTrue(strong.passes(flax(10, 1)));
        assertFalse(strong.passes(flax(6, 2)), "below the threshold");
        assertFalse(strong.passes(flax(8, 3)), "gives the narrower select, not strong");
        assertFalse(strong.passes(linen(9)), "another species");
        assertTrue(selection("variety/select").passes(flax(8, 3)));
        assertFalse(selection("variety/select").passes(flax(8, 2)));
    }

    /** A requirement condition passes the seeds whose variety passes it: strong and select, not long. */
    @Test
    void requirementPassesSeedsOfEveryPassingVariety() {
        Selection strongFiber = selection("requirement/strong_fiber");
        assertTrue(strongFiber.passes(flax(7, 1)), "strong");
        assertTrue(strongFiber.passes(flax(9, 4)), "select");
        assertFalse(strongFiber.passes(flax(5, 4)), "long");
        assertFalse(strongFiber.passes(flax(5, 1)), "no variety");
        assertFalse(strongFiber.passes(linen(10)), "another species");
    }

    @Test
    void traitPassesExpressedValuesFromTheThresholdUp() {
        Selection atLeast8 = selection("trait/fiber_strength/8");
        assertFalse(atLeast8.passes(flax(7, 1)));
        assertTrue(atLeast8.passes(flax(8, 1)));
        assertTrue(atLeast8.passes(flax(10, 1)));
        assertTrue(atLeast8.passes(linen(9)), "any species that has the trait");
        Individual hidden = flax(5, 1).with("fiber_strength",
                new Chromosome(new Allele.Numeric(5), new Allele.Numeric(10)));
        assertFalse(atLeast8.passes(hidden), "the inactive allele is not expressed");
    }

    @Test
    void traitRefusesSpeciesWithoutTheTrait() {
        assertFalse(selection("trait/fiber_length/2").passes(linen(10)));
        assertTrue(selection("trait/fiber_length/2").passes(flax(1, 2)));
    }

    @Test
    void unknownIdsHaveNoSelection() {
        for (String id : List.of("variety/missing", "requirement/missing", "trait/fiber_strength/1",
                "trait/fiber_strength/11", "trait/fiber_strength/x", "trait/missing/5", "trait/fiber_strength",
                "", "everything")) {
            assertEquals(Optional.empty(), BREEDING.selection(id).map(Selection::id), id);
        }
    }
}
