package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where the genome of a plant and of a seed is kept, and that it stays there: through a
 * harvest by a Create harvester, through a save, and no longer than the plant itself.
 * Create is reached through registries, so the mod does not compile against it.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantGenomeGameTests {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos PLANT = SOIL.above();

    private PlantGenomeGameTests() {
    }

    /**
     * A creative motor turns a mechanical bearing; the bearing carries a block with a
     * harvester on its side, and the harvester sweeps through a ripe plant grown from a
     * seed with a genome. The harvester takes the drops, removes the block and puts a
     * young plant in its place: that plant must have the genome of the harvested one, and
     * so must the seed the harvester drops.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void harvesterReplantsPlantWithItsGenome(GameTestHelper helper) {
        Individual genome = genome("fiber_flax", 7, 4);
        plantSeed(helper, genome);
        ripen(helper);
        if (!plantAt(helper).genome().equals(Optional.of(genome))) {
            throw new GameTestAssertException("The planted seed gave the plant the genome " + plantAt(helper).genome());
        }

        BlockPos motor = new BlockPos(1, 0, 1);
        BlockPos bearing = motor.above();
        BlockPos anchor = bearing.above();
        helper.setBlock(motor, createBlock("creative_motor").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(bearing, createBlock("mechanical_bearing").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(anchor, Blocks.OAK_PLANKS);
        // Mounted on the east side of the anchor, one block north of the plant: any turn of the
        // bearing takes its blades through the plant.
        helper.setBlock(anchor.east(), createBlock("mechanical_harvester").defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));

        helper.succeedWhen(() -> {
            BlockState state = helper.getBlockState(PLANT);
            if (!state.is(FarmingEvolution.CROP)) {
                throw new GameTestAssertException("In place of the plant there is " + state);
            }
            if (state.getValue(CropBlock.AGE) != 0) {
                throw new GameTestAssertException("The harvester has not harvested the plant yet");
            }
            if (!plantAt(helper).genome().equals(Optional.of(genome))) {
                throw new GameTestAssertException("The replanted plant has the genome " + plantAt(helper).genome()
                        + ", the harvested one had " + genome);
            }

            // The loot of a ripe plant is two seeds and one product; the harvester keeps one seed.
            // Which item the product is, is not checked here.
            List<ItemStack> drops = helper.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PLANT)).inflate(3))
                    .stream().map(ItemEntity::getItem).toList();
            List<ItemStack> seeds = drops.stream().filter(stack -> stack.is(FarmingEvolution.SEEDS)).toList();
            if (seeds.size() != 1 || seeds.get(0).getCount() != 1
                    || !genome.equals(seeds.get(0).get(FarmingEvolution.GENOME))) {
                throw new GameTestAssertException("Expected 1 dropped seed with the genome of the plant, got " + drops);
            }
            int products = drops.stream().filter(stack -> !stack.is(FarmingEvolution.SEEDS)).mapToInt(ItemStack::getCount).sum();
            if (products != 1) {
                throw new GameTestAssertException("Expected 1 dropped item that is not a seed, got " + drops);
            }
        });
    }

    /**
     * The genome of a removed plant is handed only to a plant put in its place within the
     * same tick, which is how a harvester replants. A plant placed there later is a new one.
     */
    @GameTest(template = "empty")
    public static void plantPlacedLaterDoesNotInheritRemovedGenome(GameTestHelper helper) {
        plantSeed(helper, genome("wild_hemp", 3, 3));
        helper.getLevel().destroyBlock(helper.absolutePos(PLANT), false);
        helper.runAfterDelay(1, () -> {
            helper.setBlock(PLANT, FarmingEvolution.CROP.get());
            if (plantAt(helper).genome().isPresent()) {
                throw new GameTestAssertException("A plant placed a tick after another was removed got its genome "
                        + plantAt(helper).genome());
            }
            helper.succeed();
        });
    }

    /**
     * A player breaks a plant and plants a seed in its place within one tick. The new plant
     * grows from that seed, so it has the genome of the seed and not of the broken plant.
     */
    @GameTest(template = "empty")
    public static void seedPlantedByPlayerKeepsItsGenomeWherePlantWasJustRemoved(GameTestHelper helper) {
        Individual ofSeed = genome("fiber_flax", 8, 1);
        plantSeed(helper, genome("wild_hemp", 3, 3));
        helper.getLevel().destroyBlock(helper.absolutePos(PLANT), false);
        plantByPlayer(helper, seedOf(ofSeed));
        if (!plantAt(helper).genome().equals(Optional.of(ofSeed))) {
            throw new GameTestAssertException("A seed with " + ofSeed + " planted by a player gave the plant "
                    + plantAt(helper).genome());
        }
        helper.succeed();
    }

    /** The same with a seed that has no genome: the plant has none, not the one of the broken plant. */
    @GameTest(template = "empty")
    public static void seedWithoutGenomePlantedByPlayerDoesNotInheritRemovedGenome(GameTestHelper helper) {
        plantSeed(helper, genome("wild_hemp", 3, 3));
        helper.getLevel().destroyBlock(helper.absolutePos(PLANT), false);
        plantByPlayer(helper, new ItemStack(FarmingEvolution.SEEDS.get()));
        if (plantAt(helper).genome().isPresent()) {
            throw new GameTestAssertException("A seed without a genome planted by a player gave the plant "
                    + plantAt(helper).genome());
        }
        helper.succeed();
    }

    /** What a world save holds: the genome of a plant in its block entity, of a seed on its stack. */
    @GameTest(template = "empty")
    public static void genomeSurvivesSavingPlantAndSeed(GameTestHelper helper) {
        Individual genome = new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, new Chromosome(new Allele.Named("fiber_flax", true), new Allele.Named("wild_flax", false)),
                "fiber_strength", new Chromosome(new Allele.Numeric(9), new Allele.Numeric(2))));
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        plantSeed(helper, genome);

        PlantBlockEntity saved = plantAt(helper);
        CompoundTag plantTag = saved.saveWithoutMetadata(registries);
        PlantBlockEntity loaded = new PlantBlockEntity(saved.getBlockPos(), saved.getBlockState());
        loaded.loadWithComponents(plantTag, registries);
        if (!loaded.genome().equals(Optional.of(genome))) {
            throw new GameTestAssertException("A plant saved with " + genome + " loaded with " + loaded.genome());
        }

        Tag seedTag = seedOf(genome).save(registries);
        Individual onLoadedSeed = ItemStack.parse(registries, seedTag)
                .orElseThrow(() -> new GameTestAssertException("The saved seed does not load: " + seedTag))
                .get(FarmingEvolution.GENOME);
        if (!genome.equals(onLoadedSeed)) {
            throw new GameTestAssertException("A seed saved with " + genome + " loaded with " + onLoadedSeed);
        }
        helper.succeed();
    }

    /** A homozygous species and a heterozygous trait: neither is what an empty genome would give. */
    private static Individual genome(String species, int activeStrength, int inactiveStrength) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named(species, true)),
                "fiber_strength", new Chromosome(new Allele.Numeric(activeStrength), new Allele.Numeric(inactiveStrength))));
    }

    private static ItemStack seedOf(Individual genome) {
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        return seed;
    }

    /** Plants the way a player does: the seed in the hand is used on the top of the soil. */
    private static void plantByPlayer(GameTestHelper helper, ItemStack seed) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, seed);
        BlockPos soil = helper.absolutePos(SOIL);
        BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false);
        if (!seed.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)).consumesAction()) {
            throw new GameTestAssertException("The player could not plant the seed on farmland");
        }
    }

    /** Plants the way a machine does: the seed item places its block, with no player involved. */
    private static void plantSeed(GameTestHelper helper, Individual genome) {
        helper.setBlock(SOIL, Blocks.FARMLAND);
        ItemStack seed = seedOf(genome);
        boolean planted = FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(PLANT), Direction.DOWN, seed, Direction.UP)).consumesAction();
        if (!planted) {
            throw new GameTestAssertException("The seed could not be planted on farmland");
        }
    }

    private static void ripen(GameTestHelper helper) {
        helper.setBlock(PLANT, helper.getBlockState(PLANT).setValue(CropBlock.AGE, CropBlock.MAX_AGE));
    }

    private static PlantBlockEntity plantAt(GameTestHelper helper) {
        if (helper.getBlockEntity(PLANT) instanceof PlantBlockEntity plant) {
            return plant;
        }
        throw new GameTestAssertException("No plant block entity at " + PLANT);
    }

    private static Block createBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Block " + id + " is not registered: is Create installed?"));
    }
}
