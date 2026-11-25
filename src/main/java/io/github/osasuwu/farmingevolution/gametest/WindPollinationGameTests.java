package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
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
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wind pollination (issue #123, slice 2 of #104; CONCEPT.md, "Опыление"): when a plant ripens it
 * remembers the pollen of the plants with a genome in the 3×3 square around it at its height, of
 * any stage, that the engine crosses with it, and of the wild flax there when it crosses with
 * fiber_flax; each seed of its harvest takes a random one of them as its father, and a plant
 * that remembered none pollinates itself. The analyzer's pollen goes before the wind.
 *
 * <p>Parents are told apart by the strength of the fibre: a seed takes one allele from each
 * parent, moved by at most 1, so the values here lie at least 3 apart.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindPollinationGameTests {
    private static final BlockPos MOTHER = new BlockPos(2, 2, 2);
    private static final String STRENGTH = "fiber_strength";
    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("8e3f1c52-6a0d-4b7e-9c21-d4f05a7b3e18"), "fe-wind");

    private WindPollinationGameTests() {
    }

    /** A plant with no neighbour remembers no pollen, and every seed of it is its own offspring. */
    @GameTest(template = "empty")
    public static void isolatedPlantPollinatesItself(GameTestHelper helper) {
        for (int round = 0; round < 10; round++) {
            plant(helper, MOTHER, flax(3, 3));
            ripen(helper, MOTHER);
            expectNoWindPollen(helper, MOTHER);
            expectEverySeed(harvest(helper, MOTHER), "of a 3|3 plant alone", seed -> bothNear(seed, 3));
        }
        helper.succeed();
    }

    /**
     * Only the square 3×3 at the plant's own height: a plant two blocks away and plants one
     * block higher and one block lower beside it do not pollinate it.
     */
    @GameTest(template = "empty")
    public static void plantsOutsideSquareOrOffItsHeightDoNotPollinate(GameTestHelper helper) {
        plant(helper, new BlockPos(4, 2, 2), flax(9, 9));
        plant(helper, new BlockPos(2, 3, 3), flax(9, 9));
        plant(helper, new BlockPos(1, 1, 2), flax(9, 9));
        for (int round = 0; round < 5; round++) {
            plant(helper, MOTHER, flax(3, 3));
            ripen(helper, MOTHER);
            expectNoWindPollen(helper, MOTHER);
            expectEverySeed(harvest(helper, MOTHER), "of a 3|3 plant with 9|9 plants only out of its square",
                    seed -> bothNear(seed, 3));
        }
        helper.succeed();
    }

    /**
     * One homozygous neighbour, still a sprout when the plant ripens: its allele is in every
     * seed, one allele near 3 from the plant and one near 9 from the neighbour.
     */
    @GameTest(template = "empty")
    public static void homozygousNeighbourPutsItsAlleleIntoEverySeed(GameTestHelper helper) {
        plant(helper, new BlockPos(3, 2, 3), flax(9, 9));
        for (int round = 0; round < 5; round++) {
            plant(helper, MOTHER, flax(3, 3));
            ripen(helper, MOTHER);
            expectEverySeed(harvest(helper, MOTHER), "of a 3|3 plant beside a 9|9 sprout", seed -> pair(seed, 3, 9));
        }
        helper.succeed();
    }

    /**
     * Two neighbours, 9|9 and 1|1: every seed of a 5|5 plant has one of them as its father, and
     * over 40 seeds both are fathers. That one father is never picked has a chance of 2^-39.
     */
    @GameTest(template = "empty")
    public static void twoNeighboursAreBothFathers(GameTestHelper helper) {
        plant(helper, new BlockPos(1, 2, 2), flax(9, 9));
        plant(helper, new BlockPos(3, 2, 2), flax(1, 1));
        List<Individual> seeds = new ArrayList<>();
        for (int round = 0; round < 20; round++) {
            plant(helper, MOTHER, flax(5, 5));
            ripen(helper, MOTHER);
            seeds.addAll(harvest(helper, MOTHER));
        }
        expectEverySeed(seeds, "of a 5|5 plant between a 9|9 and a 1|1", seed -> pair(seed, 5, 9) || pair(seed, 5, 1));
        if (seeds.stream().noneMatch(seed -> pair(seed, 5, 9)) || seeds.stream().noneMatch(seed -> pair(seed, 5, 1))) {
            throw new GameTestAssertException("Expected seeds of both neighbours among " + seeds);
        }
        helper.succeed();
    }

    /**
     * A neighbour whose chromosome set the engine does not pair with the plant is not
     * remembered: a fiber_flax beside a hemp (strength and length of the fibre) pollinates itself,
     * and so does a hemp beside a fiber_flax and a wild flax, since it does not cross with
     * fiber_flax.
     */
    @GameTest(template = "empty")
    public static void incompatibleNeighbourIsNotRemembered(GameTestHelper helper) {
        Individual hemp = FarmingEvolution.BREEDING.wild("hemp", Sex.NONE);
        plant(helper, new BlockPos(3, 2, 2), hemp);
        plant(helper, MOTHER, flax(3, 3));
        ripen(helper, MOTHER);
        expectNoWindPollen(helper, MOTHER);
        expectEverySeed(harvest(helper, MOTHER), "of a 3|3 fiber_flax beside a hemp", seed -> bothNear(seed, 3));

        plant(helper, new BlockPos(3, 2, 2), flax(9, 9));
        wildFlax(helper, new BlockPos(1, 2, 2));
        plant(helper, MOTHER, hemp);
        ripen(helper, MOTHER);
        expectNoWindPollen(helper, MOTHER);
        expectEverySeed(harvest(helper, MOTHER), "of a hemp beside a fiber_flax and a wild flax",
                seed -> seed.species().equals("hemp"));
        helper.succeed();
    }

    /**
     * A wild flax block beside a 9|9 plant pollinates it with the genome of its own seeds, the
     * wild fiber_flax 5|5: every seed has one allele near 9 and one near 5.
     */
    @GameTest(template = "empty")
    public static void wildFlaxNearbyGivesWildAlleles(GameTestHelper helper) {
        BlockPos wild = new BlockPos(3, 2, 2);
        wildFlax(helper, wild);
        Individual ofItsSeeds = Block.getDrops(helper.getBlockState(wild), helper.getLevel(), helper.absolutePos(wild), null)
                .get(0).get(FarmingEvolution.GENOME);
        for (int round = 0; round < 5; round++) {
            plant(helper, MOTHER, flax(9, 9));
            ripen(helper, MOTHER);
            if (!plantAt(helper, MOTHER).windPollen().equals(List.of(ofItsSeeds))) {
                throw new GameTestAssertException("Expected the plant to remember the pollen " + ofItsSeeds
                        + " of the wild flax, it remembered " + plantAt(helper, MOTHER).windPollen());
            }
            expectEverySeed(harvest(helper, MOTHER), "of a 9|9 plant beside a wild flax", seed -> pair(seed, 9, 5));
        }
        helper.succeed();
    }

    /**
     * The analyzer's pollen goes before the wind, even put on after the plant ripened and
     * remembered its 9|9 neighbour: every seed of the 5|5 plant pollinated with 1|1 has an
     * allele near 1 and none near 9.
     */
    @GameTest(template = "empty")
    public static void analyzerPollenGoesBeforeWind(GameTestHelper helper) {
        plant(helper, new BlockPos(3, 2, 2), flax(9, 9));
        FakePlayer breeder = FakePlayerFactory.get(helper.getLevel(), PROFILE);
        for (int round = 0; round < 5; round++) {
            plant(helper, MOTHER, flax(5, 5));
            ripen(helper, MOTHER);
            ItemStack analyzer = new ItemStack(FarmingEvolution.ANALYZER.get());
            analyzer.set(FarmingEvolution.POLLEN, flax(1, 1));
            breeder.setItemInHand(InteractionHand.MAIN_HAND, analyzer);
            breeder.setShiftKeyDown(true);
            BlockPos absolute = helper.absolutePos(MOTHER);
            breeder.gameMode.useItemOn(breeder, helper.getLevel(), analyzer, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
            breeder.setShiftKeyDown(false);
            if (plantAt(helper, MOTHER).pollen().isEmpty()) {
                throw new GameTestAssertException("The analyzer did not pollinate the plant");
            }
            expectEverySeed(harvest(helper, MOTHER), "of a 5|5 plant beside a 9|9, pollinated with 1|1 by the analyzer",
                    seed -> pair(seed, 5, 1));
        }
        helper.succeed();
    }

    /**
     * The fathers are fixed when a plant ripens, not when it is harvested: of two ripe
     * neighbours, 3|3 and 9|9, the one harvested second still has the first as its father,
     * whichever goes first.
     */
    @GameTest(template = "empty")
    public static void harvestOrderDoesNotChangeFathers(GameTestHelper helper) {
        BlockPos other = new BlockPos(3, 2, 2);
        for (boolean motherFirst : List.of(true, false)) {
            plant(helper, MOTHER, flax(3, 3));
            plant(helper, other, flax(9, 9));
            ripen(helper, MOTHER);
            ripen(helper, other);
            List<Individual> ofMother;
            List<Individual> ofOther;
            if (motherFirst) {
                ofMother = harvest(helper, MOTHER);
                ofOther = harvest(helper, other);
            } else {
                ofOther = harvest(helper, other);
                ofMother = harvest(helper, MOTHER);
            }
            String order = motherFirst ? "the 3|3 harvested first" : "the 9|9 harvested first";
            expectEverySeed(ofMother, "of the 3|3, " + order, seed -> pair(seed, 3, 9));
            expectEverySeed(ofOther, "of the 9|9, " + order, seed -> pair(seed, 9, 3));
        }
        helper.succeed();
    }

    /**
     * A plant remembers its neighbour however it ripens: by a player's bone meal and by its own
     * growth on random ticks, not only when its stage is set directly.
     */
    @GameTest(template = "empty")
    public static void ripeningByBoneMealOrGrowthRemembersPollen(GameTestHelper helper) {
        plant(helper, new BlockPos(3, 2, 2), flax(9, 9));

        plant(helper, MOTHER, flax(3, 3));
        FakePlayer breeder = FakePlayerFactory.get(helper.getLevel(), PROFILE);
        BlockPos absolute = helper.absolutePos(MOTHER);
        for (int use = 0; use < 20 && !isRipe(helper, MOTHER); use++) {
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
            breeder.setItemInHand(InteractionHand.MAIN_HAND, boneMeal);
            breeder.gameMode.useItemOn(breeder, helper.getLevel(), boneMeal, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        }
        if (!isRipe(helper, MOTHER)) {
            throw new GameTestAssertException("20 uses of bone meal did not ripen the plant: " + helper.getBlockState(MOTHER));
        }
        expectEverySeed(harvest(helper, MOTHER), "of a 3|3 plant ripened by bone meal beside a 9|9", seed -> pair(seed, 3, 9));

        plant(helper, MOTHER, flax(3, 3));
        for (int tick = 0; tick < 100_000 && !isRipe(helper, MOTHER); tick++) {
            helper.getBlockState(MOTHER).randomTick(helper.getLevel(), absolute, helper.getLevel().random);
        }
        if (!isRipe(helper, MOTHER)) {
            throw new GameTestAssertException("100000 random ticks did not ripen the plant: " + helper.getBlockState(MOTHER));
        }
        expectEverySeed(harvest(helper, MOTHER), "of a 3|3 plant ripened by growth beside a 9|9", seed -> pair(seed, 3, 9));
        helper.succeed();
    }

    private static boolean isRipe(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.is(FarmingEvolution.CROP) && state.getValue(CropBlock.AGE) == CropBlock.MAX_AGE;
    }

    /** A sprout planted beside a plant after it ripened does not pollinate it. */
    @GameTest(template = "empty")
    public static void sproutPlantedAfterRipeningDoesNotPollinate(GameTestHelper helper) {
        BlockPos sprout = new BlockPos(3, 2, 2);
        for (int round = 0; round < 5; round++) {
            helper.setBlock(sprout, Blocks.AIR);
            plant(helper, MOTHER, flax(3, 3));
            ripen(helper, MOTHER);
            plant(helper, sprout, flax(9, 9));
            expectEverySeed(harvest(helper, MOTHER), "of a 3|3 plant that ripened before the 9|9 beside it was planted",
                    seed -> bothNear(seed, 3));
        }
        helper.succeed();
    }

    /**
     * A Create harvester takes the same fathers as a hand: a 3|3 plant that ripened beside a
     * 9|9 sprout, harvested by a harvester on a turning bearing, drops a seed with an allele
     * near 9. The layout is that of PlantGenomeGameTests; the sprout is out of the blades' way.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void createHarvesterTakesSameFathers(GameTestHelper helper) {
        plant(helper, new BlockPos(3, 2, 3), flax(9, 9));
        plant(helper, MOTHER, flax(3, 3));
        ripen(helper, MOTHER);

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
            BlockState state = helper.getBlockState(MOTHER);
            if (!state.is(FarmingEvolution.CROP) || state.getValue(CropBlock.AGE) != 0) {
                throw new GameTestAssertException("The harvester has not harvested the plant yet: " + state);
            }
            List<ItemStack> drops = helper.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MOTHER)).inflate(3))
                    .stream().map(ItemEntity::getItem).toList();
            List<Individual> seeds = genomesOfSeeds(drops);
            if (seeds.isEmpty()) {
                throw new GameTestAssertException("The harvester dropped no seed: " + drops);
            }
            expectEverySeed(seeds, "a harvester dropped from a 3|3 plant beside a 9|9 sprout", seed -> pair(seed, 3, 9));
        });
    }

    /**
     * The remembered pollen outlives saving and loading, and a copy of the plant as an item
     * carries none. A plant saved before wind pollination (no remembered pollen in its save)
     * loads with none and pollinates itself.
     */
    @GameTest(template = "empty")
    public static void windPollenSurvivesSavingAndOldSavePollinatesItself(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        plant(helper, new BlockPos(1, 2, 2), flax(9, 9));
        plant(helper, new BlockPos(3, 2, 2), flax(1, 1));
        plant(helper, MOTHER, flax(5, 5));
        ripen(helper, MOTHER);
        PlantBlockEntity plant = plantAt(helper, MOTHER);
        if (plant.windPollen().size() != 2) {
            throw new GameTestAssertException("Expected the plant to remember its 2 neighbours, got " + plant.windPollen());
        }
        CompoundTag saved = plant.saveWithoutMetadata(registries);
        PlantBlockEntity loaded = new PlantBlockEntity(plant.getBlockPos(), plant.getBlockState());
        loaded.loadWithComponents(saved, registries);
        if (!loaded.windPollen().equals(plant.windPollen())) {
            throw new GameTestAssertException("Saved with the wind pollen " + plant.windPollen() + ", loaded with "
                    + loaded.windPollen());
        }

        // A copy of the plant as an item, as a creative pick of the block makes it, carries no pollen.
        plant.setPollen(flax(1, 1));
        CompoundTag picked = plant.saveCustomAndMetadata(registries);
        plant.removeComponentsFromTag(picked);
        if (picked.contains("wind_pollen") || picked.contains("pollen")) {
            throw new GameTestAssertException("The block data of a picked plant carries pollen: " + picked);
        }
        plant.setPollen(null);

        // What a plant saved before wind pollination holds: its genome alone.
        CompoundTag old = new CompoundTag();
        old.put("genome", saved.get("genome").copy());
        PlantBlockEntity fromOldSave = new PlantBlockEntity(plant.getBlockPos(), plant.getBlockState());
        fromOldSave.loadWithComponents(old, registries);
        if (!fromOldSave.windPollen().isEmpty() || fromOldSave.genome().isEmpty()) {
            throw new GameTestAssertException("A save with only a genome loaded with " + fromOldSave.genome()
                    + " and the wind pollen " + fromOldSave.windPollen());
        }
        List<Individual> seeds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            seeds.addAll(genomesOfSeeds(Block.getDrops(plant.getBlockState(), helper.getLevel(), plant.getBlockPos(), fromOldSave)));
        }
        expectEverySeed(seeds, "of a 5|5 plant loaded from a save without wind pollen", seed -> bothNear(seed, 5));
        helper.succeed();
    }

    /**
     * Selection by what the player sees reaches strong_flax (AC6): one plant alone at a time,
     * from the wild fiber_flax (5); of each generation's seeds the two with the highest expressed
     * strength are sown again. Every seed descends from its plant alone.
     */
    @GameTest(template = "empty")
    public static void selectionByExpressedValueReachesStrongFlax(GameTestHelper helper) {
        List<Individual> sown = List.of(flax(5, 5));
        for (int generation = 1; generation <= 400; generation++) {
            List<Individual> seeds = new ArrayList<>();
            for (Individual parent : sown) {
                plant(helper, MOTHER, parent);
                ripen(helper, MOTHER);
                List<ItemStack> drops = breakBlock(helper, MOTHER);
                List<Individual> offspring = genomesOfSeeds(drops);
                expectEverySeed(offspring, "of " + parent + " alone", seed -> descendsFrom(seed, parent, parent));
                expectProduct(drops, parent);
                if (parent.trait(STRENGTH) >= 7) {
                    if (generation == 1) {
                        throw new GameTestAssertException("The wild flax is strong_flax already: nothing was selected");
                    }
                    helper.succeed();
                    return;
                }
                seeds.addAll(offspring);
            }
            sown = seeds.stream().sorted(Comparator.comparingInt((Individual seed) -> seed.trait(STRENGTH)).reversed())
                    .limit(2).toList();
        }
        throw new GameTestAssertException("400 generations of selection by the expressed value did not reach strong_flax, "
                + "the last sown: " + sown);
    }

    /**
     * Crossing by the wind reaches strong_flax without the analyzer (AC6): two plants side by
     * side from the wild fiber_flax (5) pollinate each other; of each one's seeds the one with
     * the highest expressed strength is sown again in its place. Every seed descends from the
     * pair: one allele near the plant's, one near its neighbour's.
     */
    @GameTest(template = "empty")
    public static void windPairByExpressedValueReachesStrongFlax(GameTestHelper helper) {
        BlockPos other = new BlockPos(3, 2, 2);
        Individual first = flax(5, 5);
        Individual second = flax(5, 5);
        for (int generation = 1; generation <= 400; generation++) {
            plant(helper, MOTHER, first);
            plant(helper, other, second);
            ripen(helper, MOTHER);
            ripen(helper, other);
            if (!plantAt(helper, MOTHER).windPollen().equals(List.of(second))
                    || !plantAt(helper, other).windPollen().equals(List.of(first))) {
                throw new GameTestAssertException("Expected the two plants to remember each other's pollen, they remembered "
                        + plantAt(helper, MOTHER).windPollen() + " and " + plantAt(helper, other).windPollen());
            }
            List<ItemStack> firstDrops = breakBlock(helper, MOTHER);
            List<ItemStack> secondDrops = breakBlock(helper, other);
            Individual mother = first;
            Individual father = second;
            expectEverySeed(genomesOfSeeds(firstDrops), "of " + first + " beside " + second,
                    seed -> descendsFrom(seed, mother, father));
            expectEverySeed(genomesOfSeeds(secondDrops), "of " + second + " beside " + first,
                    seed -> descendsFrom(seed, father, mother));
            expectProduct(firstDrops, first);
            expectProduct(secondDrops, second);
            if (first.trait(STRENGTH) >= 7 || second.trait(STRENGTH) >= 7) {
                if (generation == 1) {
                    throw new GameTestAssertException("The wild flax is strong_flax already: nothing was crossed");
                }
                helper.succeed();
                return;
            }
            first = bestExpressed(genomesOfSeeds(firstDrops));
            second = bestExpressed(genomesOfSeeds(secondDrops));
        }
        throw new GameTestAssertException("400 generations of crossing by the wind did not reach strong_flax, the last pair: "
                + first + " and " + second);
    }

    private static Individual bestExpressed(List<Individual> seeds) {
        return seeds.stream().max(Comparator.comparingInt(seed -> seed.trait(STRENGTH))).orElseThrow();
    }

    private static void expectProduct(List<ItemStack> drops, Individual plant) {
        String expected = plant.trait(STRENGTH) >= 7 ? "farmingevolution:strong_flax x1" : "farmingevolution:fiber_flax_fiber x1";
        List<String> products = drops.stream().filter(stack -> !stack.is(FarmingEvolution.SEEDS))
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()).toList();
        if (!products.equals(List.of(expected))) {
            throw new GameTestAssertException("Expected " + expected + " from " + plant + ", got " + products);
        }
    }

    private static void expectNoWindPollen(GameTestHelper helper, BlockPos pos) {
        List<Individual> remembered = plantAt(helper, pos).windPollen();
        if (!remembered.isEmpty()) {
            throw new GameTestAssertException("Expected the plant at " + pos + " to remember no pollen, it remembered " + remembered);
        }
    }

    /** At least one seed, and each passes the test; the failure names the seeds and what they are. */
    private static void expectEverySeed(List<Individual> seeds, String what, Predicate<Individual> test) {
        if (seeds.isEmpty() || !seeds.stream().allMatch(test)) {
            throw new GameTestAssertException("Unexpected seeds " + what + ": " + seeds);
        }
    }

    /** Both alleles of the strength within 1 of the value: an offspring of a homozygous plant alone. */
    private static boolean bothNear(Individual seed, int value) {
        return strengths(seed).stream().allMatch(allele -> Math.abs(allele - value) <= 1);
    }

    /** One allele within 1 of each value: an offspring of two homozygous parents. */
    private static boolean pair(Individual seed, int mother, int father) {
        List<Integer> alleles = strengths(seed);
        return seed.species().equals("fiber_flax")
                && (near(alleles.get(0), mother) && near(alleles.get(1), father)
                        || near(alleles.get(1), mother) && near(alleles.get(0), father));
    }

    /** One allele within 1 of an allele of each parent. */
    private static boolean descendsFrom(Individual seed, Individual mother, Individual father) {
        List<Integer> alleles = strengths(seed);
        return seed.species().equals(mother.species())
                && (nearAny(alleles.get(0), mother) && nearAny(alleles.get(1), father)
                        || nearAny(alleles.get(1), mother) && nearAny(alleles.get(0), father));
    }

    private static boolean nearAny(int allele, Individual parent) {
        return strengths(parent).stream().anyMatch(own -> near(allele, own));
    }

    private static boolean near(int allele, int value) {
        return Math.abs(allele - value) <= 1;
    }

    private static List<Integer> strengths(Individual individual) {
        Chromosome strength = individual.genome().get(STRENGTH);
        return List.of(((Allele.Numeric) strength.active()).value(), ((Allele.Numeric) strength.inactive()).value());
    }

    private static Individual flax(int active, int inactive) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("fiber_flax", true)),
                STRENGTH, new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive))));
    }

    /** Puts a wild flax on grass at the position. */
    private static void wildFlax(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.getOptional(id("wild_flax"))
                .orElseThrow(() -> new GameTestAssertException("No block wild_flax")));
    }

    /** Plants a seed with the genome on farmland at the position, the way a machine does; a new sprout. */
    private static void plant(GameTestHelper helper, BlockPos pos, Individual genome) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos, Blocks.AIR);
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(pos), Direction.DOWN, seed, Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + pos);
        }
    }

    private static void ripen(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, helper.getBlockState(pos).setValue(CropBlock.AGE, CropBlock.MAX_AGE));
    }

    private static PlantBlockEntity plantAt(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof PlantBlockEntity plant)) {
            throw new GameTestAssertException("No plant at " + pos);
        }
        return plant;
    }

    /** Breaks the ripe plant by hand and returns the genomes of its seeds. */
    private static List<Individual> harvest(GameTestHelper helper, BlockPos pos) {
        return genomesOfSeeds(breakBlock(helper, pos));
    }

    private static List<ItemStack> breakBlock(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        if (!FakePlayerFactory.get(level, PROFILE).gameMode.destroyBlock(helper.absolutePos(pos))
                || !helper.getBlockState(pos).isAir()) {
            throw new GameTestAssertException("The player could not break the block at " + pos);
        }
        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1));
        List<ItemStack> stacks = dropped.stream().map(entity -> entity.getItem().copy()).toList();
        dropped.forEach(ItemEntity::discard);
        return stacks;
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

    private static Block createBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Block " + id + " is not registered: is Create installed?"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path);
    }
}
