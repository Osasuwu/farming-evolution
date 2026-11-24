package io.github.osasuwu.farmingevolution.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.checks.BreedingSimulation.Method;
import io.github.osasuwu.farmingevolution.checks.BreedingSimulation.Outcome;
import io.github.osasuwu.farmingevolution.checks.BreedingSimulation.Scenario;
import io.github.osasuwu.farmingevolution.checks.BreedingSimulation.Sight;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class BreedingSimulationTest {
    /** Flax is wild at 3; "plain" asks for what it already has, "strong" for four more. */
    private static final String SCENARIO = """
            {
              "pack": {
                "chromosomes": [{"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": SHIFT}],
                "species": [
                  {"id": "flax", "wild": {"fiber_strength": 3}},
                  {"id": "hemp", "wild": {"fiber_strength": 9}}
                ],
                "varieties": [
                  {"id": "plain", "species": "flax", "thresholds": {"fiber_strength": 3}},
                  {"id": "strong", "species": "flax", "thresholds": {"fiber_strength": 7}}
                ]
              },
              "seed": 23,
              "runs": 50,
              "generation_limit": 40,
              "methods": [
                {"id": "selection", "sees": "expressed", "offspring": 8, "minutes_per_generation": 5},
                {"id": "crossing", "sees": "alleles", "offspring": 6, "minutes_per_generation": 7}
              ]
            }
            """;
    private static final Method SELECTION = new Method("selection", Sight.EXPRESSED, 8, 5);
    private static final Method CROSSING = new Method("crossing", Sight.ALLELES, 6, 7);

    /** Every roll lands on the first outcome of its distribution. */
    private static final RandomGenerator FIRST_OUTCOME = () -> 0L;

    private static Scenario scenario(String shiftChance) {
        return BreedingSimulation.parse(new StringReader(SCENARIO.replace("SHIFT", shiftChance)));
    }

    private static Individual flax(Sex sex, int active, int inactive) {
        PackData pack = scenario("0.2").pack();
        return new Breeding(pack).wild("flax", sex).with("fiber_strength",
                new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive)));
    }

    @Test
    void scenarioFileGivesSeedRunsLimitAndMethods() {
        Scenario scenario = scenario("0.2");
        assertEquals(23, scenario.seed());
        assertEquals(50, scenario.runs());
        assertEquals(40, scenario.generationLimit());
        assertEquals(List.of(SELECTION, CROSSING), scenario.methods());
        assertEquals(List.of("plain", "strong"), List.copyOf(scenario.pack().varieties().keySet()));
    }

    @Test
    void generationsAreCountedFromWildFoundersToTheVarietyAndStopAtTheLimit() {
        // Shift chance 1 and the first outcome of every roll: each gamete carries the parent's
        // active value plus one. Wild flax is 3, so the parents express 7 after 4 generations.
        Scenario scenario = scenario("1.0");
        var strong = scenario.pack().varieties().get("strong");
        assertEquals(OptionalInt.of(4), BreedingSimulation.generations(scenario.pack(), strong, SELECTION, 4, FIRST_OUTCOME));
        assertEquals(OptionalInt.empty(), BreedingSimulation.generations(scenario.pack(), strong, SELECTION, 3, FIRST_OUTCOME));
        // Founders that already meet the thresholds need no breeding at all.
        var plain = scenario.pack().varieties().get("plain");
        assertEquals(OptionalInt.of(0), BreedingSimulation.generations(scenario.pack(), plain, SELECTION, 0, FIRST_OUTCOME));
    }

    @Test
    void lineHasReachedTheVarietyOnlyWhenBothParentsAreOfItsSpeciesAndExpressItsThresholds() {
        PackData pack = scenario("0.2").pack();
        var strong = pack.varieties().get("strong");
        // The inactive allele is not expressed: 7 over 2 gives the product, 6 over 9 does not.
        assertEquals(true, BreedingSimulation.reached(List.of(flax(Sex.NONE, 7, 2), flax(Sex.NONE, 8, 8)), strong));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(Sex.NONE, 7, 7), flax(Sex.NONE, 6, 9)), strong));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(Sex.NONE, 6, 9), flax(Sex.NONE, 7, 7)), strong));
        // Wild hemp expresses 9, but "strong" is a variety of flax.
        Individual hemp = new Breeding(pack).wild("hemp", Sex.NONE);
        assertEquals(false, BreedingSimulation.reached(List.of(hemp, hemp), strong));
    }

    @Test
    void everyVarietyIsBredByEveryMethodForTheGivenNumberOfRuns() {
        // Shift chance 0: nothing ever changes, so "plain" is met at once and "strong" never.
        List<Outcome> outcomes = BreedingSimulation.run(scenario("0"));
        List<OptionalInt> atOnce = java.util.Collections.nCopies(50, OptionalInt.of(0));
        List<OptionalInt> never = java.util.Collections.nCopies(50, OptionalInt.empty());
        assertEquals(List.of(
                new Outcome("plain", SELECTION, 40, atOnce),
                new Outcome("plain", CROSSING, 40, atOnce),
                new Outcome("strong", SELECTION, 40, never),
                new Outcome("strong", CROSSING, 40, never)), outcomes);
    }

    @Test
    void reportLineGivesMedianAndWorstGenerationsWithGameTime() {
        Outcome outcome = new Outcome("strong", CROSSING, 40,
                List.of(OptionalInt.of(9), OptionalInt.of(4), OptionalInt.of(5)));
        // Sorted 4, 5, 9; crossing takes 7 minutes a generation.
        assertEquals("variety strong, method crossing: median 5 generations (35 min), worst 9 generations (63 min);"
                + " 3 of 3 lines reached the thresholds within 40 generations", outcome.line());
        assertEquals(Optional.empty(), outcome.failure());
    }

    @Test
    void lineThatMissesTheThresholdsWithinTheLimitIsAFailure() {
        Outcome outcome = new Outcome("strong", SELECTION, 40,
                List.of(OptionalInt.of(12), OptionalInt.empty(), OptionalInt.of(30), OptionalInt.empty()));
        assertEquals(Optional.of("variety strong, method selection:"
                + " 2 of 4 lines did not reach the thresholds within 40 generations"), outcome.failure());
        assertEquals("variety strong, method selection: median 30 generations (150 min), worst 30 generations (150 min);"
                + " 2 of 4 lines reached the thresholds within 40 generations", outcome.line());

        Outcome lost = new Outcome("strong", SELECTION, 40, List.of(OptionalInt.empty()));
        assertEquals("variety strong, method selection:"
                + " 0 of 1 lines reached the thresholds within 40 generations", lost.line());
    }

    @Test
    void sameSeedGivesTheSameRunsAndAnotherSeedDoesNot() {
        Scenario scenario = scenario("0.2");
        Scenario reseeded = new Scenario(scenario.pack(), 24, scenario.runs(), scenario.generationLimit(),
                scenario.methods());
        assertEquals(BreedingSimulation.run(scenario), BreedingSimulation.run(scenario("0.2")));
        assertNotEquals(BreedingSimulation.run(scenario), BreedingSimulation.run(reseeded));
    }

    @Test
    void selectionRanksByTheExpressedValueAndCrossingByBothAlleles() {
        var strong = scenario("0.2").pack().varieties().get("strong");
        Individual showy = flax(Sex.NONE, 7, 2);
        Individual solid = flax(Sex.NONE, 6, 6);
        Individual middling = flax(Sex.NONE, 5, 6);
        Individual weak = flax(Sex.NONE, 3, 3);
        List<Individual> offspring = List.of(weak, middling, solid, showy);
        // Expressed: 7, 6, 5, 3. Both alleles, each counted up to the threshold 7: 12, 11, 9, 6.
        assertEquals(List.of(showy, solid), BreedingSimulation.parents(offspring, strong, Sight.EXPRESSED, false));
        assertEquals(List.of(solid, middling), BreedingSimulation.parents(offspring, strong, Sight.ALLELES, false));
    }

    @Test
    void valueAboveTheThresholdDoesNotOutrankTheThreshold() {
        // With two thresholds a surplus in one trait must not pay for a shortfall in the other;
        // with one, it shows as a tie that keeps birth order.
        var strong = scenario("0.2").pack().varieties().get("strong");
        Individual first = flax(Sex.NONE, 7, 7);
        Individual surplus = flax(Sex.NONE, 10, 10);
        Individual short6 = flax(Sex.NONE, 6, 6);
        assertEquals(List.of(first, surplus),
                BreedingSimulation.parents(List.of(short6, first, surplus), strong, Sight.ALLELES, false));
    }

    @Test
    void twoSexedLineKeepsTheBestFemaleAndTheBestMaleAndIsLostWithoutOneOfThem() {
        var strong = scenario("0.2").pack().varieties().get("strong");
        Individual bestFemale = flax(Sex.FEMALE, 8, 8);
        Individual otherFemale = flax(Sex.FEMALE, 6, 6);
        Individual bestMale = flax(Sex.MALE, 4, 4);
        Individual otherMale = flax(Sex.MALE, 2, 2);
        assertEquals(List.of(bestFemale, bestMale), BreedingSimulation.parents(
                List.of(otherMale, otherFemale, bestMale, bestFemale), strong, Sight.EXPRESSED, true));
        assertEquals(List.of(), BreedingSimulation.parents(
                List.of(otherFemale, bestFemale), strong, Sight.EXPRESSED, true));
    }

    @Test
    void scenarioThatWouldSimulateNothingIsRefused() {
        String noVarieties = SCENARIO.replace("SHIFT", "0.2")
                .replaceAll("(?s)\"varieties\": \\[.*?\\]\\s*\\}", "\"varieties\": []}");
        assertEquals("scenario: pack data has no varieties to breed", assertThrows(IllegalArgumentException.class,
                () -> BreedingSimulation.parse(new StringReader(noVarieties))).getMessage());

        String noMethods = SCENARIO.replace("SHIFT", "0.2").replaceAll("(?s)\"methods\": \\[.*\\]", "\"methods\": []");
        assertEquals("scenario: no methods", assertThrows(IllegalArgumentException.class,
                () -> BreedingSimulation.parse(new StringReader(noMethods))).getMessage());

        String noRuns = SCENARIO.replace("SHIFT", "0.2").replace("\"runs\": 50", "\"runs\": 0");
        assertEquals("scenario: runs must be at least 1, got 0", assertThrows(IllegalArgumentException.class,
                () -> BreedingSimulation.parse(new StringReader(noRuns))).getMessage());

        // A limit below 1 would never be hit: the stage could not fail, and could not stop.
        String noLimit = SCENARIO.replace("SHIFT", "0.2")
                .replace("\"generation_limit\": 40", "\"generation_limit\": 0");
        assertEquals("scenario: generation_limit must be at least 1, got 0", assertThrows(IllegalArgumentException.class,
                () -> BreedingSimulation.parse(new StringReader(noLimit))).getMessage());

        String unknownSight =SCENARIO.replace("SHIFT", "0.2").replace("\"sees\": \"alleles\"", "\"sees\": \"genes\"");
        assertEquals("scenario: method crossing: sees must be \"expressed\" or \"alleles\", got \"genes\"",
                assertThrows(IllegalArgumentException.class,
                        () -> BreedingSimulation.parse(new StringReader(unknownSight))).getMessage());
    }

    /** Flax is wild at 3; "low" asks for 2 or less, "band" for 5 or 6. */
    private static final String BOUNDED = """
            {
              "pack": {
                "chromosomes": [{"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 1.0}],
                "species": [{"id": "flax", "wild": {"fiber_strength": 3}}],
                "varieties": [
                  {"id": "low", "species": "flax", "thresholds": {"fiber_strength": {"max": 2}}},
                  {"id": "band", "species": "flax", "thresholds": {"fiber_strength": {"min": 5, "max": 6}}}
                ]
              },
              "seed": 23,
              "runs": 1,
              "generation_limit": 40,
              "methods": [{"id": "selection", "sees": "expressed", "offspring": 8, "minutes_per_generation": 5}]
            }
            """;

    /** Every roll lands on the last outcome of its distribution. */
    private static final RandomGenerator LAST_OUTCOME = () -> -1L;

    private static PackData bounded() {
        return BreedingSimulation.parse(new StringReader(BOUNDED)).pack();
    }

    @Test
    void lineWithAParentAboveTheCeilingHasNotReachedTheVariety() {
        var band = bounded().varieties().get("band");
        // Both edges of 5..6 are inside; the inactive allele is not expressed.
        assertEquals(true, BreedingSimulation.reached(List.of(flax(Sex.NONE, 5, 9), flax(Sex.NONE, 6, 1)), band));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(Sex.NONE, 6, 6), flax(Sex.NONE, 7, 5)), band));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(Sex.NONE, 7, 5), flax(Sex.NONE, 6, 6)), band));
        var low = bounded().varieties().get("low");
        assertEquals(true, BreedingSimulation.reached(List.of(flax(Sex.NONE, 2, 9), flax(Sex.NONE, 1, 1)), low));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(Sex.NONE, 2, 2), flax(Sex.NONE, 3, 1)), low));
    }

    @Test
    void generationsAreCountedDownToACeilingAndALineThatOnlyClimbsNeverGetsThere() {
        PackData pack = bounded();
        // Shift chance 1. The first outcome of every roll adds one to each passed value, the
        // last takes one away. Wild flax is 3.
        var low = pack.varieties().get("low");
        assertEquals(OptionalInt.of(1), BreedingSimulation.generations(pack, low, SELECTION, 3, LAST_OUTCOME));
        assertEquals(OptionalInt.empty(), BreedingSimulation.generations(pack, low, SELECTION, 3, FIRST_OUTCOME));
        // 3, 4, 5: the band 5..6 is entered from below after 2 generations.
        var band = pack.varieties().get("band");
        assertEquals(OptionalInt.of(2), BreedingSimulation.generations(pack, band, SELECTION, 3, FIRST_OUTCOME));
    }

    @Test
    void selectionLeadsATraitDownToACeilingAsItLeadsItUpToAThreshold() {
        var low = bounded().varieties().get("low");
        Individual five = flax(Sex.NONE, 5, 5);
        Individual three = flax(Sex.NONE, 3, 3);
        Individual hidden = flax(Sex.NONE, 2, 6);
        Individual two = flax(Sex.NONE, 2, 2);
        List<Individual> offspring = List.of(five, three, hidden, two);
        // Expressed 5, 3, 2, 2: the two at the ceiling, in birth order.
        assertEquals(List.of(hidden, two), BreedingSimulation.parents(offspring, low, Sight.EXPRESSED, false));
        // Both alleles, each counted by how far it is above 2: 6, 2, 4, 0.
        assertEquals(List.of(two, three), BreedingSimulation.parents(offspring, low, Sight.ALLELES, false));
    }

    @Test
    void valueAboveTheCeilingRanksBelowAValueInsideTheRegionAndBelowANearerMiss() {
        var band = bounded().varieties().get("band");
        Individual eight = flax(Sex.NONE, 8, 8);
        Individual four = flax(Sex.NONE, 4, 4);
        Individual one = flax(Sex.NONE, 1, 1);
        Individual six = flax(Sex.NONE, 6, 6);
        // Distance to 5..6: 8 is two above, 4 is one below, 1 is four below, 6 is inside.
        assertEquals(List.of(six, four),
                BreedingSimulation.parents(List.of(eight, four, one, six), band, Sight.EXPRESSED, false));
        assertEquals(List.of(eight, one),
                BreedingSimulation.parents(List.of(one, eight), band, Sight.EXPRESSED, false));
    }

    /** "sturdy" is passed by "strong" and by the narrower "finest", not by the wider "plain". */
    private static Scenario required(String shiftChance) {
        return BreedingSimulation.parse(new StringReader(SCENARIO.replace("SHIFT", shiftChance)
                .replace("{\"id\": \"strong\",", "{\"id\": \"finest\", \"species\": \"flax\","
                        + " \"thresholds\": {\"fiber_strength\": 9}},\n{\"id\": \"strong\",")
                .replaceFirst("(?s)(\"varieties\": \\[.*?\\])", "$1, \"requirements\": ["
                        + "{\"id\": \"sturdy\", \"species\": \"flax\", \"thresholds\": {\"fiber_strength\": 7}}]")));
    }

    private static Outcome outcome(String variety, Method method, Integer... generations) {
        return new Outcome(variety, method, 40, java.util.Arrays.stream(generations)
                .map(one -> one == null ? OptionalInt.empty() : OptionalInt.of(one)).toList());
    }

    @Test
    void requirementIsReachedThroughThePassingVarietyWithTheLowestMedian() {
        Outcome strongBySelection = outcome("strong", SELECTION, 30, 10, 12);
        Outcome finestByCrossing = outcome("finest", CROSSING, 9, 8);
        List<Outcome> outcomes = List.of(
                // Reached at once, but "plain" does not pass the requirement.
                outcome("plain", SELECTION, 0, 0, 0),
                outcome("plain", CROSSING, 0, 0),
                outcome("finest", SELECTION, 20, 25, 40),
                finestByCrossing,
                strongBySelection,
                // The lower median, but one line of two did not get there.
                outcome("strong", CROSSING, 5, null));
        List<BreedingSimulation.RequirementOutcome> reached =
                BreedingSimulation.requirements(required("0.2"), outcomes);
        assertEquals(List.of(
                new BreedingSimulation.RequirementOutcome(
                        "sturdy", SELECTION, 40, List.of("finest", "strong"), Optional.of(strongBySelection)),
                new BreedingSimulation.RequirementOutcome(
                        "sturdy", CROSSING, 40, List.of("finest", "strong"), Optional.of(finestByCrossing))), reached);
        // Sorted 10, 12, 30 at 5 minutes a generation; sorted 8, 9 at 7.
        assertEquals("requirement sturdy, method selection: variety strong,"
                + " median 12 generations (60 min), worst 30 generations (150 min)", reached.get(0).line());
        assertEquals("requirement sturdy, method crossing: variety finest,"
                + " median 9 generations (63 min), worst 9 generations (63 min)", reached.get(1).line());
        assertEquals(Optional.empty(), reached.get(0).failure());
    }

    @Test
    void equalMediansAreSettledByTheWorstLineAndThenByPackDataOrder() {
        Outcome finest = outcome("finest", SELECTION, 12, 12, 20);
        Outcome strong = outcome("strong", SELECTION, 12, 12, 19);
        assertEquals(Optional.of(strong), BreedingSimulation.requirements(required("0.2"),
                List.of(finest, outcome("finest", CROSSING, 1), strong, outcome("strong", CROSSING, 1)))
                .get(0).reachedBy());
        Outcome same = outcome("strong", SELECTION, 12, 12, 20);
        assertEquals(Optional.of(finest), BreedingSimulation.requirements(required("0.2"),
                List.of(same, outcome("strong", CROSSING, 1), finest, outcome("finest", CROSSING, 1)))
                .get(0).reachedBy());
    }

    @Test
    void requirementWhoseEveryPassingVarietyLosesALineIsOutOfReachAndIsAFailure() {
        List<BreedingSimulation.RequirementOutcome> reached = BreedingSimulation.requirements(required("0.2"), List.of(
                outcome("plain", SELECTION, 0), outcome("plain", CROSSING, 0),
                outcome("finest", SELECTION, 20, null), outcome("finest", CROSSING, 9),
                outcome("strong", SELECTION, null, 15), outcome("strong", CROSSING, 5)));
        assertEquals(new BreedingSimulation.RequirementOutcome(
                "sturdy", SELECTION, 40, List.of("finest", "strong"), Optional.empty()), reached.get(0));
        assertEquals("requirement sturdy, method selection: out of reach", reached.get(0).line());
        assertEquals(Optional.of("requirement sturdy, method selection: none of the varieties that pass it"
                + " (finest, strong) was reached by every line within 40 generations"), reached.get(0).failure());
        assertEquals(Optional.empty(), reached.get(1).failure());
    }

    @Test
    void requirementIsOutOfReachWhenTheSimulatedLinesNeverGetToAPassingVariety() {
        // Shift chance 0: wild flax stays at 3, below "strong" and "finest".
        Scenario scenario = required("0");
        List<BreedingSimulation.RequirementOutcome> reached =
                BreedingSimulation.requirements(scenario, BreedingSimulation.run(scenario));
        assertEquals(List.of(Optional.empty(), Optional.empty()),
                reached.stream().map(BreedingSimulation.RequirementOutcome::reachedBy).toList());
        // Shift chance 1 and 8 or 6 offspring: with seed 23 every line climbs to 7 within the limit.
        Scenario climbing = required("1.0");
        assertEquals(List.of("strong", "strong"),
                BreedingSimulation.requirements(climbing, BreedingSimulation.run(climbing)).stream()
                        .map(one -> one.reachedBy().orElseThrow().variety()).toList());
    }
}
