package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BreedingTest {
    private static final String PLANTS = """
            {
              "chromosomes": [
                {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                {"id": "habit", "kind": "alleles", "alleles": [{"id": "tall", "dominant": true}, {"id": "dwarf"}]}
              ],
              "species": [
                {"id": "wild_flax", "wild": {"fiber_strength": 2, "habit": "tall"}},
                {"id": "wild_hemp", "wild": {"fiber_strength": 3, "habit": "dwarf"}},
                {"id": "fiber_flax", "wild": {"fiber_strength": 5, "habit": "dwarf"}}
              ],
              "mutations": [
                {"parents": ["wild_flax", "wild_hemp"], "result": "fiber_flax", "chance": 0.25,
                 "conditions": {"soil": ["loam", "chernozem"], "climate": {"temperature": {"min": 10, "max": 25}}}}
              ]
            }
            """;
    private static final Environment LOAM_AT_18 = new Environment(Map.of("temperature", 18.0), "loam");

    private static Breeding breeding(String packData) {
        return new Breeding(PackData.parse(new StringReader(packData)));
    }

    private static Chromosome numbers(int active, int inactive) {
        return new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive));
    }

    /** Fifty offspring of one heterozygous pair, rolled from one seeded generator. */
    private static List<Individual> litter(long seed) {
        Breeding breeding = breeding(PLANTS);
        Individual first = breeding.wild("wild_flax", Sex.NONE).with("fiber_strength", numbers(2, 7));
        Individual second = breeding.wild("wild_hemp", Sex.NONE).with("fiber_strength", numbers(3, 5));
        Random random = new Random(seed);
        List<Individual> offspring = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            offspring.add(breeding.cross(first, second, Environment.NONE, random));
        }
        return offspring;
    }

    @Test
    void sameSeedGivesTheSameOffspringAndAnotherSeedDoesNot() {
        assertEquals(litter(42), litter(42));
        assertNotEquals(litter(42), litter(43));
    }

    @Test
    void selectedTraitGrowsPastBothFoundersAndStopsAtTheTraitMaximum() {
        Breeding breeding = breeding(PLANTS);
        // Founders express 2 and 3; the trait runs from 1 to 10.
        List<Individual> parents = List.of(breeding.wild("wild_flax", Sex.NONE), breeding.wild("wild_hemp", Sex.NONE));
        Random random = new Random(7);
        int highestAllele = 0;
        // Sixty generations of eight offspring; the two strongest become the next parents.
        for (int generation = 0; generation < 60; generation++) {
            List<Individual> offspring = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                Individual child = breeding.cross(parents.get(0), parents.get(1), Environment.NONE, random);
                Chromosome fiber = child.genome().get("fiber_strength");
                highestAllele = Math.max(highestAllele, ((Allele.Numeric) fiber.active()).value());
                highestAllele = Math.max(highestAllele, ((Allele.Numeric) fiber.inactive()).value());
                offspring.add(child);
            }
            offspring.sort(Comparator.comparingInt((Individual child) -> child.trait("fiber_strength")).reversed());
            parents = offspring.subList(0, 2);
        }
        assertEquals(10, parents.get(0).trait("fiber_strength"));
        assertEquals(10, highestAllele);
    }

    @Test
    void twoSpeciesCrossIntoTheThirdSpeciesFromPackData() {
        Breeding breeding = breeding(PLANTS.replace("\"chance\": 0.25", "\"chance\": 1.0"));
        Individual flax = breeding.wild("wild_flax", Sex.NONE);
        Individual hemp = breeding.wild("wild_hemp", Sex.NONE);
        // The wild type of fiber_flax as PLANTS writes it; a mutated gamete carries it unshifted.
        Individual fiberFlax = new Individual(Sex.NONE, Map.of(
                "species", Chromosome.pure(new Allele.Named("fiber_flax", false)),
                "fiber_strength", numbers(5, 5),
                "habit", Chromosome.pure(new Allele.Named("dwarf", false))));

        assertEquals(fiberFlax, breeding.cross(flax, hemp, LOAM_AT_18, new Random(1)));
        assertEquals(fiberFlax, breeding.cross(hemp, flax, LOAM_AT_18, new Random(1)));
        assertEquals("wild_flax", breeding.cross(flax, flax, LOAM_AT_18, new Random(1)).species());
    }

    /** The share of gametes turned into fiber_flax over 10 000 wild_flax x wild_hemp crosses. */
    private static double mutatedShare(Environment environment) {
        Breeding breeding = breeding(PLANTS);
        Individual flax = breeding.wild("wild_flax", Sex.NONE);
        Individual hemp = breeding.wild("wild_hemp", Sex.NONE);
        Allele fiberFlax = new Allele.Named("fiber_flax", false);
        Random random = new Random(11);
        int mutated = 0;
        for (int i = 0; i < 10_000; i++) {
            Chromosome species = breeding.cross(flax, hemp, environment, random).genome().get("species");
            mutated += (species.active().equals(fiberFlax) ? 1 : 0) + (species.inactive().equals(fiberFlax) ? 1 : 0);
        }
        return mutated / 20_000.0;
    }

    @Test
    void conditionalMutationNeverHappensOffConditionAndHappensAtItsChanceOnCondition() {
        // PLANTS: chance 0.25 per gamete, on loam or chernozem, at temperature 10..25.
        // 20 000 gametes put one standard deviation at sqrt(0.25 * 0.75 / 20 000) = 0.0031, so 0.015
        // is about five of them; a rule that mutated once per cross, not per gamete, would read 0.125.
        assertEquals(0.25, mutatedShare(LOAM_AT_18), 0.015);
        assertEquals(0.25, mutatedShare(new Environment(Map.of("temperature", 10.0), "chernozem")), 0.015);
        assertEquals(0.25, mutatedShare(new Environment(Map.of("temperature", 25.0), "loam")), 0.015);

        assertEquals(0, mutatedShare(new Environment(Map.of("temperature", 18.0), "sand")));
        assertEquals(0, mutatedShare(new Environment(Map.of("temperature", 18.0), null)));
        assertEquals(0, mutatedShare(new Environment(Map.of("temperature", 9.5), "loam")));
        assertEquals(0, mutatedShare(new Environment(Map.of("temperature", 25.5), "loam")));
        assertEquals(0, mutatedShare(new Environment(Map.of("rainfall", 18.0), "loam")));
    }

    private static final String ANIMALS = """
            {
              "chromosomes": [
                {"id": "milk_yield", "kind": "trait", "min": 1, "max": 20, "shift_chance": 0.3}
              ],
              "species": [
                {"id": "aurochs", "two_sexed": true, "wild": {"milk_yield": 4}}
              ]
            }
            """;

    @Test
    void twoSexedAnimalSpeciesBreedsThroughGenerationsOfFemalesAndMales() {
        Breeding breeding = breeding(ANIMALS);
        Individual cow = breeding.wild("aurochs", Sex.FEMALE);
        Individual bull = breeding.wild("aurochs", Sex.MALE);
        Random random = new Random(3);
        Comparator<Individual> byMilk = Comparator.comparingInt(calf -> calf.trait("milk_yield"));
        Map<Sex, Integer> born = new EnumMap<>(Sex.class);
        // Thirty generations of twelve calves; the best heifer and the best bull calf breed next.
        for (int generation = 0; generation < 30; generation++) {
            List<Individual> calves = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                // Either parent may come first.
                Individual calf = i % 2 == 0
                        ? breeding.cross(cow, bull, Environment.NONE, random)
                        : breeding.cross(bull, cow, Environment.NONE, random);
                born.merge(calf.sex(), 1, Integer::sum);
                calves.add(calf);
            }
            cow = calves.stream().filter(calf -> calf.sex() == Sex.FEMALE).max(byMilk).orElse(cow);
            bull = calves.stream().filter(calf -> calf.sex() == Sex.MALE).max(byMilk).orElse(bull);
        }
        // 360 calves, each a female or a male with equal odds: one standard deviation of the
        // female count is sqrt(360) / 2 = 9.5, so 40 is about four of them.
        assertEquals(Set.of(Sex.FEMALE, Sex.MALE), born.keySet());
        assertEquals(360, born.get(Sex.FEMALE) + born.get(Sex.MALE));
        assertEquals(180, born.get(Sex.FEMALE), 40);
        // Both founders express 4.
        assertTrue(cow.trait("milk_yield") > 4, "cow: " + cow.trait("milk_yield"));
        assertTrue(bull.trait("milk_yield") > 4, "bull: " + bull.trait("milk_yield"));
    }

    @Test
    void crossRejectsAPairThatCannotBreed() {
        Breeding animals = breeding(ANIMALS);
        Individual cow = animals.wild("aurochs", Sex.FEMALE);
        Random random = new Random(1);
        assertEquals("a cross of two-sexed aurochs needs one female and one male, got FEMALE and FEMALE",
                assertThrows(IllegalArgumentException.class,
                        () -> animals.cross(cow, cow, Environment.NONE, random)).getMessage());

        Breeding plants = breeding(PLANTS);
        Individual flax = plants.wild("wild_flax", Sex.NONE);
        assertEquals("wild_flax has no sexes, got NONE and MALE",
                assertThrows(IllegalArgumentException.class,
                        () -> plants.cross(flax, plants.wild("wild_flax", Sex.MALE), Environment.NONE, random))
                        .getMessage());
        Map<String, Chromosome> withoutHabit = new LinkedHashMap<>(flax.genome());
        withoutHabit.remove("habit");
        assertEquals("parents have different chromosome sets: [species, fiber_strength, habit] and [species, fiber_strength]",
                assertThrows(IllegalArgumentException.class,
                        () -> plants.cross(flax, new Individual(Sex.NONE, withoutHabit), Environment.NONE, random))
                        .getMessage());
    }

    @Test
    void reportedOddsMatchFrequenciesOverASeriesOfCrosses() {
        Breeding breeding = breeding(PLANTS);
        // A pair on which every rule plays: active or inactive allele, the shift and its stop
        // at the maximum, dominance, a species mutation.
        Individual first = breeding.wild("wild_flax", Sex.NONE).with("fiber_strength", numbers(10, 2));
        Individual second = breeding.wild("wild_hemp", Sex.NONE)
                .with("habit", new Chromosome(new Allele.Named("tall", true), new Allele.Named("dwarf", false)));
        CrossOdds odds = breeding.odds(first, second, LOAM_AT_18);

        // Worked by hand from PLANTS: each gamete is fiber_flax with 0.25, else its parent's species;
        // no species is dominant, so either allele of a mixed pair is active with 0.5.
        Allele flax = new Allele.Named("wild_flax", false);
        Allele hemp = new Allele.Named("wild_hemp", false);
        Allele fiberFlax = new Allele.Named("fiber_flax", false);
        assertEquals(Map.of(
                new Chromosome(fiberFlax, fiberFlax), 0.0625,
                new Chromosome(fiberFlax, flax), 0.09375,
                new Chromosome(flax, fiberFlax), 0.09375,
                new Chromosome(fiberFlax, hemp), 0.09375,
                new Chromosome(hemp, fiberFlax), 0.09375,
                new Chromosome(flax, hemp), 0.28125,
                new Chromosome(hemp, flax), 0.28125), odds.chromosomes().get("species").odds());
        assertEquals(Map.of(Sex.NONE, 1.0), odds.sex().odds());
        // Habit: the first gamete is dwarf only by mutation (0.25); the second by mutation or, half
        // the time, from the parent's inactive allele (0.25 + 0.75 / 2 = 0.625). Tall is dominant,
        // so a mixed pair (0.75 * 0.625 + 0.25 * 0.375) always has tall active.
        Allele tall = new Allele.Named("tall", true);
        Allele dwarf = new Allele.Named("dwarf", false);
        assertEquals(Map.of(
                new Chromosome(tall, tall), 0.28125,
                new Chromosome(tall, dwarf), 0.5625,
                new Chromosome(dwarf, dwarf), 0.15625), odds.chromosomes().get("habit").odds());

        int rolls = 40_000;
        Random random = new Random(5);
        Map<String, Map<Chromosome, Integer>> seen = new LinkedHashMap<>();
        for (int i = 0; i < rolls; i++) {
            breeding.cross(first, second, LOAM_AT_18, random).genome().forEach((type, chromosome) ->
                    seen.computeIfAbsent(type, any -> new LinkedHashMap<>()).merge(chromosome, 1, Integer::sum));
        }
        assertEquals(Set.of("species", "fiber_strength", "habit"), odds.chromosomes().keySet());
        List<String> mismatches = new ArrayList<>();
        for (String type : odds.chromosomes().keySet()) {
            Distribution<Chromosome> reported = odds.chromosomes().get(type);
            Set<Chromosome> outcomes = new LinkedHashSet<>(reported.odds().keySet());
            outcomes.addAll(seen.get(type).keySet());
            for (Chromosome outcome : outcomes) {
                double chance = reported.chance(outcome);
                double frequency = seen.get(type).getOrDefault(outcome, 0) / (double) rolls;
                // Five standard deviations of a frequency over 40 000 rolls: at most 0.0125 for an
                // even outcome, 0.0008 for a one-in-a-thousand one, zero for one never reported.
                double tolerance = 5 * Math.sqrt(chance * (1 - chance) / rolls);
                if (Math.abs(chance - frequency) > tolerance) {
                    mismatches.add(type + " " + outcome + ": reported " + chance + ", rolled " + frequency);
                }
            }
        }
        assertEquals(List.of(), mismatches);
    }

    @Test
    void reportedOddsOfATwoSexedPairGiveEvenSexesAndTheShiftAtItsChance() {
        Breeding breeding = breeding(ANIMALS);
        CrossOdds odds = breeding.odds(
                breeding.wild("aurochs", Sex.MALE), breeding.wild("aurochs", Sex.FEMALE), Environment.NONE);
        assertEquals(Map.of(Sex.FEMALE, 0.5, Sex.MALE, 0.5), odds.sex().odds());

        // Both parents are pure 4 and ANIMALS sets shift_chance 0.3: a gamete carries 4 with 0.7,
        // 5 with 0.15, 3 with 0.15. Two unequal values pair in either order with equal odds.
        Map<Chromosome, Double> expected = Map.of(
                numbers(4, 4), 0.49,
                numbers(4, 5), 0.105, numbers(5, 4), 0.105,
                numbers(4, 3), 0.105, numbers(3, 4), 0.105,
                numbers(5, 5), 0.0225, numbers(3, 3), 0.0225,
                numbers(5, 3), 0.0225, numbers(3, 5), 0.0225);
        Map<Chromosome, Double> reported = odds.chromosomes().get("milk_yield").odds();
        assertEquals(expected.keySet(), reported.keySet());
        // Exact but for floating point rounding.
        expected.forEach((pair, chance) -> assertEquals(chance, reported.get(pair), 1e-12, pair.toString()));
    }
}
