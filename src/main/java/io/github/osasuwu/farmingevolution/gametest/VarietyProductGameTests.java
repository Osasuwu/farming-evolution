package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.GenomeCodec;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * What a harvest gives (CONCEPT.md, "Селекция → Сорт"): one product item, of the narrowest
 * variety the plant fits or of its species when it fits none. wild_flax is of the mod's pack
 * data (src/main/resources/farmingevolution/pack.json) and has no variety. hemp is test data
 * of the build (src/gametest/pack.json), added only in the GameTest run, so a player never
 * sees it: it has a strength and a length of the fibre and the varieties strong_hemp
 * (strength not below 7), long_hemp (length not below 7) and select_hemp (both).
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VarietyProductGameTests {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos PLANT = SOIL.above();
    private static final GameProfile HARVESTING_PLAYER =
            new GameProfile(UUID.fromString("5f0c6d0e-24a1-4c3e-9d0b-6f2f8f3d2401"), "fe-harvest");

    private VarietyProductGameTests() {
    }

    @GameTest(template = "empty")
    public static void ripePlantFittingVarietyGivesItsProductToHand(GameTestHelper helper) {
        Individual longHemp = hemp(5, 8);
        plantSeed(helper, longHemp);
        expect("an unripe plant with strength 5 and length 8", breakByHand(helper), List.of());
        expect("a ripe plant with strength 5 and length 8", harvestByHand(helper, longHemp),
                List.of("farmingevolution:long_hemp x1"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plantFittingNestedVarietiesGivesProductOfNarrowest(GameTestHelper helper) {
        // Fits strong_hemp, long_hemp and select_hemp; the region of select_hemp lies inside the other two.
        expect("a plant with strength 8 and length 9", harvestByHand(helper, hemp(8, 9)),
                List.of("farmingevolution:select_hemp x1"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plantFittingNoVarietyOfItsSpeciesGivesProductOfSpecies(GameTestHelper helper) {
        // The unexpressed alleles would fit select_hemp: only the expressed value counts.
        Individual plainHemp = new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("hemp", false)),
                "fiber_strength", new Chromosome(new Allele.Numeric(6), new Allele.Numeric(10)),
                "fiber_length", new Chromosome(new Allele.Numeric(6), new Allele.Numeric(10))));
        expect("a hemp with strength 6 and length 6", harvestByHand(helper, plainHemp),
                List.of("farmingevolution:hemp_fiber x1"));
        // Strength 9 fits strong_flax and strong_hemp, but those are varieties of other species.
        Individual wildFlax = new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("wild_flax", false)),
                "fiber_strength", Chromosome.pure(new Allele.Numeric(9))));
        expect("a wild flax with strength 9", harvestByHand(helper, wildFlax),
                List.of("farmingevolution:wild_flax_fiber x1"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void varietyBoundedOnTwoTraitsNeedsBoth(GameTestHelper helper) {
        expect("a plant with strength 7 and length 7", harvestByHand(helper, hemp(7, 7)),
                List.of("farmingevolution:select_hemp x1"));
        expect("a plant with strength 7 and length 6", harvestByHand(helper, hemp(7, 6)),
                List.of("farmingevolution:strong_hemp x1"));
        expect("a plant with strength 6 and length 7", harvestByHand(helper, hemp(6, 7)),
                List.of("farmingevolution:long_hemp x1"));
        helper.succeed();
    }

    /**
     * A genome put on a seed by code never went through the reading of a save. This one has no
     * length of the fibre, which hemp has: the harvest treats it as a save would be, wild
     * hemp has length 3, and strength 8 with length 3 is strong_hemp.
     */
    @GameTest(template = "empty")
    public static void plantWithGenomePackDataDoesNotDescribeGivesProductOfRestoredGenome(GameTestHelper helper) {
        Individual withoutLength = new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("hemp", false)),
                "fiber_strength", new Chromosome(new Allele.Numeric(8), new Allele.Numeric(1))));
        expect("a hemp with strength 8 and no length", harvestByHand(helper, withoutLength),
                List.of("farmingevolution:strong_hemp x1"));
        helper.succeed();
    }

    /**
     * The harvester of Create sweeps through a ripe plant (the machine of
     * {@link PlantGenomeGameTests#harvesterReplantsPlantWithItsGenome}). What it drops besides
     * the seed is what a hand got from a plant with the same genome.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void harvesterGivesSameProductAsHand(GameTestHelper helper) {
        Individual genome = hemp(8, 9);
        List<String> byHand = harvestByHand(helper, genome);
        expect("a hand harvesting a plant with strength 8 and length 9", byHand, List.of("farmingevolution:select_hemp x1"));

        plantSeed(helper, genome);
        ripen(helper);
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
            BlockState state = helper.getBlockState(PLANT);
            if (!state.is(FarmingEvolution.CROP) || state.getValue(CropBlock.AGE) != 0) {
                throw new GameTestAssertException("The harvester has not harvested the plant yet: " + state);
            }
            expect("the harvester harvesting a plant with the same genome", products(helper, false), byHand);
        });
    }

    /**
     * A save written when pack data still had the species nettle and let the length of a
     * fibre reach 12. Both are gone from pack data: the plant and the seed load with the wild
     * type in their place, and the log says what was replaced.
     */
    @GameTest(template = "empty")
    public static void savedGenomeWithAllelesRemovedFromPackDataLoadsWithWildTypeAndLogsIt(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        Individual saved = hemp(8, 9);
        Individual restored = new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("hemp", false)),
                "fiber_strength", new Chromosome(new Allele.Numeric(8), new Allele.Numeric(1)),
                // Wild hemp has length 3.
                "fiber_length", new Chromosome(new Allele.Numeric(3), new Allele.Numeric(1))));
        List<String> replacements = List.of(
                "Genome from a save: species: unknown species nettle, replaced with hemp",
                "Genome from a save: fiber_length: 12 is outside 1..10, replaced with 3 of wild hemp");
        plantSeed(helper, saved);

        PlantBlockEntity plant = plantAt(helper);
        CompoundTag plantTag = plant.saveWithoutMetadata(registries);
        outdate(plantTag.getCompound("genome"));
        PlantBlockEntity loaded = new PlantBlockEntity(plant.getBlockPos(), plant.getBlockState());
        List<String> log = logOf(() -> loaded.loadWithComponents(plantTag, registries));
        if (!loaded.genome().equals(Optional.of(restored))) {
            throw new GameTestAssertException("The plant loaded with " + loaded.genome() + ", expected " + restored);
        }
        expect("the log of loading the plant", log, replacements);

        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, saved);
        CompoundTag seedTag = (CompoundTag) seed.save(registries);
        outdate(seedTag.getCompound("components").getCompound("farmingevolution:genome"));
        List<ItemStack> loadedSeed = new ArrayList<>();
        log = logOf(() -> ItemStack.parse(registries, seedTag).ifPresent(loadedSeed::add));
        if (loadedSeed.size() != 1 || !restored.equals(loadedSeed.get(0).get(FarmingEvolution.GENOME))) {
            throw new GameTestAssertException("The seed loaded as " + loadedSeed + ", expected the genome " + restored);
        }
        expect("the log of loading the seed", log, replacements);
        helper.succeed();
    }

    /** A plant set by a command or by world generation has no genome, so no species and no product. */
    @GameTest(template = "empty")
    public static void ripePlantWithoutGenomeGivesOnlySeeds(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND);
        helper.setBlock(PLANT, FarmingEvolution.CROP.get().defaultBlockState());
        ripen(helper);
        breakPlant(helper);
        List<String> dropped = helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PLANT)).inflate(3)).stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item.getItem().getItem()) + " x" + item.getItem().getCount())
                .toList();
        expect("a ripe plant without a genome", dropped,
                List.of("farmingevolution:seeds x1", "farmingevolution:seeds x1"));
        helper.succeed();
    }

    /**
     * Two plants with different genomes that give the same product: the two items are one
     * stack, because nothing of the plant is written on them.
     */
    @GameTest(template = "empty")
    public static void productsCarryNoDataAndStack(GameTestHelper helper) {
        Individual[][] pairs = {{hemp(7, 2), hemp(10, 5)}, {hemp(2, 3), hemp(6, 1)}};
        String[] items = {"strong_hemp", "hemp_fiber"};
        for (int i = 0; i < pairs.length; i++) {
            Individual[] pair = pairs[i];
            plantSeed(helper, pair[0]);
            ripen(helper);
            breakPlant(helper);
            ItemStack first = productStacks(helper, true).get(0);
            plantSeed(helper, pair[1]);
            ripen(helper);
            breakPlant(helper);
            ItemStack second = productStacks(helper, true).get(0);
            if (!BuiltInRegistries.ITEM.getKey(first.getItem()).getPath().equals(items[i])) {
                throw new GameTestAssertException("Expected the product " + items[i] + ", got " + first);
            }
            if (!first.getComponentsPatch().isEmpty() || !second.getComponentsPatch().isEmpty()) {
                throw new GameTestAssertException("A product carries data: " + first.getComponentsPatch()
                        + " and " + second.getComponentsPatch());
            }
            if (!ItemStack.isSameItemSameComponents(first, second) || first.getMaxStackSize() != 64) {
                throw new GameTestAssertException(first + " and " + second + " do not make one stack of up to 64");
            }
        }
        helper.succeed();
    }

    /** A hemp that expresses the given strength and length and carries 1 as the other allele of both. */
    private static Individual hemp(int strength, int length) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("hemp", false)),
                "fiber_strength", new Chromosome(new Allele.Numeric(strength), new Allele.Numeric(1)),
                "fiber_length", new Chromosome(new Allele.Numeric(length), new Allele.Numeric(1))));
    }

    /** Turns a saved genome of {@link #hemp} into one of older pack data: a species and a length it no longer has. */
    private static void outdate(CompoundTag genome) {
        CompoundTag chromosomes = genome.getCompound("chromosomes");
        if (!chromosomes.contains(Breeding.SPECIES) || !chromosomes.contains("fiber_length")) {
            throw new GameTestAssertException("The saved genome is not written the way this test expects: " + genome);
        }
        chromosomes.getCompound(Breeding.SPECIES).getCompound("inactive").putString("id", "nettle");
        chromosomes.getCompound("fiber_length").put("active", IntTag.valueOf(12));
    }

    /** What {@link GenomeCodec} writes to the log while the action runs. */
    private static List<String> logOf(Runnable action) {
        List<String> lines = new ArrayList<>();
        Logger logger = (Logger) LogManager.getLogger(GenomeCodec.class);
        AbstractAppender appender = new AbstractAppender("variety-product-game-tests", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                lines.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return lines;
    }

    /** Plants a seed with the genome, lets the plant ripen and harvests it by hand; returns the products. */
    private static List<String> harvestByHand(GameTestHelper helper, Individual genome) {
        plantSeed(helper, genome);
        ripen(helper);
        return breakByHand(helper);
    }

    private static List<String> breakByHand(GameTestHelper helper) {
        breakPlant(helper);
        return products(helper, true);
    }

    /** Breaks the plant the way a player in survival does, with an empty hand. */
    private static void breakPlant(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), HARVESTING_PLAYER);
        if (!player.gameMode.destroyBlock(helper.absolutePos(PLANT)) || !helper.getBlockState(PLANT).isAir()) {
            throw new GameTestAssertException("The player could not break the plant");
        }
    }

    /** The dropped items around the plant that are not seeds, as "id xcount". */
    private static List<String> products(GameTestHelper helper, boolean remove) {
        return productStacks(helper, remove).stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()).toList();
    }

    private static List<ItemStack> productStacks(GameTestHelper helper, boolean remove) {
        List<ItemEntity> dropped = helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PLANT)).inflate(3));
        List<ItemStack> products = dropped.stream().map(ItemEntity::getItem)
                .filter(stack -> !stack.is(FarmingEvolution.SEEDS)).map(ItemStack::copy).toList();
        if (remove) {
            dropped.forEach(ItemEntity::discard);
        }
        return products;
    }

    private static void expect(String what, List<String> got, List<String> expected) {
        if (!got.equals(expected)) {
            throw new GameTestAssertException("Expected " + expected + " from " + what + ", got " + got);
        }
    }

    private static void plantSeed(GameTestHelper helper, Individual genome) {
        helper.setBlock(SOIL, Blocks.FARMLAND);
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
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
