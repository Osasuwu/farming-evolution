package io.github.osasuwu.farmingevolution.plant;

import com.mojang.logging.LogUtils;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.CrossOdds;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.Optional;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.LevelReader;
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
    private static final String POLLEN_TAG = "pollen";

    @Nullable
    private Individual genome;

    /** Saved with the plant but not a component: a dropped seed never carries it. */
    @Nullable
    private Individual pollen;

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

    /** The genome of the pollen this plant was pollinated with; empty for a self-pollinating plant. */
    public Optional<Individual> pollen() {
        return Optional.ofNullable(pollen);
    }

    public void setPollen(@Nullable Individual pollen) {
        this.pollen = pollen;
        setChanged();
    }

    /**
     * The pair whose offspring the seeds of this plant are, empty for a plant without a genome.
     * The harvest rolls it and the analyzer shows its odds, so both always speak of the same
     * cross: the plant first, then its pollen, or the plant itself when it has none or when
     * the engine cannot cross the two (pack data changed since the pollination).
     */
    public Optional<Parents> parents(LevelReader level) {
        if (genome == null) {
            return Optional.empty();
        }
        Environment environment = environment(level);
        if (pollen != null) {
            Parents pair = Parents.of(genome, pollen, environment);
            try {
                pair.odds();
                return Optional.of(pair);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("The pollen of the plant at {} cannot cross with it, the plant pollinates itself: {}",
                        getBlockPos(), e.getMessage());
            }
        }
        return Optional.of(Parents.of(genome, genome, environment));
    }

    /**
     * The conditions any cross of this plant happens under: the climate of its place in the
     * level. The level is given, not read from the block entity, because a harvest may ask for
     * the drops of a block entity that is not placed in a level.
     */
    public Environment environment(LevelReader level) {
        return PlantBlock.environmentAt(level, getBlockPos());
    }

    /**
     * The two parents of a cross, in the order the engine takes them, and the conditions it
     * happens under. The odds and the roll both read all three from here.
     */
    public record Parents(Individual first, Individual second, Environment environment) {
        /**
         * The pair of two genomes brought up to the current pack data. A genome set by a
         * command, or carried by an item, has not been through the reading of a save; see
         * Products.of.
         */
        public static Parents of(Individual first, Individual second, Environment environment) {
            Breeding breeding = FarmingEvolution.BREEDING;
            return new Parents(breeding.restore(first, replaced -> { }), breeding.restore(second, replaced -> { }),
                    environment);
        }

        public CrossOdds odds() {
            return FarmingEvolution.BREEDING.odds(first, second, environment);
        }

        public Individual cross(RandomGenerator random) {
            return FarmingEvolution.BREEDING.cross(first, second, environment, random);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (genome != null) {
            tag.put(GENOME_TAG, GenomeCodec.INDIVIDUAL.encodeStart(NbtOps.INSTANCE, genome).getOrThrow());
        }
        if (pollen != null) {
            tag.put(POLLEN_TAG, GenomeCodec.INDIVIDUAL.encodeStart(NbtOps.INSTANCE, pollen).getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        genome = read(tag, GENOME_TAG, "genome");
        pollen = read(tag, POLLEN_TAG, "pollen");
    }

    @Nullable
    private Individual read(CompoundTag tag, String key, String what) {
        if (!tag.contains(key)) {
            return null;
        }
        return GenomeCodec.saved(FarmingEvolution.BREEDING).parse(NbtOps.INSTANCE, tag.get(key))
                .resultOrPartial(error -> LOGGER.error("Unreadable {} of the plant at {}: {}", what, getBlockPos(), error))
                .orElse(null);
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
