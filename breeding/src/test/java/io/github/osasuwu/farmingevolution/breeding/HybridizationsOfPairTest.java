package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Which hybridizations of pack data a pair is a candidate for, whatever the place (issue #90):
 * the analyzer names each of them and says whether its condition holds where the pair crosses.
 */
class HybridizationsOfPairTest {
    private static final String PACK = """
            {
              "chromosomes": [
                {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2}
              ],
              "species": [
                {"id": "wild_flax", "wild": {"fiber_strength": 2}},
                {"id": "wild_hemp", "wild": {"fiber_strength": 3}},
                {"id": "fiber_flax", "dominant": true, "wild": {"fiber_strength": 5}},
                {"id": "oil_flax", "dominant": true, "wild": {"fiber_strength": 3}}
              ],
              "hybridizations": [
                {"parents": ["wild_flax", "wild_hemp"], "result": "fiber_flax", "chance": 0.25,
                 "conditions": {"climate": {"temperature": {"min": 10, "max": 25}}}},
                {"parents": ["fiber_flax", "fiber_flax"], "result": "oil_flax", "chance": 0.1,
                 "conditions": {"climate": {"temperature": {"min": 20}, "humidity": {"max": 35}}}}
              ]
            }
            """;

    private final Breeding breeding = new Breeding(PackData.parse(new StringReader(PACK)));
    private final PackData data = PackData.parse(new StringReader(PACK));

    @Test
    void pairGetsTheRulesNamingItsActiveSpeciesInEitherOrder() {
        Hybridization flaxHemp = data.hybridizations().get(0);
        Hybridization oil = data.hybridizations().get(1);
        Individual flax = breeding.wild("wild_flax", Sex.NONE);
        Individual hemp = breeding.wild("wild_hemp", Sex.NONE);
        Individual fiber = breeding.wild("fiber_flax", Sex.NONE);

        assertEquals(List.of(flaxHemp), breeding.hybridizations(flax, hemp));
        assertEquals(List.of(flaxHemp), breeding.hybridizations(hemp, flax));
        assertEquals(List.of(oil), breeding.hybridizations(fiber, fiber));
        assertEquals(List.of(), breeding.hybridizations(flax, flax));
        assertEquals(List.of(), breeding.hybridizations(fiber, hemp));
    }

    /**
     * The species that counts is the expressed one, as in the cross: a plant that expresses
     * oil_flax and carries fiber_flax is no fiber_flax parent, one that expresses fiber_flax
     * and carries oil_flax is.
     */
    @Test
    void onlyTheExpressedSpeciesCounts() {
        Hybridization oil = data.hybridizations().get(1);
        Individual fiber = breeding.wild("fiber_flax", Sex.NONE);
        Chromosome oilOverFiber = new Chromosome(new Allele.Named("oil_flax", true), new Allele.Named("fiber_flax", true));
        Chromosome fiberOverOil = new Chromosome(new Allele.Named("fiber_flax", true), new Allele.Named("oil_flax", true));

        assertEquals(List.of(), breeding.hybridizations(fiber.with(Breeding.SPECIES, oilOverFiber), fiber));
        assertEquals(List.of(oil), breeding.hybridizations(fiber.with(Breeding.SPECIES, fiberOverOil), fiber));
    }
}
