package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The pace of growth of a plant of the mod (issue #87): from seed to ripe plant in 8 to 10
 * minutes on average on watered farmland at the default random tick speed, slower on dry
 * farmland but at most twice as slow; bone meal still makes it grow.
 *
 * <p>A plant is grown from age 0 to ripe by its own random ticks, the way the world grows it,
 * many times over; the mean number of random ticks is turned into minutes with the mean time
 * between two random ticks of one block at the default speed. A lamp next to the plant gives it
 * the light crops need, whatever the time of day.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GrowthPaceGameTests {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos PLANT = SOIL.above();
    private static final BlockPos LAMP = PLANT.west();

    /** Plants grown per mean; the mean of 400 is within a few seconds of the true one. */
    private static final int TRIALS = 400;

    /** More random ticks than any pace of the issue needs for one generation. */
    private static final int TICK_LIMIT = 10_000;

    /**
     * Mean game ticks between two random ticks of one block at the default random tick speed:
     * a section of 16 × 16 × 16 = 4096 blocks gets 3 random ticks every game tick.
     */
    private static final double GAME_TICKS_PER_RANDOM_TICK = 4096.0 / 3;

    private static final double GAME_TICKS_PER_MINUTE = 20 * 60;

    private GrowthPaceGameTests() {
    }

    @GameTest(template = "empty")
    public static void plantOnWateredFarmlandRipensInEightToTenMinutesOnAverage(GameTestHelper helper) {
        double minutes = meanMinutesToRipe(helper, 7);
        if (minutes < 8 || minutes > 10) {
            throw new GameTestAssertException("Expected a plant on watered farmland to ripen in 8 to 10 minutes on"
                    + " average, got " + minutes + " minutes over " + TRIALS + " plants");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plantOnDryFarmlandRipensSlowerButAtMostTwiceAsSlow(GameTestHelper helper) {
        double wet = meanMinutesToRipe(helper, 7);
        double dry = meanMinutesToRipe(helper, 0);
        double ratio = dry / wet;
        // "Заметно дольше": a quarter longer at least, so the difference is seen in a field.
        if (ratio < 1.25 || ratio > 2) {
            throw new GameTestAssertException("Expected a plant on dry farmland to ripen 1.25 to 2 times slower than"
                    + " on watered farmland, got " + dry + " against " + wet + " minutes (" + ratio + " times)");
        }
        helper.succeed();
    }

    /** Bone meal on a seedling grows it by two to five stages at once, as it does wheat. */
    @GameTest(template = "empty")
    public static void boneMealStillGrowsPlant(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.setBlock(LAMP, Blocks.GLOWSTONE);
        helper.setBlock(PLANT, FarmingEvolution.CROP.get().defaultBlockState());
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
        if (!BoneMealItem.growCrop(boneMeal, helper.getLevel(), helper.absolutePos(PLANT))) {
            throw new GameTestAssertException("Bone meal was not used on the plant");
        }
        int age = helper.getBlockState(PLANT).getValue(CropBlock.AGE);
        if (age < 2 || age > 5 || !boneMeal.isEmpty()) {
            throw new GameTestAssertException("Expected bone meal to grow the plant from age 0 to 2..5 and be used up,"
                    + " got age " + age + " and " + boneMeal);
        }
        helper.succeed();
    }

    /** Mean minutes from age 0 to ripe of a plant on farmland of the given moisture. */
    private static double meanMinutesToRipe(GameTestHelper helper, int moisture) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, moisture));
        helper.setBlock(LAMP, Blocks.GLOWSTONE);
        long randomTicks = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            helper.setBlock(PLANT, FarmingEvolution.CROP.get().defaultBlockState());
            int ticks = 0;
            while (helper.getBlockState(PLANT).getValue(CropBlock.AGE) < CropBlock.MAX_AGE && ticks < TICK_LIMIT) {
                helper.randomTick(PLANT);
                ticks++;
            }
            if (helper.getBlockState(PLANT).getValue(CropBlock.AGE) != CropBlock.MAX_AGE) {
                throw new GameTestAssertException("The plant did not ripen in " + TICK_LIMIT + " random ticks: "
                        + helper.getBlockState(PLANT));
            }
            if (!helper.getBlockState(SOIL).is(Blocks.FARMLAND)
                    || helper.getBlockState(SOIL).getValue(FarmBlock.MOISTURE) != moisture) {
                throw new GameTestAssertException("The farmland under the plant changed: " + helper.getBlockState(SOIL));
            }
            randomTicks += ticks;
        }
        return (double) randomTicks / TRIALS * GAME_TICKS_PER_RANDOM_TICK / GAME_TICKS_PER_MINUTE;
    }
}
