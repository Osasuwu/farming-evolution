package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A growing plant with a genome. The block is an ordinary crop, which is what harvesting
 * machines of other mods look for; the genome is in its block entity.
 */
public final class PlantBlock extends CropBlock implements EntityBlock {
    public PlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return FarmingEvolution.SEEDS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlantBlockEntity(pos, state);
    }

    /** The plant is being replaced by another block: its genome is kept until the end of the tick. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PlantBlockEntity plant) {
            plant.genome().ifPresent(genome ->
                    level.getData(FarmingEvolution.REMOVED_PLANTS).remember(level.getGameTime(), pos, genome));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * A plant put where another was removed during this tick continues it (see
     * {@link RemovedPlants}), unless it grows from a seed: a seed starts a new plant. The game
     * gives the seed's components to the block entity either before this call (a player's
     * hand) or after it (an item placing its block directly); both orders end with the genome
     * of the seed.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this)) {
            return;
        }
        Individual genome = level.getData(FarmingEvolution.REMOVED_PLANTS).recall(level.getGameTime(), pos);
        if (genome != null && level.getBlockEntity(pos) instanceof PlantBlockEntity plant && !plant.plantedFromSeed()) {
            plant.setGenome(genome);
        }
    }
}
