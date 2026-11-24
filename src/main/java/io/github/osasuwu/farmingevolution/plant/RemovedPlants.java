package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

/**
 * The genomes of the plants removed from one level during the current tick, by position.
 *
 * <p>A harvester that replants does not change the plant in place: it takes the drops,
 * replaces the block with air and then sets a young plant there, all in one tick. The block
 * entity dies with the first step, so the genome waits here for the second. Nothing is kept
 * past the tick and nothing is saved: a plant that is not replaced at once is gone.
 */
public final class RemovedPlants {
    private final Map<BlockPos, Individual> genomes = new HashMap<>();
    private long tick = Long.MIN_VALUE;

    public void remember(long gameTime, BlockPos pos, Individual genome) {
        if (tick != gameTime) {
            genomes.clear();
            tick = gameTime;
        }
        genomes.put(pos.immutable(), genome);
    }

    /** The genome of the plant removed from this position during this tick; it is handed out once. */
    @Nullable
    public Individual recall(long gameTime, BlockPos pos) {
        return tick == gameTime ? genomes.remove(pos) : null;
    }
}
