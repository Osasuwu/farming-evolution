package io.github.osasuwu.farmingevolution.plant;

import com.mojang.logging.LogUtils;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Where the genome of a growing plant lives. The genome is exposed as an implicit data
 * component, so the game itself carries it between the plant and the seed item: a placed
 * seed hands its component to this block entity, and the loot table copies it back onto
 * the dropped seeds.
 */
public final class PlantBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GENOME_TAG = "genome";

    @Nullable
    private Individual genome;

    /** Not saved: only tells {@link PlantBlock#onPlace} apart from a plant put without a seed. */
    private boolean plantedFromSeed;

    public PlantBlockEntity(BlockPos pos, BlockState state) {
        super(FarmingEvolution.PLANT_BLOCK_ENTITY.get(), pos, state);
    }

    /** Empty for a plant that was put into the world without a seed, e.g. by a command. */
    public Optional<Individual> genome() {
        return Optional.ofNullable(genome);
    }

    /**
     * True once a seed item has given this plant its components, with or without a genome.
     * Such a plant is a new one and does not continue a plant removed from its place.
     */
    boolean plantedFromSeed() {
        return plantedFromSeed;
    }

    public void setGenome(@Nullable Individual genome) {
        this.genome = genome;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (genome != null) {
            tag.put(GENOME_TAG, GenomeCodec.INDIVIDUAL.encodeStart(NbtOps.INSTANCE, genome).getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        genome = null;
        if (tag.contains(GENOME_TAG)) {
            genome = GenomeCodec.INDIVIDUAL.parse(NbtOps.INSTANCE, tag.get(GENOME_TAG))
                    .resultOrPartial(error -> LOGGER.error("Unreadable genome of the plant at {}: {}", getBlockPos(), error))
                    .orElse(null);
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        genome = input.get(FarmingEvolution.GENOME);
        plantedFromSeed = true;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (genome != null) {
            components.set(FarmingEvolution.GENOME, genome);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(GENOME_TAG);
    }
}
