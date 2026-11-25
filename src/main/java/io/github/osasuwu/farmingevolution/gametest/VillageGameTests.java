package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.Products;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Villages and the mod's crops (issue #88): villagers leave the plants of the mod alone, the
 * composter takes the mod's seeds, wild berries and products, and the farmer has a trade that
 * gives bone meal and none with food.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageGameTests {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos PLANT = SOIL.above();

    private VillageGameTests() {
    }

    /**
     * A farmer standing at a ripe plant of the mod runs the farming behaviour of vanilla
     * villagers ({@link HarvestFarmland}), the one its brain runs at work. Without the mod's rule
     * it breaks the ripe plant and its harvest falls on the ground; with it the plant stays,
     * ripe, with its genome, and nothing is dropped.
     */
    @GameTest(template = "empty")
    public static void farmerLeavesRipePlantOfTheMod(GameTestHelper helper) {
        Individual genome = ripePlant(helper);
        Villager farmer = helper.spawn(EntityType.VILLAGER, PLANT);
        farmer.setNoAi(true);
        farmer.setVillagerData(farmer.getVillagerData().setProfession(VillagerProfession.FARMER));
        ServerLevel level = helper.getLevel();
        farmer.getBrain().setMemory(MemoryModuleType.SECONDARY_JOB_SITE,
                List.of(GlobalPos.of(level.dimension(), helper.absolutePos(SOIL))));

        HarvestFarmland harvest = new HarvestFarmland();
        long time = level.getGameTime();
        if (harvest.tryStart(level, farmer, time)) {
            for (int tick = 1; tick <= 20; tick++) {
                harvest.tickOrStop(level, farmer, time + tick);
            }
        }
        assertPlantStays(helper, genome);
        helper.succeed();
    }

    /**
     * A villager lands on the farmland under a ripe plant of the mod after a fall of five blocks,
     * long enough for vanilla to trample the farmland every time: the landing is the call the
     * game makes when a falling entity touches the ground, {@code Block.fallOn} of the block it
     * lands on. The farmland and the plant stay as they were.
     */
    @GameTest(template = "empty")
    public static void villagerLandingOnFarmlandUnderPlantDoesNotTrampleIt(GameTestHelper helper) {
        Individual genome = ripePlant(helper);
        Villager villager = helper.spawn(EntityType.VILLAGER, PLANT);
        villager.setNoAi(true);
        BlockPos soil = helper.absolutePos(SOIL);
        BlockState farmland = helper.getLevel().getBlockState(soil);
        farmland.getBlock().fallOn(helper.getLevel(), farmland, soil, villager, 5.0F);
        assertPlantStays(helper, genome);
        helper.succeed();
    }

    /**
     * A player puts each item of the mod that is meant for compost into an empty composter, and
     * the composter rises to level 1; an item that is not compostable leaves it empty. An empty
     * composter takes any compostable item on the first try, so the test does not depend on luck.
     */
    @GameTest(template = "empty")
    public static void playerCompostsSeedsBerriesAndProducts(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos composter = SOIL;
        for (ItemStack stack : compostable()) {
            helper.setBlock(composter, Blocks.COMPOSTER);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            helper.useBlock(composter, player);
            assertComposterLevel(helper, composter, 1, stack);
            if (!player.getMainHandItem().isEmpty()) {
                throw new GameTestAssertException("The composter left " + player.getMainHandItem() + " in the hand");
            }
        }
        helper.setBlock(composter, Blocks.COMPOSTER);
        ItemStack control = new ItemStack(FarmingEvolution.ANALYZER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, control.copy());
        helper.useBlock(composter, player);
        assertComposterLevel(helper, composter, 0, control);
        helper.succeed();
    }

    /**
     * The same items reach an empty composter through a hopper above it, one at a time, and
     * each raises it to level 1; a hopper keeps an item that is not compostable.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void hopperFeedsComposterSeedsBerriesAndProducts(GameTestHelper helper) {
        BlockPos composter = SOIL;
        BlockPos hopperPos = composter.above();
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(hopperPos);
        GameTestSequence sequence = helper.startSequence();
        for (ItemStack stack : compostable()) {
            sequence.thenExecute(() -> {
                helper.setBlock(composter, Blocks.COMPOSTER);
                hopper.setItem(0, stack.copy());
            }).thenWaitUntil(() -> {
                if (!hopper.isEmpty()) {
                    throw new GameTestAssertException("The hopper still holds " + hopper.getItem(0));
                }
                assertComposterLevel(helper, composter, 1, stack);
            });
        }
        ItemStack control = new ItemStack(FarmingEvolution.ANALYZER.get());
        sequence.thenExecute(() -> {
            helper.setBlock(composter, Blocks.COMPOSTER);
            hopper.setItem(0, control.copy());
        }).thenIdle(40).thenExecute(() -> {
            if (!ItemStack.matches(hopper.getItem(0), control)) {
                throw new GameTestAssertException("The hopper passed on " + control + ", which is not compostable");
            }
            assertComposterLevel(helper, composter, 0, control);
        }).thenSucceed();
    }

    /**
     * A novice farmer offers two trades: it buys a product of the mod for an emerald and sells
     * bone meal for an emerald. No offer takes or gives an item a player can eat, of Minecraft or
     * of the mod.
     */
    @GameTest(template = "empty")
    public static void noviceFarmerTradesBoneMealAndNoFood(GameTestHelper helper) {
        Set<Item> products = products();
        List<String> seen = new ArrayList<>();
        boolean buysProduct = false;
        boolean sellsBoneMeal = false;
        for (int villagerNo = 0; villagerNo < 10; villagerNo++) {
            Villager farmer = helper.spawn(EntityType.VILLAGER, PLANT);
            farmer.setNoAi(true);
            farmer.setVillagerData(farmer.getVillagerData().setProfession(VillagerProfession.FARMER));
            for (MerchantOffer offer : farmer.getOffers()) {
                ItemStack costA = offer.getBaseCostA();
                ItemStack costB = offer.getCostB();
                ItemStack result = offer.getResult();
                seen.add(costA + " + " + costB + " -> " + result);
                List<ItemStack> food = Stream.of(costA, costB, result)
                        .filter(stack -> stack.has(DataComponents.FOOD)).toList();
                if (!food.isEmpty()) {
                    throw new GameTestAssertException("A farmer trades food " + food + " in " + seen.getLast());
                }
                if (products.contains(costA.getItem()) && costB.isEmpty() && result.is(Items.EMERALD)) {
                    buysProduct = true;
                }
                if (costA.is(Items.EMERALD) && costB.isEmpty() && result.is(Items.BONE_MEAL)) {
                    sellsBoneMeal = true;
                }
            }
            farmer.discard();
        }
        if (!buysProduct || !sellsBoneMeal) {
            throw new GameTestAssertException("Expected novice farmers to buy a product of the mod for an emerald ("
                    + buysProduct + ") and to sell bone meal for an emerald (" + sellsBoneMeal + "), got " + seen);
        }
        helper.succeed();
    }

    /** A ripe wild_flax plant on farmland at {@link #PLANT}; returns its genome. */
    private static Individual ripePlant(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND);
        helper.setBlock(PLANT, FarmingEvolution.CROP.get().defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        Individual genome = FarmingEvolution.BREEDING.wild("wild_flax", Sex.NONE);
        plantAt(helper).setGenome(genome);
        return genome;
    }

    private static PlantBlockEntity plantAt(GameTestHelper helper) {
        if (!(helper.getBlockEntity(PLANT) instanceof PlantBlockEntity plant)) {
            throw new GameTestAssertException("No plant at " + PLANT + ": " + helper.getBlockState(PLANT));
        }
        return plant;
    }

    /** The ripe plant is still at {@link #PLANT} on farmland, with its genome, and nothing lies around. */
    private static void assertPlantStays(GameTestHelper helper, Individual genome) {
        if (!helper.getBlockState(SOIL).is(Blocks.FARMLAND)) {
            throw new GameTestAssertException("Expected farmland under the plant, got " + helper.getBlockState(SOIL));
        }
        BlockState state = helper.getBlockState(PLANT);
        if (!state.is(FarmingEvolution.CROP.get()) || state.getValue(CropBlock.AGE) != CropBlock.MAX_AGE) {
            throw new GameTestAssertException("Expected the ripe plant to stay, got " + state);
        }
        if (!plantAt(helper).genome().equals(Optional.of(genome))) {
            throw new GameTestAssertException("The plant's genome changed to " + plantAt(helper).genome());
        }
        AABB around = new AABB(helper.absolutePos(PLANT)).inflate(3);
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around);
        if (!dropped.isEmpty()) {
            throw new GameTestAssertException("Items lie at the plant: "
                    + dropped.stream().map(ItemEntity::getItem).toList());
        }
    }

    private static void assertComposterLevel(GameTestHelper helper, BlockPos pos, int expected, ItemStack item) {
        BlockState state = helper.getBlockState(pos);
        if (!state.is(Blocks.COMPOSTER) || state.getValue(ComposterBlock.LEVEL) != expected) {
            throw new GameTestAssertException("After " + item + " the composter is " + state + ", expected level "
                    + expected);
        }
    }

    /**
     * What must compost: a seed with a genome, wild berries, and the product of every variety
     * and every species of pack data, named from pack data rather than from the mod's tag.
     */
    private static List<ItemStack> compostable() {
        List<ItemStack> items = new ArrayList<>();
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, FarmingEvolution.BREEDING.wild("wild_flax", Sex.NONE));
        items.add(seed);
        items.add(new ItemStack(FarmingEvolution.WILD_BERRIES.get()));
        products().forEach(product -> items.add(new ItemStack(product)));
        return items;
    }

    /** The product items of every variety and species of pack data. */
    private static Set<Item> products() {
        Set<String> paths = new TreeSet<>();
        FarmingEvolution.PACK.varieties().keySet().forEach(id -> paths.add(Products.varietyItem(id)));
        FarmingEvolution.PACK.species().keySet().forEach(id -> paths.add(Products.speciesItem(id)));
        Set<Item> items = paths.stream()
                .map(path -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path)))
                .collect(Collectors.toSet());
        if (items.contains(Items.AIR) || items.size() < 2) {
            throw new GameTestAssertException("Product items of pack data not registered: " + paths);
        }
        return items;
    }
}
