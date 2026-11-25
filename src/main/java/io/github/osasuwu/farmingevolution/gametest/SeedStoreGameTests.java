package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.store.SeedStore;
import io.github.osasuwu.farmingevolution.store.SeedStoreBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The seed store of issue #30: it takes seeds from a hopper and from a Create belt through a
 * Create funnel, lets out only the seeds that pass the condition the player has set, and gives
 * back every seed with the genome it came in with. Create is reached by registry names and
 * through its items used by a stand-in player, the way a player builds it; the mod does not
 * compile against Create.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeedStoreGameTests {
    private static final Breeding BREEDING = FarmingEvolution.BREEDING;

    private SeedStoreGameTests() {
    }

    // ---- In: hopper, Create belt and funnel ----

    /** A hopper on top of the store moves its seeds, each genome in its own stack, into the store. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void hopperPutsSeedsIntoStore(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        BlockPos hopperPos = storePos.above();
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        List<ItemStack> seeds = List.of(seed(flax(5)), seed(flax(7)), seed(flax(9)));
        IItemHandler hopper = handler(helper, hopperPos);
        for (ItemStack stack : seeds) {
            insertAll(hopper, stack.copy());
        }
        SeedStoreBlockEntity store = store(helper, storePos);
        helper.succeedWhen(() -> {
            if (!contents(hopper).isEmpty()) {
                throw new GameTestAssertException("The hopper still holds " + contents(hopper));
            }
            assertSameSeeds("store", seeds, store.contents());
        });
    }

    /**
     * A Create belt, laid by a player with a belt connector between two shafts and driven by a
     * creative motor, carries seeds to a Create funnel on the store's side above the belt's last
     * segment; the funnel puts them into the store.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void createBeltAndFunnelPutSeedsIntoStore(GameTestHelper helper) {
        buildBelt(helper);
        BlockPos storePos = new BlockPos(2, 2, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        BlockPos funnelPos = storePos.west();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        use(helper, player, createItem("andesite_funnel"), storePos, Direction.WEST);
        ResourceLocation funnel = BuiltInRegistries.BLOCK.getKey(helper.getBlockState(funnelPos).getBlock());
        if (!funnel.equals(ResourceLocation.fromNamespaceAndPath("create", "andesite_belt_funnel"))) {
            throw new GameTestAssertException("The funnel on the store above the belt is " + funnel
                    + ", not a belt funnel");
        }

        List<ItemStack> seeds = List.of(seed(flax(6)), seed(flax(8)));
        putOnBelt(helper, seeds);
        SeedStoreBlockEntity store = store(helper, storePos);
        helper.succeedWhen(() -> assertSameSeeds("store", seeds, store.contents()));
    }

    /**
     * The control of the belt test: without a funnel the belt carries a seed up to a store it
     * runs into and holds it there, at the store's side, but does not put it in.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void createBeltAloneDoesNotPutSeedsIntoStore(GameTestHelper helper) {
        buildBelt(helper);
        BlockPos storePos = new BlockPos(2, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        List<ItemStack> seeds = List.of(seed(flax(6)));
        putOnBelt(helper, seeds);
        SeedStoreBlockEntity store = store(helper, storePos);
        helper.runAtTickTime(300, () -> {
            if (!store.contents().isEmpty()) {
                throw new GameTestAssertException("A belt with no funnel put " + store.contents() + " into the store");
            }
            List<ItemStack> first = contents(handler(helper, new BlockPos(0, 1, 1)));
            if (!first.isEmpty() || !ejected(helper).isEmpty()) {
                throw new GameTestAssertException("The belt did not carry the seed to the store: on its first segment "
                        + first + ", on the ground " + ejected(helper));
            }
            assertSameSeeds("last segment of the belt", seeds, contents(handler(helper, new BlockPos(1, 1, 1))));
            helper.succeed();
        });
    }

    /** The store takes nothing but seeds: a hopper keeps the stick and the fibre it holds. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void storeTakesNothingButSeeds(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        BlockPos hopperPos = storePos.above();
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        IItemHandler storage = handler(helper, storePos);
        for (Item item : List.of(Items.STICK, modItem("strong_flax"), modItem("fiber_flax_fiber"))) {
            ItemStack left = ItemHandlerHelper.insertItemStacked(storage, new ItemStack(item), false);
            if (left.getCount() != 1) {
                throw new GameTestAssertException("The store took " + item);
            }
        }
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        IItemHandler hopper = handler(helper, hopperPos);
        insertAll(hopper, new ItemStack(Items.STICK));
        insertAll(hopper, seed(flax(5)));
        SeedStoreBlockEntity store = store(helper, storePos);
        helper.runAtTickTime(60, () -> {
            assertSameSeeds("store", List.of(seed(flax(5))), store.contents());
            List<ItemStack> kept = contents(hopper);
            if (kept.size() != 1 || !kept.get(0).is(Items.STICK)) {
                throw new GameTestAssertException("Expected the hopper to keep the stick, it holds " + kept);
            }
            helper.succeed();
        });
    }

    // ---- Out: only the seeds that pass the condition ----

    /** Fibre flax of strength 7 and 9 give strong_flax; strength 5, wild flax and a seed with no genome do not. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void varietyConditionLetsOutSeedsGivingItsProduct(GameTestHelper helper) {
        letsOut(helper, "variety/strong_flax", List.of(flax(7), flax(9)));
    }

    /** strong_fiber is passed by strong_flax only, so it lets out the same seeds. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void requirementConditionLetsOutSeedsOfPassingVarieties(GameTestHelper helper) {
        letsOut(helper, "requirement/strong_fiber", List.of(flax(7), flax(9)));
    }

    /** A trait condition looks at the trait alone: wild flax of strength 9 passes it too. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void traitConditionLetsOutSeedsFromThresholdUp(GameTestHelper helper) {
        letsOut(helper, "trait/fiber_strength/8", List.of(flax(9), wildFlax(9)));
    }

    /** A new store holds: nothing comes out. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void holdLetsOutNothing(GameTestHelper helper) {
        letsOut(helper, "hold", List.of());
    }

    /** "all" lets out every seed, the one with no genome included. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void allLetsOutEverySeed(GameTestHelper helper) {
        List<Individual> all = new ArrayList<>(mixedSet());
        all.add(null);
        letsOut(helper, "all", all);
    }

    /**
     * A hopper keeps trying the store, so what the store decided about a slot must follow the
     * slot's stack and the condition. A slot emptied by the hopper and filled with a seed that
     * does not pass keeps it; a seed held under one condition comes out once the player sets one
     * it passes.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void conditionFollowsTheSlotAndThePlayer(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 2, 1);
        BlockPos hopperPos = storePos.below();
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        stepTo(helper, storePos, "variety/strong_flax");
        insertAll(store.storage(), seed(flax(9)));
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        IItemHandler hopper = handler(helper, hopperPos);
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    assertSameSeeds("hopper", List.of(seed(flax(9))), contents(hopper));
                    insertAll(store.storage(), seed(flax(5)));
                })
                .thenExecuteAfter(30, () -> {
                    assertSameSeeds("hopper", List.of(seed(flax(9))), contents(hopper));
                    assertSameSeeds("store", List.of(seed(flax(5))), store.contents());
                    stepTo(helper, storePos, "all");
                })
                .thenExecuteAfter(30, () -> {
                    assertSameSeeds("hopper", List.of(seed(flax(9)), seed(flax(5))), contents(hopper));
                    assertSameSeeds("store", List.of(), store.contents());
                })
                .thenSucceed();
    }

    /** Stepping back wraps around: from the first condition, hold, to the last one, all, and forward again. */
    @GameTest(template = "empty")
    public static void stepBackWrapsAround(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        if (!store.selection().id().equals("hold")) {
            throw new GameTestAssertException("A new store's condition is " + store.selection() + ", not hold");
        }
        store.step(true);
        if (!store.selection().id().equals("all")) {
            throw new GameTestAssertException("Stepping back went from hold to " + store.selection() + ", not all");
        }
        store.step(false);
        if (!store.selection().id().equals("hold")) {
            throw new GameTestAssertException("Stepping went from all to " + store.selection() + ", not hold");
        }
        helper.succeed();
    }

    // ---- The genome is kept ----

    /**
     * Seeds of genomes the engine bred (two species, one heterozygous for the species) go into
     * the store, the store is written as a save and read back, and the seeds let out of the read
     * store carry genomes equal to those that went in.
     */
    @GameTest(template = "empty")
    public static void genomeIsUnchangedAfterSaveAndLoad(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        List<ItemStack> seeds = bredSeeds();
        for (ItemStack stack : seeds) {
            insertAll(store.storage(), stack.copy());
        }
        store.step(true);

        CompoundTag saved = store.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity read = BlockEntity.loadStatic(helper.absolutePos(storePos), helper.getBlockState(storePos), saved,
                helper.getLevel().registryAccess());
        if (!(read instanceof SeedStoreBlockEntity loaded)) {
            throw new GameTestAssertException("The saved store reads back as " + read);
        }
        if (!loaded.selection().id().equals("all")) {
            throw new GameTestAssertException("The condition read back is " + loaded.selection() + ", not all");
        }
        List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < loaded.storage().getSlots(); slot++) {
            ItemStack taken = loaded.storage().extractItem(slot, Integer.MAX_VALUE, false);
            if (!taken.isEmpty()) {
                out.add(taken);
            }
        }
        assertSameSeeds("let out of the read store", seeds, out);
        helper.succeed();
    }

    /** Breaking the store drops its seeds with the genomes they went in with. */
    @GameTest(template = "empty")
    public static void breakingStoreDropsSeedsWithTheirGenomes(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        List<ItemStack> seeds = bredSeeds();
        for (ItemStack stack : seeds) {
            insertAll(store.storage(), stack.copy());
        }
        helper.setBlock(storePos, Blocks.AIR);
        List<ItemStack> dropped = ejected(helper).stream().filter(stack -> stack.is(FarmingEvolution.SEEDS.get())).toList();
        assertSameSeeds("dropped", seeds, dropped);
        helper.succeed();
    }

    /**
     * A store saved under other pack data, whose condition pack data no longer has, reads back
     * as hold and keeps its seeds.
     */
    @GameTest(template = "empty")
    public static void unknownConditionReadsBackAsHold(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        insertAll(store.storage(), seed(flax(8)));
        CompoundTag saved = store.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putString("selection", "variety/missing");
        BlockEntity read = BlockEntity.loadStatic(helper.absolutePos(storePos), helper.getBlockState(storePos), saved,
                helper.getLevel().registryAccess());
        if (!(read instanceof SeedStoreBlockEntity loaded) || !loaded.selection().id().equals("hold")) {
            throw new GameTestAssertException("The store with an unknown condition reads back as " + read);
        }
        assertSameSeeds("read store", List.of(seed(flax(8))), loaded.contents());
        helper.succeed();
    }

    // ---- Helpers ----

    /**
     * Fills a store with {@link #mixedSet()} and a seed with no genome, sets the condition by
     * stepping it until it shows the wanted one, and puts a hopper under it. After the hopper
     * had time to pull everything it may, it holds exactly the expected seeds and the store
     * everything else.
     *
     * @param passing the genomes expected out, null for the seed with no genome
     */
    private static void letsOut(GameTestHelper helper, String selection, List<Individual> passing) {
        BlockPos storePos = new BlockPos(1, 2, 1);
        BlockPos hopperPos = storePos.below();
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        List<ItemStack> all = new ArrayList<>();
        mixedSet().forEach(genome -> all.add(seed(genome)));
        all.add(seed(null));
        for (ItemStack stack : all) {
            insertAll(store.storage(), stack.copy());
        }

        stepTo(helper, storePos, selection);

        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        IItemHandler hopper = handler(helper, hopperPos);
        List<ItemStack> expectedOut = passing.stream().map(SeedStoreGameTests::seed).toList();
        List<ItemStack> expectedKept = all.stream()
                .filter(stack -> expectedOut.stream().noneMatch(out -> ItemStack.isSameItemSameComponents(out, stack)))
                .toList();
        // A hopper pulls one item every 8 ticks; 6 seeds need 48.
        helper.runAtTickTime(120, () -> {
            assertSameSeeds("hopper under the store with " + selection, expectedOut, contents(hopper));
            assertSameSeeds("store with " + selection, expectedKept, store.contents());
            helper.succeed();
        });
    }

    /**
     * Steps the store's condition forward until it shows the wanted one. How the player sets the
     * condition (the window of #91) is tested in {@code SeedStoreWindowGameTests}.
     */
    private static void stepTo(GameTestHelper helper, BlockPos storePos, String selection) {
        SeedStoreBlockEntity store = store(helper, storePos);
        int steps = 0;
        while (!store.selection().id().equals(selection)) {
            if (++steps > BREEDING.selections().size()) {
                throw new GameTestAssertException("Stepping the store never reached the condition " + selection);
            }
            store.step(false);
        }
    }

    /** Fibre flax of strength 5, 7 and 9 and wild flax of strength 9. */
    private static List<Individual> mixedSet() {
        return List.of(flax(5), flax(7), flax(9), wildFlax(9));
    }

    private static Individual flax(int strength) {
        return withStrength(BREEDING.wild("fiber_flax", Sex.NONE), strength);
    }

    private static Individual wildFlax(int strength) {
        return withStrength(BREEDING.wild("wild_flax", Sex.NONE), strength);
    }

    private static Individual withStrength(Individual individual, int strength) {
        return individual.with("fiber_strength", Chromosome.pure(new Allele.Numeric(strength)));
    }

    /**
     * Seeds of genomes the engine bred: wild fibre flax, wild flax, and offspring of the two,
     * at least one of which carries both species. Each is its own stack.
     */
    private static List<ItemStack> bredSeeds() {
        Individual fibre = BREEDING.wild("fiber_flax", Sex.NONE);
        Individual wild = BREEDING.wild("wild_flax", Sex.NONE);
        List<Individual> genomes = new ArrayList<>(List.of(fibre, wild));
        Random random = new Random(30);
        boolean hybrid = false;
        while (genomes.size() < 8) {
            Individual child = BREEDING.cross(wild, BREEDING.cross(fibre, fibre, Environment.NONE, random),
                    Environment.NONE, random);
            if (genomes.contains(child)) {
                continue;
            }
            Chromosome species = child.genome().get(Breeding.SPECIES);
            hybrid |= !species.active().equals(species.inactive());
            genomes.add(child);
        }
        if (!hybrid) {
            throw new GameTestAssertException("No bred genome carries both species: " + genomes);
        }
        List<ItemStack> seeds = new ArrayList<>();
        for (int i = 0; i < genomes.size(); i++) {
            ItemStack stack = seed(genomes.get(i));
            stack.setCount(1 + i);
            seeds.add(stack);
        }
        return seeds;
    }

    private static ItemStack seed(@Nullable Individual genome) {
        ItemStack stack = new ItemStack(FarmingEvolution.SEEDS.get());
        if (genome != null) {
            stack.set(FarmingEvolution.GENOME, genome);
        }
        return stack;
    }

    /**
     * Whether the two lists hold the same seeds: as many of each genome, in any order and any
     * split into stacks. Genomes are compared whole, as {@link Individual#equals}.
     */
    private static void assertSameSeeds(String where, List<ItemStack> expected, List<ItemStack> actual) {
        List<String> want = summary(expected);
        List<String> got = summary(actual);
        if (!want.equals(got)) {
            throw new GameTestAssertException("Expected in " + where + " " + want + ", got " + got);
        }
    }

    private static List<String> summary(List<ItemStack> stacks) {
        java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
        for (ItemStack stack : stacks) {
            String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + stack.get(FarmingEvolution.GENOME);
            counts.merge(key, stack.getCount(), Integer::sum);
        }
        return counts.entrySet().stream().map(entry -> entry.getValue() + " x " + entry.getKey()).toList();
    }

    /**
     * Two shafts side by side joined by a belt the way a player joins them, a creative
     * motor on the first. The belt runs along x at y 1, from x 0 to x 1, inside the 3x3x3
     * template, and carries items towards x 1.
     */
    private static void buildBelt(GameTestHelper helper) {
        BlockPos first = new BlockPos(0, 1, 1);
        BlockPos last = new BlockPos(1, 1, 1);
        for (BlockPos shaft : List.of(first, last)) {
            helper.setBlock(shaft, createBlock("shaft").defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack connector = new ItemStack(createItem("belt_connector"));
        player.setItemInHand(InteractionHand.MAIN_HAND, connector);
        for (BlockPos shaft : List.of(first, last)) {
            BlockPos absolute = helper.absolutePos(shaft);
            connector.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        }
        ResourceLocation belt = BuiltInRegistries.BLOCK.getKey(helper.getBlockState(first).getBlock());
        if (!belt.equals(ResourceLocation.fromNamespaceAndPath("create", "belt"))) {
            throw new GameTestAssertException("The belt connector left " + belt + " at the first shaft");
        }
        // From the south side the motor turns the belt so that its top runs east, towards x 1.
        helper.setBlock(new BlockPos(0, 1, 2), createBlock("creative_motor").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    /**
     * Puts each stack on its own segment of the belt from its first one on, through the item
     * handler of the segment. A belt exposes one only once it has set itself up, on its first
     * tick, so the stacks go on a few ticks in.
     */
    private static void putOnBelt(GameTestHelper helper, List<ItemStack> stacks) {
        helper.runAtTickTime(5, () -> {
            for (int i = 0; i < stacks.size(); i++) {
                insertAll(handler(helper, new BlockPos(i, 1, 1)), stacks.get(i).copy());
            }
        });
    }

    /** Uses the item, as a player in survival holding it, on the face of the block. */
    private static void use(GameTestHelper helper, Player player, Item item, BlockPos pos, Direction face) {
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(pos);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false)));
    }

    private static SeedStoreBlockEntity store(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof SeedStoreBlockEntity store) {
            return store;
        }
        throw new GameTestAssertException("No seed store at " + pos);
    }

    private static void insertAll(IItemHandler handler, ItemStack stack) {
        ItemStack left = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        if (!left.isEmpty()) {
            throw new GameTestAssertException("Could not put " + left + " into " + handler);
        }
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        if (handler == null) {
            throw new GameTestAssertException("The block at " + pos + " exposes no item handler");
        }
        return handler;
    }

    private static List<ItemStack> contents(IItemHandler handler) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /** The items lying in and right around the test's structure. */
    private static List<ItemStack> ejected(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1)).stream()
                .map(ItemEntity::getItem)
                .toList();
    }

    private static Item modItem(String path) {
        return item(ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path));
    }

    private static Item createItem(String path) {
        return item(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    private static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Item " + id + " is not registered"));
    }

    private static Block createBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Block " + id + " is not registered: is Create installed?"));
    }
}
