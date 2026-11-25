package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Hybridization;
import io.github.osasuwu.farmingevolution.climate.Climate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The place of the GameTest grid (issue #82). The game puts the grid of all GameTests at a
 * random point of the world, and the climate stub differs from place to place, so every run
 * of the GameTests went in a random climate: a hybridization that depends on the climate fired
 * in some runs and not in others, in the plants the tests grow. The GameTest server of
 * {@code gradlew runGameTestServer} puts the grid at {@link #GRID_ORIGIN} instead: a dev-only
 * mod of that run alone does it (source set gametestGrid, see build.gradle).
 *
 * <p>The GameTests that need a particular climate ask for it at absolute positions of their
 * own ({@link ClimateGameTests}); every other test assumes that the climate of the grid
 * triggers no hybridization. This test is that assumption, made a check.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GameTestGridGameTests {
    /**
     * The north-west corner of the first test of the grid: the origin of the world, on the
     * floor of the flat world the game makes for the GameTests (the height the game itself
     * uses). The climate stub gives 12 °C and humidity 50 % here.
     */
    public static final BlockPos GRID_ORIGIN = new BlockPos(0, -59, 0);

    /** How the game lays the grid out (StructureGridSpawner of GameTestServer): tests per row, gaps. */
    private static final int TESTS_PER_ROW = 8;
    private static final int SPACE_BETWEEN_COLUMNS = 5;
    private static final int SPACE_BETWEEN_ROWS = 6;

    private GameTestGridGameTests() {
    }

    @GameTest(template = "empty")
    public static void gridStandsWhereNoClimateHybridizationHolds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Collection<TestFunction> tests = GameTestRegistry.getAllTestFunctions();
        // The widest template, in either direction: a rotated test swaps its width and depth.
        int widest = 1;
        for (TestFunction test : tests) {
            Vec3i size = level.getStructureManager().get(ResourceLocation.parse(test.structureName()))
                    .orElseThrow(() -> new IllegalStateException("Missing test structure: " + test.structureName()))
                    .getSize();
            widest = Math.max(widest, Math.max(size.getX(), size.getZ()));
        }
        // An upper bound of the grid: every test as wide as the widest, rows of TESTS_PER_ROW.
        int rows = (tests.size() + TESTS_PER_ROW - 1) / TESTS_PER_ROW;
        int minX = GRID_ORIGIN.getX();
        int minZ = GRID_ORIGIN.getZ();
        int maxX = minX + TESTS_PER_ROW * (widest + SPACE_BETWEEN_COLUMNS);
        int maxZ = minZ + rows * (widest + SPACE_BETWEEN_ROWS);

        BlockPos here = helper.absolutePos(BlockPos.ZERO);
        if (here.getX() < minX || here.getX() > maxX || here.getZ() < minZ || here.getZ() > maxZ) {
            helper.fail("Expected the GameTest grid at " + GRID_ORIGIN.toShortString() + " (columns x " + minX + ".." + maxX
                    + ", z " + minZ + ".." + maxZ + "), but this test runs at " + here.toShortString()
                    + ": the place of the grid is not pinned");
        }

        // Only the climate part of each condition: the place has no soil yet, and a soil model
        // must not hide a climate that would trigger the rule once soil is there. A rule
        // without a climate condition fires everywhere alike, so the place does not matter to it.
        List<Hybridization> byClimate = FarmingEvolution.PACK.hybridizations().stream()
                .filter(rule -> !rule.condition().climate().isEmpty())
                .toList();
        for (Hybridization rule : byClimate) {
            Hybridization.Condition climateOnly = new Hybridization.Condition(Set.of(), rule.condition().climate());
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos column = new BlockPos(x, GRID_ORIGIN.getY(), z);
                    Climate climate = FarmingEvolution.CLIMATE.at(level, column);
                    if (climateOnly.holds(new Environment(climate.readings(), null))) {
                        helper.fail("The climate " + climate + " at " + column.toShortString()
                                + ", inside the GameTest grid, meets the climate condition " + rule.condition().climate()
                                + " of the hybridization " + rule.firstParent() + " x " + rule.secondParent() + " -> "
                                + rule.result() + ": tests that grow these species would depend on it");
                    }
                }
            }
        }
        helper.succeed();
    }
}
