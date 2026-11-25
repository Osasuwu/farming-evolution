package io.github.osasuwu.farmingevolution.plant;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;

/**
 * How fast a plant of the mod grows (issue #87). The pace is set by one number, the mean minutes
 * from seed to ripe plant on watered farmland at the default random tick speed
 * ({@link io.github.osasuwu.farmingevolution.Config#GENERATION_MINUTES}).
 *
 * <p>A block gets a random tick on average every 4096 / 3 game ticks (a section of 16³ blocks,
 * 3 random ticks per game tick), so a generation of {@code m} minutes is
 * {@code m · 1200 / (4096 / 3)} random ticks on average, and each random tick grows the plant by
 * {@code 7 / that} stages on average: the whole part always, one stage more with the chance of
 * the fraction. A faster random tick speed makes the plant grow faster, as it does wheat.
 */
public final class GrowthPace {
    /** How many times longer a plant takes on dry farmland than on watered farmland. */
    public static final double DRY_SLOWDOWN = 1.5;

    /** Mean game ticks between two random ticks of one block at the default random tick speed. */
    private static final double GAME_TICKS_PER_RANDOM_TICK = 4096.0 / 3;

    private static final double GAME_TICKS_PER_MINUTE = 20 * 60;

    private GrowthPace() {
    }

    /** Mean growth stages per random tick for a generation of the given minutes. */
    static double stagesPerRandomTick(double generationMinutes, boolean watered) {
        double randomTicks = generationMinutes * GAME_TICKS_PER_MINUTE / GAME_TICKS_PER_RANDOM_TICK;
        double stages = CropBlock.MAX_AGE / randomTicks;
        return watered ? stages : stages / DRY_SLOWDOWN;
    }

    /** The stages one random tick grows: the whole part of the mean, and one more by chance. */
    static int stages(double stagesPerRandomTick, RandomSource random) {
        int whole = (int) stagesPerRandomTick;
        return whole + (random.nextDouble() < stagesPerRandomTick - whole ? 1 : 0);
    }
}
