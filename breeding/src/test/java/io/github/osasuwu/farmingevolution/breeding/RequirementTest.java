package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The raw-material requirement of CONCEPT.md, "Селекция → Сорт → Требование к сырью": a named
 * species plus region, passed by a variety whose region lies inside it (issue #47).
 */
class RequirementTest {
    /** The flax varieties of the example in CONCEPT.md: strong, long and select. */
    private static final List<String> FLAX_VARIETIES = List.of(
            variety("strong", "flax", "{\"fiber_strength\": 7}"),
            variety("long", "flax", "{\"fiber_length\": 7}"),
            variety("select", "flax", "{\"fiber_strength\": 7, \"fiber_length\": 7}"));

    /** Two species with the same two traits, both running from 1 to 10. */
    private static String pack(List<String> varieties, String... requirements) {
        return """
                {
                  "chromosomes": [
                    {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                    {"id": "fiber_length", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                    {"id": "habit", "kind": "alleles", "alleles": [{"id": "tall"}, {"id": "dwarf"}]}
                  ],
                  "species": [
                    {"id": "flax", "wild": {"fiber_strength": 5, "fiber_length": 5, "habit": "tall"}},
                    {"id": "hemp", "wild": {"fiber_strength": 5, "fiber_length": 5}}
                  ],
                  "varieties": [%s],
                  "requirements": [%s]
                }
                """.formatted(String.join(",\n", varieties), String.join(",\n", requirements));
    }

    private static String variety(String id, String species, String thresholds) {
        return "{\"id\": \"" + id + "\", \"species\": \"" + species + "\", \"thresholds\": " + thresholds + "}";
    }

    /** An entry of "requirements"; it is written with the keys of a variety. */
    private static String requirement(String id, String species, String thresholds) {
        return variety(id, species, thresholds);
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

    /** The ids of the varieties that pass each requirement of the pack, in pack data order. */
    private static Map<String, List<String>> passing(String packData) {
        PackData data = PackData.parse(new StringReader(packData));
        Map<String, List<String>> passing = new LinkedHashMap<>();
        data.requirements().values().forEach(requirement ->
                passing.put(requirement.id(), data.passing(requirement).stream().map(Variety::id).toList()));
        return passing;
    }

    @Test
    void requirementIsReadWithItsIdSpeciesAndThresholdsOfBothKinds() {
        PackData data = PackData.parse(new StringReader(pack(
                List.of(variety("band", "flax", "{\"fiber_strength\": {\"min\": 5, \"max\": 6}, \"fiber_length\": 4}")),
                requirement("sturdy_fiber", "flax", "{\"fiber_strength\": {\"min\": 4, \"max\": 8}, \"fiber_length\": 2}"),
                requirement("soft_fiber", "flax", "{\"fiber_strength\": {\"max\": 6}}"))));
        assertEquals(Map.of(
                "sturdy_fiber", new Requirement("sturdy_fiber", "flax",
                        Map.of("fiber_strength", 4, "fiber_length", 2), Map.of("fiber_strength", 8)),
                "soft_fiber", new Requirement("soft_fiber", "flax", Map.of(), Map.of("fiber_strength", 6))),
                data.requirements());
        assertEquals(List.of("sturdy_fiber", "soft_fiber"), List.copyOf(data.requirements().keySet()));
    }

    /** Each entry breaks one rule; the thresholds are held to the rules of a variety's. */
    @Test
    void faultyRequirementIsRefusedByTheRulesOfAVarietyAndNamed() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("outside the range", requirement("r", "flax", "{\"fiber_strength\": 11}"));
        entries.put("not narrowing from below", requirement("r", "flax", "{\"fiber_strength\": 1}"));
        entries.put("not narrowing from above", requirement("r", "flax", "{\"fiber_strength\": {\"max\": 10}}"));
        entries.put("empty region", requirement("r", "flax", "{\"fiber_strength\": {\"min\": 6, \"max\": 5}}"));
        entries.put("no bound", requirement("r", "flax", "{\"fiber_strength\": {}}"));
        entries.put("fraction", requirement("r", "flax", "{\"fiber_strength\": 6.5}"));
        entries.put("not a trait", requirement("r", "flax", "{\"habit\": 2}"));
        entries.put("trait of another species", requirement("r", "hemp", "{\"habit\": 2}"));
        entries.put("unknown species", requirement("r", "nettle", "{\"fiber_strength\": 7}"));
        entries.put("unknown key", "{\"id\": \"r\", \"species\": \"flax\", \"thresholds\": {\"fiber_strength\": 7}, \"rank\": 1}");
        entries.put("no thresholds", "{\"id\": \"r\", \"species\": \"flax\"}");
        entries.put("empty id", requirement("", "flax", "{\"fiber_strength\": 7}"));
        entries.put("duplicate", requirement("r", "flax", "{\"fiber_strength\": 7}") + ","
                + requirement("r", "flax", "{\"fiber_length\": 7}"));

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("outside the range", "requirement r: fiber_strength threshold 11 is outside 1..10");
        expected.put("not narrowing from below", "requirement r: fiber_strength threshold 1 does not narrow 1..10");
        expected.put("not narrowing from above", "requirement r: fiber_strength max 10 does not narrow 1..10");
        expected.put("empty region", "requirement r: fiber_strength min 6 is above max 5");
        expected.put("no bound", "requirement r: fiber_strength sets neither min nor max");
        expected.put("fraction", "requirement r: fiber_strength threshold 6.5 is not a whole number");
        expected.put("not a trait", "requirement r: flax has no trait habit");
        expected.put("trait of another species", "requirement r: hemp has no trait habit");
        expected.put("unknown species", "requirement r: unknown species nettle");
        expected.put("unknown key", "requirement r: unknown key rank");
        expected.put("no thresholds", "requirement r: missing key thresholds");
        expected.put("empty id", "requirement: empty id");
        expected.put("duplicate", "duplicate requirement r");

        Map<String, String> got = new LinkedHashMap<>();
        entries.forEach((fault, entry) -> got.put(fault, rejection(pack(FLAX_VARIETIES, entry))));
        assertEquals(expected, got);
    }

    /** CONCEPT.md: "strong fibre" (strength not below 7) is passed by strong and select flax, not by long. */
    @Test
    void strongFibreOfTheFlaxExampleIsPassedByStrongAndSelectButNotLong() {
        assertEquals(Map.of("strong_fiber", List.of("strong", "select")),
                passing(pack(FLAX_VARIETIES, requirement("strong_fiber", "flax", "{\"fiber_strength\": 7}"))));
    }

    @Test
    void varietyPassesOnlyWhenItsWholeRegionIsInsideTheRequirement() {
        Requirement medium = new Requirement("medium_fiber", "flax", Map.of("fiber_strength", 4), Map.of("fiber_strength", 8));
        Map<String, Variety> varieties = new LinkedHashMap<>();
        varieties.put("exact", new Variety("exact", "flax", Map.of("fiber_strength", 4), Map.of("fiber_strength", 8)));
        varieties.put("narrower", new Variety("narrower", "flax",
                Map.of("fiber_strength", 5, "fiber_length", 7), Map.of("fiber_strength", 7)));
        varieties.put("from below", new Variety("below", "flax", Map.of("fiber_strength", 3), Map.of("fiber_strength", 8)));
        varieties.put("to above", new Variety("above", "flax", Map.of("fiber_strength", 4), Map.of("fiber_strength", 9)));
        // Bounds the strength on one side only, or not at all: plants outside the requirement fit it.
        varieties.put("no ceiling", new Variety("no_ceiling", "flax", Map.of("fiber_strength", 5)));
        varieties.put("no floor", new Variety("no_floor", "flax", Map.of(), Map.of("fiber_strength", 7)));
        varieties.put("other trait", new Variety("other_trait", "flax", Map.of("fiber_length", 5), Map.of("fiber_length", 6)));
        // The region of "exact", but of another species.
        varieties.put("other species", new Variety("other_species", "hemp", Map.of("fiber_strength", 4), Map.of("fiber_strength", 8)));

        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("exact", true);
        expected.put("narrower", true);
        expected.put("from below", false);
        expected.put("to above", false);
        expected.put("no ceiling", false);
        expected.put("no floor", false);
        expected.put("other trait", false);
        expected.put("other species", false);

        Map<String, Boolean> got = new LinkedHashMap<>();
        varieties.forEach((name, variety) -> got.put(name, medium.passedBy(variety)));
        assertEquals(expected, got);
    }

    @Test
    void requirementNoVarietyPassesIsRefusedAndNamed() {
        assertEquals("requirement long_strong_fiber: no variety of flax passes it",
                rejection(pack(List.of(FLAX_VARIETIES.get(0)),
                        requirement("strong_fiber", "flax", "{\"fiber_strength\": 7}"),
                        requirement("long_strong_fiber", "flax", "{\"fiber_strength\": 7, \"fiber_length\": 7}"))));
        // The same region of hemp is passed by a variety of flax only.
        assertEquals("requirement hemp_fiber: no variety of hemp passes it",
                rejection(pack(FLAX_VARIETIES, requirement("hemp_fiber", "hemp", "{\"fiber_strength\": 7}"))));
    }
}
