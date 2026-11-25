package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Farmland without water (issue #85): dry farmland under a plant of the mod stays farmland, as
 * it does under wheat, and the plant stays on it; dry farmland with nothing on it turns to dirt.
 * The farmland is ticked the way the world ticks it, by its own random tick.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FarmlandGameTests {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos PLANT = SOIL.above();

    /** Random ticks of the farmland; one is enough to turn dry farmland with moisture 0 to dirt. */
    private static final int TICKS = 20;

    private FarmlandGameTests() {
    }

    /**
     * Dry farmland, no water near and no rain, a young plant of the mod on it: after random
     * ticks of the farmland it is still farmland, still dry (so nothing but the plant kept it),
     * and the plant is still there.
     */
    @GameTest(template = "empty")
    public static void dryFarmlandUnderPlantStaysAndKeepsPlant(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        BlockState plant = FarmingEvolution.CROP.get().defaultBlockState().setValue(CropBlock.AGE, 2);
        helper.setBlock(PLANT, plant);
        for (int i = 0; i < TICKS; i++) {
            helper.randomTick(SOIL);
        }
        BlockState soil = helper.getBlockState(SOIL);
        if (!soil.is(Blocks.FARMLAND) || soil.getValue(FarmBlock.MOISTURE) != 0) {
            throw new GameTestAssertException("Expected dry farmland (moisture 0) under the plant after " + TICKS
                    + " random ticks, got " + soil);
        }
        if (!helper.getBlockState(PLANT).equals(plant)) {
            throw new GameTestAssertException("Expected the plant " + plant + " to stay on the dry farmland, got "
                    + helper.getBlockState(PLANT));
        }
        helper.succeed();
    }

    /** The same dry farmland with nothing on it turns to dirt on a random tick, as before. */
    @GameTest(template = "empty")
    public static void emptyDryFarmlandTurnsToDirt(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.setBlock(PLANT, Blocks.AIR);
        helper.randomTick(SOIL);
        if (!helper.getBlockState(SOIL).is(Blocks.DIRT)) {
            throw new GameTestAssertException("Expected empty dry farmland to turn to dirt on a random tick, got "
                    + helper.getBlockState(SOIL));
        }
        helper.succeed();
    }
}
