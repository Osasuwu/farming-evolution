package io.github.osasuwu.farmingevolution.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.osasuwu.farmingevolution.breeding.PackData;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class VarietyGapsTest {
    /** Two traits of 1..10; the species and the varieties are given by the test. */
    private static List<String> gaps(String species, String varieties) {
        return VarietyGaps.lines(PackData.parse(new StringReader("""
                {
                  "chromosomes": [
                    {"id": "bitterness", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2},
                    {"id": "hardness", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2}
                  ],
                  "species": [SPECIES],
                  "varieties": [VARIETIES]
                }
                """.replace("SPECIES", species).replace("VARIETIES", varieties))));
    }

    private static final String SEED = "{\"id\": \"seed\", \"wild\": {\"bitterness\": 5, \"hardness\": 5}}";

    @Test
    void valuesBelowASingleThresholdAreAGap() {
        assertEquals(List.of("species seed: no variety for bitterness 1..6"), gaps(SEED,
                "{\"id\": \"spicy\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": 7}}"));
    }

    @Test
    void gapBetweenTwoVarietiesIsNamedWithBothItsEdgesAndASingleValueWithoutARange() {
        assertEquals(List.of("species seed: no variety for bitterness 4..6"), gaps(SEED,
                "{\"id\": \"food\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": {\"max\": 3}}},"
                        + "{\"id\": \"spicy\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": 7}}"));
        assertEquals(List.of("species seed: no variety for bitterness 4"), gaps(SEED,
                "{\"id\": \"food\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": {\"max\": 3}}},"
                        + "{\"id\": \"spicy\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": 5}}"));
    }

    @Test
    void gapsOfTwoTraitsAreRegionsThatNameOnlyTheTraitsTheyNarrow() {
        // The example of CONCEPT.md: food is bitterness 3 or less with hardness 4 or less, spicy
        // is bitterness 7 or more. Bitterness 5 fits neither at any hardness; bitterness 2 with
        // hardness 6 fits neither.
        assertEquals(List.of(
                "species seed: no variety for bitterness 1..3, hardness 5..10",
                "species seed: no variety for bitterness 4..6"), gaps(SEED,
                "{\"id\": \"food\", \"species\": \"seed\","
                        + " \"thresholds\": {\"bitterness\": {\"max\": 3}, \"hardness\": {\"max\": 4}}},"
                        + "{\"id\": \"spicy\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": 7}}"));
    }

    @Test
    void nestedVarietyLeavesNoGapInsideTheWiderOne() {
        assertEquals(List.of("species seed: no variety for bitterness 1..6"), gaps(SEED,
                "{\"id\": \"spicy\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": 7}},"
                        + "{\"id\": \"hot\", \"species\": \"seed\","
                        + " \"thresholds\": {\"bitterness\": 9, \"hardness\": {\"min\": 3, \"max\": 6}}}"));
    }

    @Test
    void varietiesThatMeetEdgeToEdgeLeaveNoGap() {
        assertEquals(List.of("species seed: no gaps"), gaps(SEED,
                "{\"id\": \"food\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": {\"max\": 6}}},"
                        + "{\"id\": \"spicy\", \"species\": \"seed\", \"thresholds\": {\"bitterness\": 7}}"));
    }

    @Test
    void everySpeciesIsListedInPackOrderAndOneWithoutVarietiesSaysSo() {
        assertEquals(List.of(
                "species weed: no varieties",
                "species seed: no variety for hardness 3..10",
                "species herb: no variety for bitterness 1..7"), gaps(
                "{\"id\": \"weed\", \"wild\": {\"bitterness\": 5}}," + SEED
                        + ", {\"id\": \"herb\", \"wild\": {\"bitterness\": 5}}",
                // Each variety counts only for its own species.
                "{\"id\": \"sharp\", \"species\": \"herb\", \"thresholds\": {\"bitterness\": 8}},"
                        + "{\"id\": \"soft\", \"species\": \"seed\", \"thresholds\": {\"hardness\": {\"max\": 2}}}"));
    }
}
