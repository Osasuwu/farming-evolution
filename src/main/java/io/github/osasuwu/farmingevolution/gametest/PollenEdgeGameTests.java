package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Edge cases of pollen found in the review of #104 (issue #113; CONCEPT.md, "Хранение генома"):
 * a plant on the border of a chunk that is not loaded, and the pollen of a plant a Create
 * harvester replants.
 *
 * <p>Parents are told apart by the strength of the fibre: a seed takes one allele from each
 * parent, moved by at most 1, so the values here lie at least 3 apart.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PollenEdgeGameTests {
    private static final String STRENGTH = "fiber_strength";

    /** Holds a chunk loaded, and only it: ticket level 33 makes a chunk full but not its neighbours. */
    private static final TicketType<ChunkPos> HOLD = TicketType.create("farmingevolution_test_hold",
            Comparator.comparingLong(ChunkPos::toLong));

    /**
     * Far from the GameTest grid, so that no test keeps these chunks loaded, and a whole number
     * of climate bands away from the origin (8000 blocks north to south, 6000 west to east;
     * CONCEPT.md, "Мир и климат"): the climate of the grid, where no hybridization holds.
     */
    private static final ChunkPos MOTHER_CHUNK = new ChunkPos(3000, 3000);

    private PollenEdgeGameTests() {
    }

    /**
     * A plant in the south-east corner of its chunk ripens and is harvested while the chunk
     * diagonal to it, which holds a 9|9 neighbour, is not loaded: neither loads that chunk,
     * the neighbour is not remembered and every seed of the 3|3 plant is its own offspring. The
     * chunks east and south are loaded, so that the game's own updates of the faces of the
     * block stay out of the unloaded chunk and only the plant's look at its square reaches it.
     * Once the chunk is loaded again, the same neighbour is remembered: it was there all along.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void neighbourInUnloadedChunkIsNotAskedAndNotLoaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos east = new ChunkPos(MOTHER_CHUNK.x + 1, MOTHER_CHUNK.z);
        ChunkPos south = new ChunkPos(MOTHER_CHUNK.x, MOTHER_CHUNK.z + 1);
        ChunkPos diagonal = new ChunkPos(MOTHER_CHUNK.x + 1, MOTHER_CHUNK.z + 1);
        List<ChunkPos> held = List.of(MOTHER_CHUNK, east, south, diagonal);
        BlockPos mother = new BlockPos(MOTHER_CHUNK.getMaxBlockX(), -50, MOTHER_CHUNK.getMaxBlockZ());
        BlockPos neighbour = mother.offset(1, 0, 1);
        if (!new ChunkPos(neighbour).equals(diagonal)) {
            throw new GameTestAssertException("The neighbour " + neighbour + " is not in the chunk " + diagonal);
        }
        Individual father = flax(9, 9);

        held.forEach(chunk -> hold(level, chunk));
        plant(level, neighbour, father);
        level.getChunkSource().removeRegionTicket(HOLD, diagonal, 0, diagonal);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (level.getChunkSource().hasChunk(diagonal.x, diagonal.z)) {
                        throw new GameTestAssertException("The chunk " + diagonal + " is still loaded");
                    }
                })
                .thenExecute(() -> {
                    try {
                        ripenAndHarvestBesideUnloadedChunk(level, mother, diagonal, father);
                    } finally {
                        level.setBlock(mother, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        level.setBlock(neighbour, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        held.forEach(chunk -> level.getChunkSource().removeRegionTicket(HOLD, chunk, 0, chunk));
                    }
                })
                .thenSucceed();
    }

    private static void ripenAndHarvestBesideUnloadedChunk(ServerLevel level, BlockPos mother, ChunkPos diagonal,
            Individual father) {
        plant(level, mother, flax(3, 3));
        ripen(level, mother);
        expectNotLoaded(level, diagonal, "the plant ripened");
        List<Individual> remembered = plantAt(level, mother).windPollen();
        if (!remembered.isEmpty()) {
            throw new GameTestAssertException("The plant remembered the pollen " + remembered
                    + " beside a chunk that is not loaded");
        }
        List<Individual> seeds = harvest(level, mother);
        expectNotLoaded(level, diagonal, "the plant was harvested");
        if (seeds.isEmpty() || !seeds.stream().allMatch(seed -> pair(seed, 3, 3))) {
            throw new GameTestAssertException("Expected only offspring of the 3|3 plant alone, got " + seeds);
        }

        // The same neighbour, once its chunk is loaded, is remembered.
        hold(level, diagonal);
        plant(level, mother, flax(3, 3));
        ripen(level, mother);
        if (!plantAt(level, mother).windPollen().equals(List.of(father))) {
            throw new GameTestAssertException("With its chunk loaded, expected the plant to remember the pollen "
                    + father + ", it remembered " + plantAt(level, mother).windPollen());
        }
    }

    /**
     * Pollen lives one harvest (CONCEPT.md, "Хранение генома"): a 5|5 plant pollinated with 1|1
     * by the analyzer, which also remembered a 9|9 neighbour when it ripened, is harvested by a
     * Create harvester. The seeds are offspring of the analyzer's pollen; the replanted plant
     * keeps the genome and carries neither the analyzer's pollen nor the wind's. The layout is
     * that of PlantGenomeGameTests; the neighbour is out of the blades' way.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void harvesterReplantsWithoutPollen(GameTestHelper helper) {
        BlockPos mother = new BlockPos(2, 2, 2);
        Individual genome = flax(5, 5);
        Individual windFather = flax(9, 9);
        plant(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 3)), windFather);
        plant(helper.getLevel(), helper.absolutePos(mother), genome);
        ripen(helper.getLevel(), helper.absolutePos(mother));
        PlantBlockEntity ripe = plantAt(helper.getLevel(), helper.absolutePos(mother));
        ripe.setPollen(flax(1, 1));
        if (!ripe.windPollen().equals(List.of(windFather))) {
            throw new GameTestAssertException("Expected the ripe plant to remember the pollen " + windFather
                    + ", it remembered " + ripe.windPollen());
        }

        BlockPos motor = new BlockPos(1, 0, 1);
        BlockPos bearing = motor.above();
        BlockPos anchor = bearing.above();
        helper.setBlock(motor, createBlock("creative_motor").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(bearing, createBlock("mechanical_bearing").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(anchor, Blocks.OAK_PLANKS);
        helper.setBlock(anchor.east(), createBlock("mechanical_harvester").defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));

        helper.succeedWhen(() -> {
            BlockState state = helper.getBlockState(mother);
            if (!state.is(FarmingEvolution.CROP) || state.getValue(CropBlock.AGE) != 0) {
                throw new GameTestAssertException("The harvester has not harvested the plant yet: " + state);
            }
            List<Individual> seeds = genomesOfSeeds(helper.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(mother)).inflate(3))
                    .stream().map(ItemEntity::getItem).toList());
            if (seeds.isEmpty() || !seeds.stream().allMatch(seed -> pair(seed, 5, 1))) {
                throw new GameTestAssertException("Expected the harvester to drop offspring of the 5|5 plant and the "
                        + "analyzer's 1|1 pollen, got " + seeds);
            }
            PlantBlockEntity replanted = plantAt(helper.getLevel(), helper.absolutePos(mother));
            if (!replanted.genome().equals(Optional.of(genome))) {
                throw new GameTestAssertException("The replanted plant has the genome " + replanted.genome()
                        + ", the harvested one had " + genome);
            }
            if (replanted.pollen().isPresent() || !replanted.windPollen().isEmpty()) {
                throw new GameTestAssertException("The replanted plant kept the analyzer's pollen " + replanted.pollen()
                        + " and the wind's " + replanted.windPollen());
            }
        });
    }

    /** Loads the chunk and keeps it loaded, its neighbours not. */
    private static void hold(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addRegionTicket(HOLD, chunk, 0, chunk);
        level.getChunk(chunk.x, chunk.z);
    }

    private static void expectNotLoaded(ServerLevel level, ChunkPos chunk, String after) {
        if (level.getChunkSource().hasChunk(chunk.x, chunk.z)) {
            throw new GameTestAssertException("The chunk " + chunk + " was loaded after " + after);
        }
    }

    /** Plants a seed with the genome on farmland at the absolute position, the way a machine does; a new sprout. */
    private static void plant(ServerLevel level, BlockPos pos, Individual genome) {
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(level, pos, Direction.DOWN, seed, Direction.UP))
                .consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + pos);
        }
    }

    private static void ripen(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(CropBlock.AGE, CropBlock.MAX_AGE), Block.UPDATE_ALL);
    }

    private static PlantBlockEntity plantAt(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PlantBlockEntity plant)) {
            throw new GameTestAssertException("No plant at " + pos);
        }
        return plant;
    }

    /** Breaks the plant with its drops and returns the genomes of its seeds. */
    private static List<Individual> harvest(ServerLevel level, BlockPos pos) {
        if (!level.destroyBlock(pos, true)) {
            throw new GameTestAssertException("The plant at " + pos + " could not be broken");
        }
        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        List<Individual> seeds = genomesOfSeeds(dropped.stream().map(ItemEntity::getItem).toList());
        dropped.forEach(ItemEntity::discard);
        return seeds;
    }

    private static List<Individual> genomesOfSeeds(List<ItemStack> stacks) {
        List<Individual> genomes = new ArrayList<>();
        for (ItemStack stack : stacks) {
            for (int i = 0; stack.is(FarmingEvolution.SEEDS) && i < stack.getCount(); i++) {
                Individual genome = stack.get(FarmingEvolution.GENOME);
                if (genome == null) {
                    throw new GameTestAssertException("A seed without a genome among " + stacks);
                }
                genomes.add(genome);
            }
        }
        return genomes;
    }

    /** One allele of the strength within 1 of each value: an offspring of two homozygous parents. */
    private static boolean pair(Individual seed, int mother, int father) {
        Chromosome strength = seed.genome().get(STRENGTH);
        int active = ((Allele.Numeric) strength.active()).value();
        int inactive = ((Allele.Numeric) strength.inactive()).value();
        return seed.species().equals("fiber_flax")
                && (near(active, mother) && near(inactive, father) || near(inactive, mother) && near(active, father));
    }

    private static boolean near(int allele, int value) {
        return Math.abs(allele - value) <= 1;
    }

    private static Individual flax(int active, int inactive) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("fiber_flax", true)),
                STRENGTH, new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive))));
    }

    private static Block createBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Block " + id + " is not registered: is Create installed?"));
    }
}
