package io.github.osasuwu.farmingevolution.plant;

import com.mojang.logging.LogUtils;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.CrossOdds;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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
    private static final String WIND_POLLEN_TAG = "wind_pollen";

    @Nullable
    private Individual genome;

    /** Saved with the plant but not a component: a dropped seed never carries it. */
    @Nullable
    private Individual pollen;

    /** Saved with the plant, never sent to the client nor put on a seed; see {@link #windPollen}. */
    private List<Individual> windPollen = List.of();

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
        if (level != null && !level.isClientSide()) {
            // The client shows the genome through the breeder's goggles: send the change.
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The genome of the pollen the analyzer put on this plant; empty when it put none. The wind's is {@link #windPollen}. */
    public Optional<Individual> pollen() {
        return Optional.ofNullable(pollen);
    }

    public void setPollen(@Nullable Individual pollen) {
        this.pollen = pollen;
        setChanged();
    }

    /**
     * The pollen the wind brought, remembered when the plant ripened (CONCEPT.md, "Опыление"):
     * one genome per neighbour, in no particular order. Empty for a plant that ripened alone,
     * has not ripened yet, or was saved before wind pollination existed.
     */
    public List<Individual> windPollen() {
        return windPollen;
    }

    /**
     * Wind pollination, called when the plant ripens: remembers the pollen of the plants with
     * a genome in the square 3×3 around it at its own height, of any stage, and of the wild flax
     * there, each when the engine crosses it with this plant. What was remembered before is
     * forgotten. A neighbour in a chunk that is not loaded is not asked, so that ripening never
     * loads a chunk.
     */
    void rememberWindPollen(Level level) {
        List<Individual> remembered = new ArrayList<>();
        if (genome != null) {
            Environment environment = environment(level);
            for (BlockPos pos : BlockPos.betweenClosed(getBlockPos().offset(-1, 0, -1), getBlockPos().offset(1, 0, 1))) {
                if (pos.equals(getBlockPos()) || !level.isLoaded(pos)) {
                    continue;
                }
                Individual neighbour = null;
                if (level.getBlockState(pos).getBlock() instanceof WildFlaxBlock) {
                    neighbour = WildFlaxBlock.genome();
                } else if (level.getBlockEntity(pos) instanceof PlantBlockEntity plant) {
                    neighbour = plant.genome;
                }
                if (neighbour != null && Parents.of(genome, neighbour, environment).crosses()) {
                    remembered.add(neighbour);
                }
            }
        }
        windPollen = List.copyOf(remembered);
        setChanged();
    }

    /**
     * The pair whose offspring the seeds of this plant are by the analyzer, empty for a plant
     * without a genome: the plant first, then the pollen the analyzer put on it, or the plant
     * itself when it has none or when the engine cannot cross the two (pack data changed since
     * the pollination). The analyzer shows its odds. The wind is not in it: a plant the wind
     * pollinated has other fathers at the harvest ({@link #harvestPairs}), the known
     * discrepancy of CONCEPT.md, "Анализатор".
     */
    public Optional<Parents> parents(LevelReader level) {
        if (genome == null) {
            return Optional.empty();
        }
        Environment environment = environment(level);
        return Optional.of(crossable(pollen, "pollen", environment).orElseGet(() -> Parents.of(genome, genome, environment)));
    }

    /**
     * The pairs the seeds of a harvest are offspring of, empty for a plant without a genome;
     * each seed is one cross of one of them, taken at random. The pollen the analyzer put on
     * the plant goes before the wind: then it is the only pair. Otherwise there is a pair with
     * each remembered {@link #windPollen}, and with nothing remembered the plant pollinates
     * itself. Pollen the engine no longer crosses with the plant (pack data changed since) is
     * left out with a line in the log.
     */
    public List<Parents> harvestPairs(LevelReader level) {
        if (genome == null) {
            return List.of();
        }
        Environment environment = environment(level);
        Optional<Parents> byAnalyzer = crossable(pollen, "pollen", environment);
        if (byAnalyzer.isPresent()) {
            return List.of(byAnalyzer.get());
        }
        List<Parents> byWind = windPollen.stream()
                .flatMap(father -> crossable(father, "wind pollen", environment).stream())
                .toList();
        return byWind.isEmpty() ? List.of(Parents.of(genome, genome, environment)) : byWind;
    }

    /** The plant and the pollen as a pair, empty when there is no pollen or the engine cannot cross them. */
    private Optional<Parents> crossable(@Nullable Individual father, String what, Environment environment) {
        if (father == null) {
            return Optional.empty();
        }
        Parents pair = Parents.of(genome, father, environment);
        try {
            pair.odds();
            return Optional.of(pair);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("The {} {} of the plant at {} cannot cross with it and is left out: {}",
                    what, father, getBlockPos(), e.getMessage());
            return Optional.empty();
        }
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

        /** Whether the engine crosses the two at all: it refuses a pair whose chromosome sets or sexes do not match. */
        public boolean crosses() {
            try {
                odds();
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
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
        if (!windPollen.isEmpty()) {
            ListTag list = new ListTag();
            windPollen.forEach(father -> list.add(GenomeCodec.INDIVIDUAL.encodeStart(NbtOps.INSTANCE, father).getOrThrow()));
            tag.put(WIND_POLLEN_TAG, list);
        }
    }

    /**
     * A plant saved before wind pollination has no {@value #WIND_POLLEN_TAG}: it loads with no
     * remembered pollen and pollinates itself unless the analyzer pollinates it. An unreadable
     * entry is left out with a line in the log, as an unreadable genome is.
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        genome = read(tag.get(GENOME_TAG), "genome");
        pollen = read(tag.get(POLLEN_TAG), "pollen");
        List<Individual> remembered = new ArrayList<>();
        for (Tag father : tag.getList(WIND_POLLEN_TAG, Tag.TAG_COMPOUND)) {
            Individual read = read(father, "wind pollen");
            if (read != null) {
                remembered.add(read);
            }
        }
        windPollen = List.copyOf(remembered);
    }

    @Nullable
    private Individual read(@Nullable Tag saved, String what) {
        if (saved == null) {
            return null;
        }
        return GenomeCodec.saved(FarmingEvolution.BREEDING).parse(NbtOps.INSTANCE, saved)
                .resultOrPartial(error -> LOGGER.error("Unreadable {} of the plant at {}: {}", what, getBlockPos(), error))
                .orElse(null);
    }

    /**
     * What the client learns of the plant: its genome, for the breeder's goggles (issue #90).
     * The pollen, the analyzer's and the wind's, stays on the server, where the harvest and the
     * analyzer read it. The client reads the tag
     * by {@link #loadAdditional}. A plant in play never loses its genome, so the update never
     * has to clear one: NeoForge skips an empty update packet, and a genome removed by a
     * command would stay on the client until its chunk loads again.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (genome != null) {
            tag.put(GENOME_TAG, GenomeCodec.INDIVIDUAL.encodeStart(NbtOps.INSTANCE, genome).getOrThrow());
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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

    /**
     * What a copy of the plant as an item (a creative pick of the block) does not carry: the
     * genome, which goes as a component, and the pollen of both kinds, which never goes onto a
     * seed.
     */
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove(GENOME_TAG);
        tag.remove(POLLEN_TAG);
        tag.remove(WIND_POLLEN_TAG);
    }
}
