package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

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

    /**
     * The harvest. Seeds come from the loot table; the product is added here, because which
     * item it is follows from the genome: a ripe plant gives one product of its variety, or of
     * its species when it fits no variety. Every way of harvesting asks for the drops here, a
     * hand and a machine alike. A plant without a genome has no species and gives no product.
     *
     * <p>The seeds of a ripe plant are its offspring: each seed is one cross of the plant's
     * {@link PlantBlockEntity#parents() parents}, the plant with itself (selection) or with the
     * pollen the analyzer put on it (crossing), in the climate of the place where the plant
     * grew, so its traits may move off the parents'. An unripe plant gives back the seed it
     * grew from unchanged.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> loot = super.getDrops(state, params);
        if (!isMaxAge(state) || !(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PlantBlockEntity plant)
                || plant.genome().isEmpty()) {
            return loot;
        }
        Individual genome = plant.genome().get();
        PlantBlockEntity.Parents parents = plant.parents().orElseThrow();
        Random random = new Random(params.getLevel().random.nextLong());
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack stack : loot) {
            if (!stack.is(FarmingEvolution.SEEDS)) {
                drops.add(stack);
                continue;
            }
            for (int i = 0; i < stack.getCount(); i++) {
                ItemStack seed = stack.copyWithCount(1);
                seed.set(FarmingEvolution.GENOME, parents.cross(random));
                drops.add(seed);
            }
        }
        drops.add(new ItemStack(FarmingEvolution.PRODUCTS.of(genome)));
        return drops;
    }

    /**
     * The conditions of a cross of plants growing at the position: the harvest rolls under them
     * and the analyzer shows the odds under them. The climate of the place decides which
     * hybridizations of pack data may happen. There is no soil model yet, so a hybridization that
     * needs a soil does not happen.
     */
    static Environment environmentAt(LevelReader level, BlockPos pos) {
        return new Environment(FarmingEvolution.CLIMATE.at(level, pos).readings(), null);
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
