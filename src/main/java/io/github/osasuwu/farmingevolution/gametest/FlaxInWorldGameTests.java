package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Flax in the world (issue #25): wild flax is found, gives seeds of the wild fiber_flax of the
 * mod's pack data (strength of the fibre 5), the seeds grow, a ripe plant gives a fibre and
 * seeds that are its offspring, and keeping the best of them reaches the variety strong_flax
 * (strength not below 7). The seed and wild berries are food.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FlaxInWorldGameTests {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos PLANT = SOIL.above();
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos WILD = GROUND.above();
    private static final String STRENGTH = "fiber_strength";
    private static final GameProfile PLAYER =
            new GameProfile(UUID.fromString("5f0c6d0e-24a1-4c3e-9d0b-6f2f8f3d2501"), "fe-forager");
    private static final Individual WILD_FIBER_FLAX = flax(5, 5);

    private FlaxInWorldGameTests() {
    }

    @GameTest(template = "empty")
    public static void brokenWildFlaxGivesTwoSeedsOfWildFiberFlax(GameTestHelper helper) {
        List<ItemStack> drops = breakWildFlax(helper);
        List<Individual> genomes = genomesOfSeeds(drops);
        if (!genomes.equals(List.of(WILD_FIBER_FLAX, WILD_FIBER_FLAX)) || drops.stream().anyMatch(s -> !isSeed(s))) {
            throw new GameTestAssertException("Expected 2 seeds of the wild fiber_flax from a wild flax, got " + drops);
        }
        helper.succeed();
    }

    /** The seed a wild flax gave is planted, grows up under random ticks and is harvested by hand. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void seedOfWildFlaxGrowsAndRipePlantGivesFiberAndSeeds(GameTestHelper helper) {
        ItemStack seed = breakWildFlax(helper).get(0);
        helper.setBlock(WILD, Blocks.AIR);
        helper.setBlock(SOIL, Blocks.FARMLAND);
        // Crops grow in light; the lamp makes the test independent of the time of day.
        helper.setBlock(PLANT.west(), Blocks.GLOWSTONE);
        plant(helper, seed);
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 20000 && helper.getBlockState(PLANT).getValue(CropBlock.AGE) < CropBlock.MAX_AGE; i++) {
                helper.randomTick(PLANT);
            }
            if (helper.getBlockState(PLANT).getValue(CropBlock.AGE) != CropBlock.MAX_AGE) {
                throw new GameTestAssertException("The plant did not ripen: " + helper.getBlockState(PLANT));
            }
            List<ItemStack> drops = breakBlock(helper, PLANT);
            List<String> products = drops.stream().filter(s -> !isSeed(s)).map(FlaxInWorldGameTests::text).toList();
            List<Individual> seeds = genomesOfSeeds(drops);
            if (!products.equals(List.of("farmingevolution:fiber_flax_fiber x1")) || seeds.size() != 2
                    || seeds.stream().anyMatch(s -> !s.species().equals("fiber_flax"))) {
                throw new GameTestAssertException(
                        "Expected the fibre of fiber_flax and 2 seeds of fiber_flax from the ripe plant, got " + drops);
            }
            helper.succeed();
        });
    }

    /** An unripe plant is not a parent yet: breaking it gives back the seed it grew from. */
    @GameTest(template = "empty")
    public static void unripePlantGivesBackItsOwnSeed(GameTestHelper helper) {
        Individual genome = flax(7, 4);
        plant(helper, seedOf(genome));
        List<ItemStack> drops = breakBlock(helper, PLANT);
        if (drops.size() != 1 || !genomesOfSeeds(drops).equals(List.of(genome))) {
            throw new GameTestAssertException("Expected the one seed the unripe plant grew from, got " + drops);
        }
        helper.succeed();
    }

    /**
     * Selection without crossing: the two seeds of a ripe plant are its offspring, each allele
     * of the strength a parent's allele moved by at most 1. Sowing the two strongest seeds of
     * every generation leads from the wild 5 to a plant that gives the product of strong_flax.
     */
    @GameTest(template = "empty")
    public static void sowingStrongestSeedsOfEachHarvestReachesStrongFlax(GameTestHelper helper) {
        List<Individual> sown = List.of(WILD_FIBER_FLAX);
        for (int generation = 1; generation <= 400; generation++) {
            List<Individual> seeds = new ArrayList<>();
            for (Individual parent : sown) {
                plant(helper, seedOf(parent));
                helper.setBlock(PLANT, helper.getBlockState(PLANT).setValue(CropBlock.AGE, CropBlock.MAX_AGE));
                List<ItemStack> drops = breakBlock(helper, PLANT);
                List<String> products = drops.stream().filter(s -> !isSeed(s)).map(FlaxInWorldGameTests::text).toList();
                List<Individual> offspring = genomesOfSeeds(drops);
                if (offspring.size() != 2 || offspring.stream().anyMatch(child -> !descendsFrom(child, parent))) {
                    throw new GameTestAssertException("Expected 2 seeds with the strength of " + parent
                            + " moved by at most 1, got " + offspring);
                }
                boolean strong = parent.trait(STRENGTH) >= 7;
                String expected = strong ? "farmingevolution:strong_flax x1" : "farmingevolution:fiber_flax_fiber x1";
                if (!products.equals(List.of(expected))) {
                    throw new GameTestAssertException("Expected " + expected + " from " + parent + ", got " + products);
                }
                if (strong) {
                    if (generation == 1) {
                        throw new GameTestAssertException("The wild flax is strong_flax already: nothing was selected");
                    }
                    helper.succeed();
                    return;
                }
                seeds.addAll(offspring);
            }
            seeds.sort(Comparator.comparingInt(FlaxInWorldGameTests::strengthOfBothAlleles).reversed());
            sown = List.copyOf(seeds.subList(0, 2));
        }
        throw new GameTestAssertException("400 generations of selection did not reach strong_flax, the last sown: " + sown);
    }

    @GameTest(template = "empty")
    public static void seedAndWildBerriesRestoreHunger(GameTestHelper helper) {
        expectEating(helper, FarmingEvolution.SEEDS.get(), 1);
        expectEating(helper, BuiltInRegistries.ITEM.getOptional(id("wild_berries"))
                .orElseThrow(() -> new GameTestAssertException("No item wild_berries")), 1);
        helper.succeed();
    }

    /** A ripe bush gives two berries to an empty hand, an empty one gives nothing and ripens again. */
    @GameTest(template = "empty")
    public static void wildBerryBushGivesBerriesToHandAndRipensAgain(GameTestHelper helper) {
        Block bush = block("wild_berry_bush");
        helper.setBlock(GROUND, Blocks.GRASS_BLOCK);
        helper.setBlock(WILD, bush.defaultBlockState().setValue(BlockStateProperties.AGE_1, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(WILD, player);
        List<String> picked = takeDrops(helper, WILD).stream().map(FlaxInWorldGameTests::text).toList();
        if (!picked.equals(List.of("farmingevolution:wild_berries x2"))
                || !helper.getBlockState(WILD).equals(bush.defaultBlockState().setValue(BlockStateProperties.AGE_1, 0))) {
            throw new GameTestAssertException("Expected 2 berries and an empty bush, got " + picked
                    + " and " + helper.getBlockState(WILD));
        }
        helper.useBlock(WILD, player);
        if (!takeDrops(helper, WILD).isEmpty()) {
            throw new GameTestAssertException("An empty bush gave berries");
        }
        for (int i = 0; i < 20000 && helper.getBlockState(WILD).getValue(BlockStateProperties.AGE_1) == 0; i++) {
            helper.randomTick(WILD);
        }
        if (helper.getBlockState(WILD).getValue(BlockStateProperties.AGE_1) != 1) {
            throw new GameTestAssertException("The bush did not ripen again");
        }
        helper.succeed();
    }

    /**
     * Wild flax does not spread onto the player's tilled field: one plant on grass among
     * farmland stays the only one.
     */
    @GameTest(template = "empty")
    public static void wildFlaxDoesNotSpreadOntoFarmland(GameTestHelper helper) {
        Block wildFlax = block("wild_flax");
        BlockPos.betweenClosed(0, 1, 0, 2, 1, 2).forEach(pos -> helper.setBlock(pos, Blocks.FARMLAND));
        helper.setBlock(WILD.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(WILD, wildFlax);
        for (int i = 0; i < 20000; i++) {
            helper.randomTick(WILD);
        }
        List<BlockPos> plants = positionsOf(helper, wildFlax, new BlockPos(-1, 0, -1), new BlockPos(3, 3, 3));
        if (plants.size() != 1) {
            throw new GameTestAssertException("Expected wild flax to stay off the farmland, got " + plants);
        }
        helper.setBlock(WILD, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Wild flax does not spread (#122, AC9): one plant in the middle of a lawn of grass stays
     * the only one however long it ticks; the way to get it again is generation in new chunks.
     */
    @GameTest(template = "empty")
    public static void wildFlaxOnGrassPlacesNoNeighbours(GameTestHelper helper) {
        Block wildFlax = block("wild_flax");
        BlockPos.betweenClosed(0, 1, 0, 4, 1, 4).forEach(pos -> helper.setBlock(pos, Blocks.GRASS_BLOCK));
        BlockPos plant = new BlockPos(2, 2, 2);
        helper.setBlock(plant, wildFlax);
        for (int i = 0; i < 20000; i++) {
            helper.randomTick(plant);
        }
        List<BlockPos> plants = positionsOf(helper, wildFlax, new BlockPos(0, 1, 0), new BlockPos(4, 3, 4));
        if (!plants.equals(List.of(plant))) {
            throw new GameTestAssertException("Expected the wild flax to stay the only one on the lawn, got " + plants);
        }
        helper.setBlock(plant, Blocks.AIR);
        helper.succeed();
    }

    /**
     * World generation: every biome of the overworld has the patches of wild flax and of wild
     * berry bushes among its features, and a patch put on a lawn plants its block there.
     */
    @GameTest(template = "field")
    public static void overworldBiomesGenerateWildFlaxAndBerryBushes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryAccess registries = level.registryAccess();
        HolderSet.Named<Biome> overworld = registries.registryOrThrow(Registries.BIOME).getTag(BiomeTags.IS_OVERWORLD)
                .orElseThrow(() -> new GameTestAssertException("No biome tag is_overworld"));
        if (overworld.size() == 0) {
            throw new GameTestAssertException("No overworld biomes");
        }
        Map<String, BlockState> patches = Map.of(
                "wild_flax_patch", block("wild_flax").defaultBlockState(),
                "wild_berry_bush_patch", block("wild_berry_bush").defaultBlockState().setValue(BlockStateProperties.AGE_1, 1));
        BlockPos.betweenClosed(0, 0, 0, 12, 0, 12).forEach(pos -> helper.setBlock(pos, Blocks.GRASS_BLOCK));
        patches.forEach((name, state) -> {
            PlacedFeature placed = registries.registryOrThrow(Registries.PLACED_FEATURE).getOptional(id(name))
                    .orElseThrow(() -> new GameTestAssertException("No placed feature " + name));
            List<String> without = overworld.stream().filter(biome -> !biome.value().getGenerationSettings().hasFeature(placed))
                    .map(Holder::getRegisteredName).toList();
            if (!without.isEmpty()) {
                throw new GameTestAssertException("Biomes without " + name + ": " + without);
            }
            ConfiguredFeature<?, ?> patch = placed.feature().value();
            patch.place(level, level.getChunkSource().getGenerator(), RandomSource.create(25), helper.absolutePos(new BlockPos(6, 1, 6)));
            List<BlockPos> plants = positionsOf(helper, state.getBlock(), new BlockPos(0, 0, 0), new BlockPos(12, 3, 12));
            if (plants.isEmpty() || plants.stream().anyMatch(pos -> pos.getY() != 1 || !helper.getBlockState(pos).equals(state))) {
                throw new GameTestAssertException("Expected " + state + " on the lawn from " + name + ", got it at " + plants);
            }
            plants.forEach(pos -> helper.setBlock(pos, Blocks.AIR));
        });
        helper.succeed();
    }

    /** A fiber_flax with the two alleles of the strength of the fibre. */
    private static Individual flax(int active, int inactive) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("fiber_flax", true)),
                STRENGTH, new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive))));
    }

    private static List<Integer> strengths(Individual individual) {
        Chromosome strength = individual.genome().get(STRENGTH);
        return List.of(((Allele.Numeric) strength.active()).value(), ((Allele.Numeric) strength.inactive()).value());
    }

    private static int strengthOfBothAlleles(Individual individual) {
        return strengths(individual).stream().mapToInt(Integer::intValue).sum();
    }

    /** Same species, and each allele of the strength is within 1 of an allele of the parent. */
    private static boolean descendsFrom(Individual child, Individual parent) {
        return child.species().equals(parent.species()) && child.genome().keySet().equals(parent.genome().keySet())
                && strengths(child).stream().allMatch(allele ->
                        strengths(parent).stream().anyMatch(own -> Math.abs(own - allele) <= 1));
    }

    private static void expectEating(GameTestHelper helper, Item food, int nutrition) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        ItemStack left = new ItemStack(food).finishUsingItem(helper.getLevel(), player);
        if (player.getFoodData().getFoodLevel() != 10 + nutrition || !left.isEmpty()) {
            throw new GameTestAssertException("Eating " + food + " at hunger 10 left hunger "
                    + player.getFoodData().getFoodLevel() + " and " + left + ", expected " + (10 + nutrition) + " and nothing");
        }
    }

    /** Puts a wild flax on grass and breaks it the way a player in survival does; returns the drops. */
    private static List<ItemStack> breakWildFlax(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.GRASS_BLOCK);
        helper.setBlock(WILD, block("wild_flax"));
        return breakBlock(helper, WILD);
    }

    private static List<ItemStack> breakBlock(GameTestHelper helper, BlockPos pos) {
        if (!FakePlayerFactory.get(helper.getLevel(), PLAYER).gameMode.destroyBlock(helper.absolutePos(pos))
                || !helper.getBlockState(pos).isAir()) {
            throw new GameTestAssertException("The player could not break the block at " + pos);
        }
        return takeDrops(helper, pos);
    }

    /** The items lying around the position, removed from the world. */
    private static List<ItemStack> takeDrops(GameTestHelper helper, BlockPos pos) {
        List<ItemEntity> dropped = helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3));
        List<ItemStack> stacks = dropped.stream().map(item -> item.getItem().copy()).toList();
        dropped.forEach(ItemEntity::discard);
        return stacks;
    }

    private static boolean isSeed(ItemStack stack) {
        return stack.is(FarmingEvolution.SEEDS);
    }

    /** The genome of every seed among the stacks, one entry per item. */
    private static List<Individual> genomesOfSeeds(List<ItemStack> stacks) {
        List<Individual> genomes = new ArrayList<>();
        for (ItemStack stack : stacks) {
            for (int i = 0; isSeed(stack) && i < stack.getCount(); i++) {
                Individual genome = stack.get(FarmingEvolution.GENOME);
                if (genome == null) {
                    throw new GameTestAssertException("A seed without a genome among " + stacks);
                }
                genomes.add(genome);
            }
        }
        return genomes;
    }

    private static ItemStack seedOf(Individual genome) {
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        return seed;
    }

    private static void plant(GameTestHelper helper, ItemStack seed) {
        helper.setBlock(SOIL, Blocks.FARMLAND);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(PLANT), Direction.DOWN, seed, Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted on farmland");
        }
    }

    private static List<BlockPos> positionsOf(GameTestHelper helper, Block block, BlockPos from, BlockPos to) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.betweenClosed(from, to).forEach(pos -> {
            if (helper.getBlockState(pos).is(block)) {
                positions.add(pos.immutable());
            }
        });
        return positions;
    }

    private static String text(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount();
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(id(path))
                .orElseThrow(() -> new GameTestAssertException("No block " + path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path);
    }
}
