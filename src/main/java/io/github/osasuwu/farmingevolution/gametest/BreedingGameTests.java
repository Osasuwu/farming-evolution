package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.io.StringReader;
import java.util.Random;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Proves the inheritance engine is on the classpath of a running game: its rules are
 * covered by the unit tests of the breeding subproject, not here.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BreedingGameTests {
    private static final String PACK_DATA = """
            {
              "chromosomes": [{"id": "fiber_strength", "kind": "trait", "min": 1, "max": 10, "shift_chance": 0.2}],
              "species": [{"id": "wild_flax", "wild": {"fiber_strength": 2}}]
            }
            """;

    private BreedingGameTests() {
    }

    @GameTest(template = "empty")
    public static void inheritanceEngineCrossesInsideTheGame(GameTestHelper helper) {
        Breeding breeding = new Breeding(PackData.parse(new StringReader(PACK_DATA)));
        Individual flax = breeding.wild("wild_flax", Sex.NONE);
        Individual offspring = breeding.cross(flax, flax, Environment.NONE, new Random(1));
        if (!offspring.species().equals("wild_flax")) {
            helper.fail("two wild flax gave " + offspring.species());
        }
        helper.succeed();
    }
}
