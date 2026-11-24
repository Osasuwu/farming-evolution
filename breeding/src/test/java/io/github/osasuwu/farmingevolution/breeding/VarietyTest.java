package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The variety rule of CONCEPT.md, "Селекция → Сорт": the region of a variety and which variety a plant gives. */
class VarietyTest {
    private static final String STRONG = "{\"id\": \"strong\", \"species\": \"flax\", \"thresholds\": {\"fiber_strength\": 7}}";
    private static final String LONG = "{\"id\": \"long\", \"species\": \"flax\", \"thresholds\": {\"fiber_length\": 7}}";
    private static final String SELECT = "{\"id\": \"select\", \"species\": \"flax\","
            + " \"thresholds\": {\"fiber_strength\": 7, \"fiber_length\": 7}}";

    /** Two species with the same two traits, both running from 1 to 10, and the given variety entries. */
    private static String flax(String... varieties) {
        return """
                {
                  "chromosomes": [
                    {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                    {"id": "fiber_length", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2}
                  ],
                  "species": [
                    {"id": "flax", "wild": {"fiber_strength": 5, "fiber_length": 5}},
                    {"id": "hemp", "wild": {"fiber_strength": 5, "fiber_length": 5}}
                  ],
                  "varieties": [%s]
                }
                """.formatted(String.join(",\n", varieties));
    }

    /** A flax variety with the given thresholds object. */
    private static String variety(String id, String thresholds) {
        return "{\"id\": \"" + id + "\", \"species\": \"flax\", \"thresholds\": " + thresholds + "}";
    }

    private static String rejection(String packData) {
        try {
            PackData.parse(new StringReader(packData));
            return "accepted";
        } catch (PackDataException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return "threw " + e;
        }
    }

    /** A plant that expresses the given values and carries 1 as the unexpressed allele of both traits. */
    private static Individual plant(Breeding breeding, String species, int strength, int length) {
        return breeding.wild(species, Sex.NONE)
                .with("fiber_strength", new Chromosome(new Allele.Numeric(strength), new Allele.Numeric(1)))
                .with("fiber_length", new Chromosome(new Allele.Numeric(length), new Allele.Numeric(1)));
    }

    /** The id of the variety each plant gives, "none" for a plant that gives no variety. */
    private static Map<String, String> given(String packData, String species, int[]... plants) {
        Breeding breeding = new Breeding(PackData.parse(new StringReader(packData)));
        Map<String, String> given = new LinkedHashMap<>();
        for (int[] values : plants) {
            Optional<Variety> variety = breeding.variety(plant(breeding, species, values[0], values[1]));
            given.put(values[0] + "/" + values[1], variety.map(Variety::id).orElse("none"));
        }
        return given;
    }

    @Test
    void thresholdIsReadAsNotBelowNotAboveOrBothAndAPlainNumberStaysNotBelow() {
        PackData data = PackData.parse(new StringReader(flax(
                variety("plain", "{\"fiber_strength\": 7}"),
                variety("capped", "{\"fiber_strength\": {\"max\": 3}}"),
                variety("band", "{\"fiber_strength\": {\"min\": 4, \"max\": 6}, \"fiber_length\": {\"min\": 2}}"))));
        assertEquals(Map.of(
                "plain", new Variety("plain", "flax", Map.of("fiber_strength", 7), Map.of()),
                "capped", new Variety("capped", "flax", Map.of(), Map.of("fiber_strength", 3)),
                "band", new Variety("band", "flax",
                        Map.of("fiber_strength", 4, "fiber_length", 2), Map.of("fiber_strength", 6))),
                data.varieties());
    }

    @Test
    void thresholdThatDoesNotNarrowTheTraitRangeOrLeavesNoValueIsRejectedNamingTheVarietyAndTheFault() {
        Map<String, String> faults = new LinkedHashMap<>();
        faults.put("{\"fiber_strength\": 1}", "variety odd: fiber_strength threshold 1 does not narrow 1..10");
        faults.put("{\"fiber_strength\": {\"min\": 1}}", "variety odd: fiber_strength min 1 does not narrow 1..10");
        faults.put("{\"fiber_strength\": {\"max\": 10}}", "variety odd: fiber_strength max 10 does not narrow 1..10");
        faults.put("{\"fiber_strength\": {\"min\": 0}}", "variety odd: fiber_strength min 0 is outside 1..10");
        faults.put("{\"fiber_strength\": {\"min\": 11}}", "variety odd: fiber_strength min 11 is outside 1..10");
        faults.put("{\"fiber_strength\": {\"max\": 0}}", "variety odd: fiber_strength max 0 is outside 1..10");
        faults.put("{\"fiber_strength\": {\"max\": 11}}", "variety odd: fiber_strength max 11 is outside 1..10");
        faults.put("{\"fiber_strength\": {\"min\": 6, \"max\": 5}}", "variety odd: fiber_strength min 6 is above max 5");
        faults.put("{\"fiber_strength\": {}}", "variety odd: fiber_strength sets neither min nor max");
        faults.put("{\"fiber_strength\": {\"min\": 4, \"mux\": 6}}", "variety odd: fiber_strength: unknown key mux");
        faults.put("{\"fiber_strength\": {\"max\": 4.5}}", "variety odd: fiber_strength max 4.5 is not a whole number");
        // The accepting half: a threshold on the far edge of the range, and a region of one value.
        faults.put("{\"fiber_strength\": 10}", "accepted");
        faults.put("{\"fiber_strength\": {\"max\": 1}}", "accepted");
        faults.put("{\"fiber_strength\": {\"min\": 5, \"max\": 5}}", "accepted");
        Map<String, String> got = new LinkedHashMap<>();
        faults.keySet().forEach(thresholds -> got.put(thresholds, rejection(flax(variety("odd", thresholds)))));
        assertEquals(faults, got);
    }

    @Test
    void plantFitsAVarietyOfItsSpeciesWhenEveryBoundedTraitIsWithinTheInclusiveBounds() {
        Breeding breeding = new Breeding(PackData.parse(new StringReader(flax())));
        // Strength from 4 to 6, length at most 3.
        Variety variety = new Variety("band", "flax", Map.of("fiber_strength", 4), Map.of("fiber_strength", 6, "fiber_length", 3));
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("flax 4/3", true);
        expected.put("flax 6/1", true);
        expected.put("flax 3/3", false);
        expected.put("flax 7/3", false);
        expected.put("flax 5/4", false);
        expected.put("hemp 5/2", false);
        Map<String, Boolean> got = new LinkedHashMap<>();
        for (String plant : expected.keySet()) {
            String[] parts = plant.split("[ /]");
            got.put(plant, variety.fits(
                    plant(breeding, parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))));
        }
        assertEquals(expected, got);
        // A trait the variety does not bound takes any value of its range.
        Variety strong = new Variety("strong", "flax", Map.of("fiber_strength", 7));
        assertEquals(List.of(true, true), List.of(
                strong.fits(plant(breeding, "flax", 7, 1)), strong.fits(plant(breeding, "flax", 10, 10))));
    }

    @Test
    void flaxExampleOfTheConceptGivesTheNarrowestFittingVarietyAndNoneOutsideEveryRegion() {
        Map<String, String> table = new LinkedHashMap<>();
        table.put("8/5", "strong");
        table.put("5/9", "long");
        table.put("8/8", "select");
        table.put("5/5", "none");
        int[][] plants = {{8, 5}, {5, 9}, {8, 8}, {5, 5}};
        // The narrowest variety is found by its region, wherever its entry stands in the data.
        assertEquals(table, given(flax(STRONG, SELECT, LONG), "flax", plants));
        assertEquals(table, given(flax(SELECT, STRONG, LONG), "flax", plants));
        assertEquals(table, given(flax(STRONG, LONG, SELECT), "flax", plants));
    }

    @Test
    void plantInAGapBetweenVarietiesOrOfAnotherSpeciesGivesNoVariety() {
        // Strength stands for bitterness, length for toughness: the gap example of the concept.
        String pack = flax(
                variety("food", "{\"fiber_strength\": {\"max\": 3}, \"fiber_length\": {\"max\": 4}}"),
                variety("spice", "{\"fiber_strength\": 7}"));
        Map<String, String> flax = new LinkedHashMap<>();
        flax.put("5/2", "none");
        flax.put("2/6", "none");
        flax.put("3/4", "food");
        flax.put("7/9", "spice");
        assertEquals(flax, given(pack, "flax", new int[][] {{5, 2}, {2, 6}, {3, 4}, {7, 9}}));
        assertEquals(Map.of("3/4", "none", "7/9", "none"), given(pack, "hemp", new int[][] {{3, 4}, {7, 9}}));
    }

    @Test
    void twoVarietiesOfOneSpeciesThatOverlapWithoutNestingNeedAVarietyWithTheirSharedRegion() {
        String shared = "varieties strong and long of flax overlap, neither lies inside the other,"
                + " and no variety of flax has the region they share";
        List<String> got = new ArrayList<>();
        got.add(rejection(flax(STRONG, LONG)));
        // A third variety inside the shared region, but not equal to it, does not settle the pair.
        got.add(rejection(flax(STRONG, LONG, variety("narrow", "{\"fiber_strength\": 8, \"fiber_length\": 7}"))));
        // A variety of another species with exactly the shared region does not either.
        got.add(rejection(flax(STRONG, LONG, SELECT.replace("flax", "hemp"))));
        got.add(rejection(flax(STRONG, LONG, SELECT)));
        // Disjoint regions, nested regions and overlapping regions of two species load.
        got.add(rejection(flax(variety("low", "{\"fiber_strength\": {\"max\": 6}}"), STRONG)));
        got.add(rejection(flax(STRONG, variety("stronger", "{\"fiber_strength\": 9}"))));
        got.add(rejection(flax(STRONG, LONG.replace("flax", "hemp"))));
        assertEquals(List.of(shared, shared, shared, "accepted", "accepted", "accepted", "accepted"), got);
    }

    @Test
    void twoVarietiesOfOneSpeciesWithTheSameRegionAreRejected() {
        String same = "varieties strong and sturdy of flax have the same region";
        assertEquals(List.of(same, same, "accepted"), List.of(
                rejection(flax(STRONG, variety("sturdy", "{\"fiber_strength\": 7}"))),
                rejection(flax(STRONG, variety("sturdy", "{\"fiber_strength\": {\"min\": 7}}"))),
                // The same region on another species is another variety.
                rejection(flax(STRONG, STRONG.replace("flax", "hemp").replace("strong", "sturdy")))));
    }
}
