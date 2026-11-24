package io.github.osasuwu.farmingevolution.breeding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PackDataTest {
    /** Sound pack data. The vigor trait and the aurochs are used by nothing else, so a case may break them alone. */
    private static final String SOUND = """
            {
              "chromosomes": [
                {"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                {"id": "habit", "kind": "alleles", "alleles": [{"id": "tall", "dominant": true}, {"id": "dwarf"}]},
                {"id": "vigor", "kind": "trait", "min": 1, "max": 5, "shift_chance": 0.1}
              ],
              "species": [
                {"id": "wild_flax", "wild": {"fiber_strength": 2, "habit": "tall"}},
                {"id": "wild_hemp", "wild": {"fiber_strength": 3, "habit": "dwarf"}},
                {"id": "fiber_flax", "wild": {"fiber_strength": 5, "habit": "dwarf"}},
                {"id": "aurochs", "two_sexed": true, "wild": {"vigor": 4}}
              ],
              "hybridizations": [
                {"parents": ["wild_flax", "wild_hemp"], "result": "fiber_flax", "chance": 0.25,
                 "conditions": {"soil": ["loam"], "climate": {"temperature": {"min": 10, "max": 25}}}}
              ],
              "varieties": [
                {"id": "strong_fiber", "species": "fiber_flax", "thresholds": {"fiber_strength": 7}},
                {"id": "fine_fiber", "species": "fiber_flax", "thresholds": {"fiber_strength": 4}}
              ]
            }
            """;

    /** One broken entry: the piece of SOUND to replace, its replacement, the message the pack author must get. */
    private record Fault(String sound, String broken, String message) {
    }

    private static final String HYBRIDIZATION = "hybridization wild_flax + wild_hemp -> fiber_flax: ";

    private static final List<Fault> FAULTS = List.of(
            new Fault("{\"id\": \"vigor\", \"kind\": \"trait\"", "{\"id\": \"species\", \"kind\": \"trait\"",
                    "chromosome id \"species\" is reserved"),
            new Fault("{\"id\": \"vigor\", \"kind\": \"trait\"", "{\"id\": \"habit\", \"kind\": \"trait\"",
                    "duplicate chromosome habit"),
            new Fault("{\"id\": \"vigor\", \"kind\": \"trait\"", "{\"id\": \"vigor\", \"kind\": \"gauge\"",
                    "chromosome vigor: unknown kind gauge"),
            new Fault("\"min\": 1, \"max\": 5", "\"min\": 6, \"max\": 5",
                    "chromosome vigor: min 6 is above max 5"),
            new Fault("\"shift_chance\": 0.1", "\"shift_chance\": 1.1",
                    "chromosome vigor: shift_chance 1.1 is outside 0..1"),
            new Fault("\"shift_chance\": 0.1", "\"shift_chance\": -0.1",
                    "chromosome vigor: shift_chance -0.1 is outside 0..1"),
            new Fault("{\"id\": \"aurochs\"", "{\"id\": \"wild_flax\"",
                    "duplicate species wild_flax"),
            new Fault("{\"vigor\": 4}", "{\"vigor\": 4, \"aroma\": 1}",
                    "species aurochs: unknown chromosome aroma"),
            new Fault("{\"vigor\": 4}", "{\"vigor\": 6}",
                    "species aurochs: vigor 6 is outside 1..5"),
            new Fault("{\"vigor\": 4}", "{\"vigor\": 0}",
                    "species aurochs: vigor 0 is outside 1..5"),
            new Fault("{\"vigor\": 4}", "{\"vigor\": \"high\"}",
                    "species aurochs: vigor high is not a whole number"),
            new Fault("\"fiber_strength\": 5, \"habit\": \"dwarf\"", "\"fiber_strength\": 5, \"habit\": \"bushy\"",
                    "species fiber_flax: habit has no allele bushy"),
            new Fault("\"result\": \"fiber_flax\"", "\"result\": \"ghost\"",
                    "hybridization wild_flax + wild_hemp -> ghost: unknown species ghost"),
            new Fault("[\"wild_flax\", \"wild_hemp\"]", "[\"wild_flax\", \"ghost\"]",
                    "hybridization wild_flax + ghost -> fiber_flax: unknown species ghost"),
            new Fault("[\"wild_flax\", \"wild_hemp\"]", "[\"ghost\", \"wild_hemp\"]",
                    "hybridization ghost + wild_hemp -> fiber_flax: unknown species ghost"),
            new Fault("\"chance\": 0.25", "\"chance\": 1.5", HYBRIDIZATION + "chance 1.5 is outside 0..1"),
            new Fault("\"chance\": 0.25", "\"chance\": -0.5", HYBRIDIZATION + "chance -0.5 is outside 0..1"),
            new Fault("{\"fiber_strength\": 3, \"habit\": \"dwarf\"}", "{\"fiber_strength\": 3}",
                    HYBRIDIZATION + "wild_flax and wild_hemp have different chromosome sets"),
            new Fault("{\"fiber_strength\": 5, \"habit\": \"dwarf\"}", "{\"fiber_strength\": 5}",
                    HYBRIDIZATION + "wild_flax and fiber_flax have different chromosome sets"),
            new Fault("{\"id\": \"wild_hemp\"", "{\"id\": \"wild_hemp\", \"two_sexed\": true",
                    HYBRIDIZATION + "wild_flax and wild_hemp differ in two_sexed"),
            new Fault("{\"id\": \"fiber_flax\", \"wild\"", "{\"id\": \"fiber_flax\", \"two_sexed\": true, \"wild\"",
                    HYBRIDIZATION + "wild_flax and fiber_flax differ in two_sexed"),
            new Fault("{\"id\": \"fine_fiber\"", "{\"id\": \"strong_fiber\"",
                    "duplicate variety strong_fiber"),
            new Fault("{\"id\": \"fine_fiber\", \"species\": \"fiber_flax\"", "{\"id\": \"fine_fiber\", \"species\": \"ghost\"",
                    "variety fine_fiber: unknown species ghost"),
            new Fault("{\"fiber_strength\": 4}", "{\"vigor\": 4}",
                    "variety fine_fiber: fiber_flax has no trait vigor"),
            new Fault("{\"fiber_strength\": 4}", "{\"habit\": 4}",
                    "variety fine_fiber: fiber_flax has no trait habit"),
            new Fault("{\"fiber_strength\": 4}", "{\"fiber_strength\": 11}",
                    "variety fine_fiber: fiber_strength threshold 11 is outside 1..10"),
            new Fault("{\"fiber_strength\": 4}", "{\"fiber_strength\": 0}",
                    "variety fine_fiber: fiber_strength threshold 0 is outside 1..10"),
            // A misspelled or extra key must not load as different rules.
            new Fault("\"varieties\": [", "\"sorts\": [], \"varieties\": [", "pack data: unknown key sorts"),
            // The old name of the new-species rule (#45) must not load, nor pass for an unknown key.
            new Fault("\"hybridizations\": [", "\"mutations\": [",
                    "pack data: key mutations is renamed to hybridizations"),
            new Fault("\"min\": 1, \"max\": 5", "\"min\": 1, \"max\": 5, \"step\": 1",
                    "chromosome vigor: unknown key step"),
            new Fault("{\"id\": \"dwarf\"}]}", "{\"id\": \"dwarf\", \"dominance\": true}]}",
                    "chromosome habit: unknown key dominance"),
            new Fault("\"two_sexed\": true", "\"two_sexed\": true, \"sexes\": 2",
                    "species aurochs: unknown key sexes"),
            new Fault("\"kind\": \"alleles\", \"alleles\"", "\"kind\": \"alleles\", \"order\": 1, \"alleles\"",
                    "chromosome habit: unknown key order"),
            new Fault("\"chance\": 0.25", "\"chance\": 0.25, \"note\": 1", HYBRIDIZATION + "unknown key note"),
            new Fault("\"soil\": [\"loam\"]", "\"soils\": [\"loam\"]", HYBRIDIZATION + "conditions: unknown key soils"),
            new Fault("{\"min\": 10, \"max\": 25}", "{\"mim\": 10, \"max\": 25}",
                    HYBRIDIZATION + "temperature: unknown key mim"),
            new Fault("{\"id\": \"fine_fiber\"", "{\"id\": \"fine_fiber\", \"tier\": 1",
                    "variety fine_fiber: unknown key tier"),
            // A missing key is named with its entry.
            new Fault("{\"id\": \"aurochs\", ", "{", "species: missing key id"),
            new Fault("{\"id\": \"aurochs\"", "{\"id\": \"\"", "species: empty id"),
            new Fault("\"result\": \"fiber_flax\", ", "", "hybridization: missing key result"),
            new Fault("\"chance\": 0.25,", "", HYBRIDIZATION + "missing key chance"),
            new Fault("\"min\": 1, \"max\": 5, ", "\"min\": 1, ", "chromosome vigor: missing key max"),
            new Fault("\"species\": \"fiber_flax\", \"thresholds\": {\"fiber_strength\": 4}", "\"species\": \"fiber_flax\"",
                    "variety fine_fiber: missing key thresholds"),
            // Values of the wrong kind are not bent into shape.
            new Fault("\"two_sexed\": true", "\"two_sexed\": \"yes\"",
                    "species aurochs: two_sexed must be true or false"),
            new Fault("\"min\": 1, \"max\": 5", "\"min\": 1.9, \"max\": 5",
                    "chromosome vigor: min 1.9 is not a whole number"),
            new Fault("\"min\": 1, \"max\": 5", "\"min\": 1, \"max\": 1e99",
                    "chromosome vigor: max 1e99 is not a whole number"),
            new Fault("{\"fiber_strength\": 4}", "{\"fiber_strength\": 4.5}",
                    "variety fine_fiber: fiber_strength threshold 4.5 is not a whole number"),
            new Fault("{\"id\": \"dwarf\"}]}", "{\"id\": \"tall\"}]}", "chromosome habit: duplicate allele tall"),
            new Fault("{\"min\": 10, \"max\": 25}", "{\"min\": 25, \"max\": 10}",
                    HYBRIDIZATION + "temperature range 25.0..10.0 is empty"));

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

    @Test
    void soundPackDataGivesItsVarietiesWithTheirThresholds() {
        PackData data = PackData.parse(new StringReader(SOUND));
        assertEquals(Map.of(
                "strong_fiber", new Variety("strong_fiber", "fiber_flax", Map.of("fiber_strength", 7)),
                "fine_fiber", new Variety("fine_fiber", "fiber_flax", Map.of("fiber_strength", 4))), data.varieties());
    }

    @Test
    void readPackDataAndReportedOddsCannotBeChanged() {
        PackData data = PackData.parse(new StringReader(SOUND));
        Species flax = data.species("wild_flax");
        Hybridization hybridization = data.hybridizations().get(0);
        Breeding breeding = new Breeding(data);
        CrossOdds odds = breeding.odds(
                breeding.wild("wild_flax", Sex.NONE), breeding.wild("wild_flax", Sex.NONE), Environment.NONE);
        Map<String, Runnable> changes = new LinkedHashMap<>();
        changes.put("chromosomes", () -> data.chromosomes().clear());
        changes.put("species", () -> data.species().clear());
        changes.put("hybridizations", () -> data.hybridizations().clear());
        changes.put("varieties", () -> data.varieties().clear());
        changes.put("wild type", () -> flax.wild().clear());
        changes.put("alleles", () -> ((ChromosomeType.Categorical) data.chromosomes().get("habit")).alleles().clear());
        changes.put("soils", () -> hybridization.condition().soils().clear());
        changes.put("climate", () -> hybridization.condition().climate().clear());
        changes.put("thresholds", () -> data.varieties().get("fine_fiber").thresholds().clear());
        changes.put("odds", () -> odds.chromosomes().clear());
        List<String> changed = new ArrayList<>();
        changes.forEach((name, change) -> {
            try {
                change.run();
                changed.add(name);
            } catch (UnsupportedOperationException expected) {
                // refused, as it must be
            }
        });
        assertEquals(List.of(), changed);
    }

    @Test
    void everyBrokenEntryIsRejectedWithAMessageNamingTheEntryAndTheFault() {
        List<String> failures = new ArrayList<>();
        for (Fault fault : FAULTS) {
            // A replacement that matches nothing would leave the data sound and the case empty.
            int at = SOUND.indexOf(fault.sound());
            assertTrue(at >= 0 && at == SOUND.lastIndexOf(fault.sound()), "matches once: " + fault.sound());
            String got = rejection(SOUND.replace(fault.sound(), fault.broken()));
            if (!got.equals(fault.message())) {
                failures.add("expected \"" + fault.message() + "\" but got \"" + got + "\"");
            }
        }
        assertEquals(List.of(), failures);
    }

    @Test
    void dataThatIsNotTheExpectedJsonIsRejectedAsMalformed() {
        List<String> failures = new ArrayList<>();
        for (String malformed : List.of(
                "{",
                "[]",
                SOUND.replace("[\"wild_flax\", \"wild_hemp\"]", "[\"wild_flax\"]"),
                SOUND.replace("\"min\": 1, \"max\": 5", "\"min\": \"low\", \"max\": 5"),
                SOUND.replace("{\"id\": \"aurochs\"", "{\"id\": {}"),
                SOUND.replace("\"wild\": {\"vigor\": 4}", "\"wild\": 4"),
                // An optional key holding the wrong kind of value.
                "{\"chromosomes\": [], \"species\": [], \"hybridizations\": {}}",
                "{\"chromosomes\": [], \"species\": [], \"varieties\": 3}",
                SOUND.replace("\"soil\": [\"loam\"]", "\"soil\": {}"),
                SOUND.replace("\"climate\": {\"temperature\": {\"min\": 10, \"max\": 25}}", "\"climate\": []"),
                SOUND.replace("{\"soil\": [\"loam\"], \"climate\": {\"temperature\": {\"min\": 10, \"max\": 25}}}",
                        "[]"))) {
            String got = rejection(malformed);
            if (!got.startsWith("malformed pack data: ")) {
                failures.add(got);
            }
        }
        assertEquals(List.of(), failures);
    }
}
