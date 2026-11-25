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
import io.github.osasuwu.farmingevolution.checks.BreedingSimulation.Pollination;
import io.github.osasuwu.farmingevolution.checks.BreedingSimulation.Scenario;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BreedingSimulationTest {
    private static final String PACK = """
            {
                "chromosomes": [{"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": SHIFT}],
                "species": [
                  {"id": "flax", "wild": {"fiber_strength": 3}},
                  {"id": "hemp", "wild": {"fiber_strength": 9}}
                ],
                "varieties": [
                  {"id": "plain", "species": "flax", "thresholds": {"fiber_strength": 3}},
                  {"id": "strong", "species": "flax", "thresholds": {"fiber_strength": 7}}
                ]
            }""";

    /** Flax is wild at 3; "plain" asks for what it already has, "strong" for four more. */
    private static final String SCENARIO = """
            {
              "pack": PACK,
              "seed": 23,
              "runs": 50,
              "seeds_per_plant": 2,
              "minutes_per_generation": 5,
              "methods": [
                {"id": "selection", "pollination": "self", "plants": 4, "minutes_limit": 200},
                {"id": "wind", "pollination": "wind", "plants": 3, "minutes_limit": 150}
              ]
            }
            """.replace("PACK", PACK);
    private static final Method SELECTION = new Method("selection", Pollination.SELF, 4, 200);
    private static final Method WIND = new Method("wind", Pollination.WIND, 3, 150);

    /** Every roll lands on the first outcome of its distribution. */
    private static final RandomGenerator FIRST_OUTCOME = () -> 0L;

    private static Scenario parse(String json) {
        return BreedingSimulation.parse(new StringReader(json), Path.of("."));
    }

    private static Scenario scenario(String shiftChance) {
        return parse(SCENARIO.replace("SHIFT", shiftChance));
    }

    private static Individual flax(int active, int inactive) {
        PackData pack = scenario("0.2").pack();
        return new Breeding(pack).wild("flax", Sex.NONE).with("fiber_strength",
                new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive)));
    }

    private static List<Integer> strengths(Individual individual) {
        Chromosome strength = individual.genome().get("fiber_strength");
        return List.of(((Allele.Numeric) strength.active()).value(), ((Allele.Numeric) strength.inactive()).value());
    }

    @Test
    void scenarioFileGivesSeedRunsSeedsTimeAndMethods() {
        Scenario scenario = scenario("0.2");
        assertEquals(23, scenario.seed());
        assertEquals(50, scenario.runs());
        assertEquals(2, scenario.seedsPerPlant());
        assertEquals(5, scenario.minutesPerGeneration());
        assertEquals(List.of(SELECTION, WIND), scenario.methods());
        assertEquals(List.of("plain", "strong"), List.copyOf(scenario.pack().varieties().keySet()));
        // Whole generations within the method's time: 200 / 5 and 150 / 5.
        assertEquals(40, scenario.generationLimit(SELECTION));
        assertEquals(30, scenario.generationLimit(WIND));
        assertEquals(2, scenario.generationLimit(new Method("short", Pollination.SELF, 1, 14)));
    }

    /** The scenario of the check names the mod's pack data by a path; the simulation reads that file. */
    @Test
    void packDataIsReadFromThePathTheScenarioNames(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pack.json"), PACK.replace("SHIFT", "0.2"), StandardCharsets.UTF_8);
        Scenario scenario = BreedingSimulation.parse(new StringReader(
                SCENARIO.replace("\"pack\": " + PACK, "\"pack\": \"pack.json\"")), directory);
        assertEquals(List.of("plain", "strong"), List.copyOf(scenario.pack().varieties().keySet()));
        assertEquals(List.of("flax", "hemp"), List.copyOf(scenario.pack().species().keySet()));

        String missing = SCENARIO.replace("\"pack\": " + PACK, "\"pack\": \"absent.json\"");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> BreedingSimulation.parse(new StringReader(missing), directory));
        assertEquals(true, refused.getMessage().startsWith("scenario: cannot read pack data "
                + directory.resolve("absent.json")), refused.getMessage());
    }

    @Test
    void generationsAreCountedFromAWildFieldToTheVarietyAndStopAtTheTimeLimit() {
        // Shift chance 1 and the first outcome of every roll: each gamete carries the parent's
        // active value plus one. Wild flax is 3, so the field expresses 7 after 4 generations.
        Scenario scenario = scenario("1.0");
        var strong = scenario.pack().varieties().get("strong");
        // 5 minutes a generation: 20 minutes are 4 generations, 19 minutes only 3.
        assertEquals(OptionalInt.of(4), BreedingSimulation.generations(scenario, strong,
                new Method("selection", Pollination.SELF, 4, 20), FIRST_OUTCOME));
        assertEquals(OptionalInt.empty(), BreedingSimulation.generations(scenario, strong,
                new Method("selection", Pollination.SELF, 4, 19), FIRST_OUTCOME));
        assertEquals(OptionalInt.of(4), BreedingSimulation.generations(scenario, strong,
                new Method("wind", Pollination.WIND, 3, 20), FIRST_OUTCOME));
        // A wild field that already meets the thresholds needs no breeding at all.
        var plain = scenario.pack().varieties().get("plain");
        assertEquals(OptionalInt.of(0), BreedingSimulation.generations(scenario, plain, SELECTION, FIRST_OUTCOME));
    }

    @Test
    void lineHasReachedTheVarietyOnceOneSownPlantOfItsSpeciesExpressesItsThresholds() {
        PackData pack = scenario("0.2").pack();
        var strong = pack.varieties().get("strong");
        // The inactive allele is not expressed: 7 over 2 gives the product, 6 over 9 does not.
        assertEquals(true, BreedingSimulation.reached(List.of(flax(3, 3), flax(7, 2)), strong));
        assertEquals(true, BreedingSimulation.reached(List.of(flax(7, 2)), strong));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(6, 9), flax(6, 9), flax(3, 3)), strong));
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
                new Outcome("plain", SELECTION, 5, atOnce),
                new Outcome("plain", WIND, 5, atOnce),
                new Outcome("strong", SELECTION, 5, never),
                new Outcome("strong", WIND, 5, never)), outcomes);
    }

    @Test
    void reportLineGivesMedianAndWorstGenerationsWithGameTime() {
        Outcome outcome = new Outcome("strong", WIND, 7,
                List.of(OptionalInt.of(9), OptionalInt.of(4), OptionalInt.of(5)));
        // Sorted 4, 5, 9 at 7 minutes a generation; the wind method's limit is 150 minutes.
        assertEquals("variety strong, method wind: median 5 generations (35 min), worst 9 generations (63 min);"
                + " 3 of 3 lines reached the thresholds within 150 min", outcome.line());
        assertEquals(Optional.empty(), outcome.failure());
    }

    @Test
    void lineThatMissesTheThresholdsWithinTheLimitIsAFailure() {
        Outcome outcome = new Outcome("strong", SELECTION, 5,
                List.of(OptionalInt.of(12), OptionalInt.empty(), OptionalInt.of(30), OptionalInt.empty()));
        assertEquals(Optional.of("variety strong, method selection:"
                + " 2 of 4 lines did not reach the thresholds within 200 min"), outcome.failure());
        assertEquals("variety strong, method selection: median 30 generations (150 min), worst 30 generations (150 min);"
                + " 2 of 4 lines reached the thresholds within 200 min", outcome.line());

        Outcome lost = new Outcome("strong", SELECTION, 5, List.of(OptionalInt.empty()));
        assertEquals("variety strong, method selection:"
                + " 0 of 1 lines reached the thresholds within 200 min", lost.line());
    }

    @Test
    void sameSeedGivesTheSameRunsAndAnotherSeedDoesNot() {
        Scenario scenario = scenario("0.2");
        Scenario reseeded = new Scenario(scenario.pack(), 24, scenario.runs(), scenario.seedsPerPlant(),
                scenario.minutesPerGeneration(), scenario.methods());
        assertEquals(BreedingSimulation.run(scenario), BreedingSimulation.run(scenario("0.2")));
        assertNotEquals(BreedingSimulation.run(scenario), BreedingSimulation.run(reseeded));
    }

    /**
     * Shift chance 0, so a seed's alleles are exactly one of the mother's and one of the
     * father's. Selection: every seed of the 2|2 plant is 2|2, of the 9|9 plant 9|9. Wind: a seed
     * of a plant takes the other allele from another plant, never from the mother herself.
     */
    @Test
    void selectionPollinatesEachPlantByItselfAndWindByAnotherPlantOfTheField() {
        Scenario scenario = scenario("0");
        Breeding breeding = new Breeding(scenario.pack());
        List<Individual> field = List.of(flax(2, 2), flax(5, 5), flax(9, 9));

        List<Individual> selfed = BreedingSimulation.harvest(breeding, field, Pollination.SELF, 2, new Random(1));
        assertEquals(List.of(flax(2, 2), flax(2, 2), flax(5, 5), flax(5, 5), flax(9, 9), flax(9, 9)), selfed);

        List<Individual> windborne = BreedingSimulation.harvest(breeding, field, Pollination.WIND, 200, new Random(1));
        assertEquals(600, windborne.size());
        for (int mother = 0; mother < field.size(); mother++) {
            int own = strengths(field.get(mother)).get(0);
            Set<Integer> fathers = new TreeSet<>();
            for (Individual seed : windborne.subList(mother * 200, mother * 200 + 200)) {
                List<Integer> alleles = new java.util.ArrayList<>(strengths(seed));
                if (!alleles.remove(Integer.valueOf(own))) {
                    throw new AssertionError("Seed " + seed + " of the " + own + "|" + own + " plant has no allele of it");
                }
                fathers.add(alleles.get(0));
            }
            // Both other plants father seeds, the mother none: 200 seeds would show it.
            Set<Integer> others = new TreeSet<>(List.of(2, 5, 9));
            others.remove(own);
            assertEquals(others, fathers, "fathers of the seeds of the " + own + "|" + own + " plant");
        }

        // A field of one plant has no neighbour: the wind leaves it to pollinate itself.
        assertEquals(List.of(flax(5, 5), flax(5, 5)),
                BreedingSimulation.harvest(breeding, List.of(flax(5, 5)), Pollination.WIND, 2, new Random(1)));
    }

    @Test
    void theBestSeedsByTheExpressedValueAreSownAsManyAsTheFieldHolds() {
        var strong = scenario("0.2").pack().varieties().get("strong");
        Individual showy = flax(7, 2);
        Individual solid = flax(6, 6);
        Individual middling = flax(5, 6);
        Individual weak = flax(3, 3);
        List<Individual> seeds = List.of(weak, middling, solid, showy);
        // Expressed: 7, 6, 5, 3. The carried allele is not seen: 5|6 ranks below 6|6, not with it.
        assertEquals(List.of(showy, solid), BreedingSimulation.sown(seeds, strong, 2));
        assertEquals(List.of(showy, solid, middling), BreedingSimulation.sown(seeds, strong, 3));
        assertEquals(List.of(showy, solid, middling, weak), BreedingSimulation.sown(seeds, strong, 9));
    }

    @Test
    void valueAboveTheThresholdDoesNotOutrankTheThreshold() {
        // With two thresholds a surplus in one trait must not pay for a shortfall in the other;
        // with one, it shows as a tie that keeps harvest order.
        var strong = scenario("0.2").pack().varieties().get("strong");
        Individual first = flax(7, 7);
        Individual surplus = flax(10, 10);
        Individual short6 = flax(6, 6);
        assertEquals(List.of(first, surplus), BreedingSimulation.sown(List.of(short6, first, surplus), strong, 2));
    }

    @Test
    void scenarioThatWouldSimulateNothingOrThatTheModelCannotBreedIsRefused() {
        String base = SCENARIO.replace("SHIFT", "0.2");
        String noVarieties = base.replaceAll("(?s)\"varieties\": \\[.*?\\]\\s*\\}", "\"varieties\": []}");
        assertEquals("scenario: pack data has no varieties to breed", refusal(noVarieties));

        String noMethods = base.replaceAll("(?s)\"methods\": \\[.*\\]", "\"methods\": []");
        assertEquals("scenario: no methods", refusal(noMethods));

        assertEquals("scenario: runs must be at least 1, got 0", refusal(base.replace("\"runs\": 50", "\"runs\": 0")));
        assertEquals("scenario: seeds_per_plant must be at least 1, got 0",
                refusal(base.replace("\"seeds_per_plant\": 2", "\"seeds_per_plant\": 0")));
        assertEquals("scenario: minutes_per_generation must be at least 1, got 0",
                refusal(base.replace("\"minutes_per_generation\": 5", "\"minutes_per_generation\": 0")));
        assertEquals("scenario: missing key seeds_per_plant",
                refusal(base.replace("\"seeds_per_plant\": 2,", "")));

        assertEquals("scenario: method wind: plants must be at least 1, got 0",
                refusal(base.replace("\"plants\": 3", "\"plants\": 0")));
        // A limit below one generation would never be hit: the stage could not fail, and could not stop.
        assertEquals("scenario: method wind: minutes_limit must be at least one generation (5 min), got 4",
                refusal(base.replace("\"minutes_limit\": 150", "\"minutes_limit\": 4")));
        assertEquals("scenario: method wind: pollination must be \"self\" or \"wind\", got \"analyzer\"",
                refusal(base.replace("\"pollination\": \"wind\"", "\"pollination\": \"analyzer\"")));

        String sexed = base.replace("{\"id\": \"flax\", \"wild\"", "{\"id\": \"flax\", \"two_sexed\": true, \"wild\"");
        assertEquals("scenario: variety plain: species flax has sexes, and the simulation breeds plants that"
                + " pollinate themselves or each other", refusal(sexed));
    }

    private static String refusal(String json) {
        return assertThrows(IllegalArgumentException.class, () -> parse(json)).getMessage();
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
              "seeds_per_plant": 2,
              "minutes_per_generation": 5,
              "methods": [{"id": "selection", "pollination": "self", "plants": 4, "minutes_limit": 15}]
            }
            """;

    /** Every roll lands on the last outcome of its distribution. */
    private static final RandomGenerator LAST_OUTCOME = () -> -1L;

    private static Scenario bounded() {
        return parse(BOUNDED);
    }

    @Test
    void plantAboveTheCeilingIsOutsideTheVariety() {
        var band = bounded().pack().varieties().get("band");
        // Both edges of 5..6 are inside; the inactive allele is not expressed.
        assertEquals(true, BreedingSimulation.reached(List.of(flax(5, 9)), band));
        assertEquals(true, BreedingSimulation.reached(List.of(flax(6, 1)), band));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(7, 5), flax(4, 6)), band));
        var low = bounded().pack().varieties().get("low");
        assertEquals(true, BreedingSimulation.reached(List.of(flax(3, 1), flax(2, 9)), low));
        assertEquals(false, BreedingSimulation.reached(List.of(flax(3, 1), flax(4, 2)), low));
    }

    @Test
    void generationsAreCountedDownToACeilingAndALineThatOnlyClimbsNeverGetsThere() {
        Scenario scenario = bounded();
        Method selection = scenario.methods().get(0);
        // Shift chance 1. The first outcome of every roll adds one to each passed value, the
        // last takes one away. Wild flax is 3; 15 minutes are 3 generations.
        var low = scenario.pack().varieties().get("low");
        assertEquals(OptionalInt.of(1), BreedingSimulation.generations(scenario, low, selection, LAST_OUTCOME));
        assertEquals(OptionalInt.empty(), BreedingSimulation.generations(scenario, low, selection, FIRST_OUTCOME));
        // 3, 4, 5: the band 5..6 is entered from below after 2 generations.
        var band = scenario.pack().varieties().get("band");
        assertEquals(OptionalInt.of(2), BreedingSimulation.generations(scenario, band, selection, FIRST_OUTCOME));
    }

    @Test
    void selectionLeadsATraitDownToACeilingAsItLeadsItUpToAThreshold() {
        var low = bounded().pack().varieties().get("low");
        Individual five = flax(5, 5);
        Individual three = flax(3, 3);
        Individual hidden = flax(2, 6);
        Individual two = flax(2, 2);
        // Expressed 5, 3, 2, 2: the two at the ceiling, in harvest order; the carried 6 is not seen.
        assertEquals(List.of(hidden, two), BreedingSimulation.sown(List.of(five, three, hidden, two), low, 2));
    }

    @Test
    void valueAboveTheCeilingRanksBelowAValueInsideTheRegionAndBelowANearerMiss() {
        var band = bounded().pack().varieties().get("band");
        Individual eight = flax(8, 8);
        Individual four = flax(4, 4);
        Individual one = flax(1, 1);
        Individual six = flax(6, 6);
        // Distance to 5..6: 8 is two above, 4 is one below, 1 is four below, 6 is inside.
        assertEquals(List.of(six, four), BreedingSimulation.sown(List.of(eight, four, one, six), band, 2));
        assertEquals(List.of(eight, one), BreedingSimulation.sown(List.of(one, eight), band, 2));
    }

    /** "sturdy" is passed by "strong" and by the narrower "finest", not by the wider "plain". */
    private static Scenario required(String shiftChance) {
        return parse(SCENARIO.replace("SHIFT", shiftChance)
                .replace("{\"id\": \"strong\",", "{\"id\": \"finest\", \"species\": \"flax\","
                        + " \"thresholds\": {\"fiber_strength\": 9}},\n{\"id\": \"strong\",")
                .replaceFirst("(?s)(\"varieties\": \\[.*?\\])", "$1, \"requirements\": ["
                        + "{\"id\": \"sturdy\", \"species\": \"flax\", \"thresholds\": {\"fiber_strength\": 7}}]"));
    }

    private static Outcome outcome(String variety, Method method, Integer... generations) {
        return new Outcome(variety, method, 5, java.util.Arrays.stream(generations)
                .map(one -> one == null ? OptionalInt.empty() : OptionalInt.of(one)).toList());
    }

    @Test
    void requirementIsReachedThroughThePassingVarietyWithTheLowestMedian() {
        Outcome strongBySelection = outcome("strong", SELECTION, 30, 10, 12);
        Outcome finestByWind = outcome("finest", WIND, 9, 8);
        List<Outcome> outcomes = List.of(
                // Reached at once, but "plain" does not pass the requirement.
                outcome("plain", SELECTION, 0, 0, 0),
                outcome("plain", WIND, 0, 0),
                outcome("finest", SELECTION, 20, 25, 40),
                finestByWind,
                strongBySelection,
                // The lower median, but one line of two did not get there.
                outcome("strong", WIND, 5, null));
        List<BreedingSimulation.RequirementOutcome> reached =
                BreedingSimulation.requirements(required("0.2"), outcomes);
        assertEquals(List.of(
                new BreedingSimulation.RequirementOutcome(
                        "sturdy", SELECTION, List.of("finest", "strong"), Optional.of(strongBySelection)),
                new BreedingSimulation.RequirementOutcome(
                        "sturdy", WIND, List.of("finest", "strong"), Optional.of(finestByWind))), reached);
        // Sorted 10, 12, 30 and sorted 8, 9, at 5 minutes a generation.
        assertEquals("requirement sturdy, method selection: variety strong,"
                + " median 12 generations (60 min), worst 30 generations (150 min)", reached.get(0).line());
        assertEquals("requirement sturdy, method wind: variety finest,"
                + " median 9 generations (45 min), worst 9 generations (45 min)", reached.get(1).line());
        assertEquals(Optional.empty(), reached.get(0).failure());
    }

    @Test
    void equalMediansAreSettledByTheWorstLineAndThenByPackDataOrder() {
        Outcome finest = outcome("finest", SELECTION, 12, 12, 20);
        Outcome strong = outcome("strong", SELECTION, 12, 12, 19);
        assertEquals(Optional.of(strong), BreedingSimulation.requirements(required("0.2"),
                List.of(finest, outcome("finest", WIND, 1), strong, outcome("strong", WIND, 1)))
                .get(0).reachedBy());
        Outcome same = outcome("strong", SELECTION, 12, 12, 20);
        assertEquals(Optional.of(finest), BreedingSimulation.requirements(required("0.2"),
                List.of(same, outcome("strong", WIND, 1), finest, outcome("finest", WIND, 1)))
                .get(0).reachedBy());
    }

    @Test
    void requirementWhoseEveryPassingVarietyLosesALineIsOutOfReachAndIsAFailure() {
        List<BreedingSimulation.RequirementOutcome> reached = BreedingSimulation.requirements(required("0.2"), List.of(
                outcome("plain", SELECTION, 0), outcome("plain", WIND, 0),
                outcome("finest", SELECTION, 20, null), outcome("finest", WIND, 9),
                outcome("strong", SELECTION, null, 15), outcome("strong", WIND, 5)));
        assertEquals(new BreedingSimulation.RequirementOutcome(
                "sturdy", SELECTION, List.of("finest", "strong"), Optional.empty()), reached.get(0));
        assertEquals("requirement sturdy, method selection: out of reach", reached.get(0).line());
        assertEquals(Optional.of("requirement sturdy, method selection: none of the varieties that pass it"
                + " (finest, strong) was reached by every line within 200 min"), reached.get(0).failure());
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
        // Shift chance 1: with seed 23 every line of both methods climbs to 7 within the limit.
        Scenario climbing = required("1.0");
        assertEquals(List.of("strong", "strong"),
                BreedingSimulation.requirements(climbing, BreedingSimulation.run(climbing)).stream()
                        .map(one -> one.reachedBy().orElseThrow().variety()).toList());
    }

    /**
     * The scenario of the check pipeline breeds the mod's own pack data (AC7 of CONCEPT.md): both
     * methods of the first hour, selection and wind, see only the expressed values and reach a
     * variety that passes the requirement of the belt's fibre.
     */
    @Test
    void scenarioOfTheCheckBreedsTheModsPackDataBySelectionAndWind() throws IOException {
        Path file = Path.of("breeding-simulation.json");
        Scenario scenario;
        try (var json = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            scenario = BreedingSimulation.parse(json, file.toAbsolutePath().getParent());
        }
        assertEquals(List.of(Pollination.SELF, Pollination.WIND),
                scenario.methods().stream().map(Method::pollination).toList());
        assertEquals(true, scenario.pack().varieties().containsKey("strong_flax"));
        assertEquals(true, scenario.pack().requirements().containsKey("strong_fiber"));
    }
}
