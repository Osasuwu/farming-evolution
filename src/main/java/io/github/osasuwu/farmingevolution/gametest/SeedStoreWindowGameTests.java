package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Selection;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.store.SeedStore;
import io.github.osasuwu.farmingevolution.store.SeedStoreBlockEntity;
import io.github.osasuwu.farmingevolution.store.SeedStoreMenu;
import io.github.osasuwu.farmingevolution.store.SeedStoreView;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Compare;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Entry;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Filter;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Key;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Sort;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The window of the seed store (issue #91): it opens on use with an empty hand, lists the
 * seeds grouped by genome with their count, lets the player take and put seeds by hand and set
 * the condition automation follows; its filter and sort are independent. The window's clicks
 * reach the server as buttons of the menu; these tests press the same buttons on the server's
 * menu. The old save of a store reads back into the new code with its seeds and condition.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeedStoreWindowGameTests {
    private static final Breeding BREEDING = FarmingEvolution.BREEDING;

    private SeedStoreWindowGameTests() {
    }

    // ---- Opening ----

    /**
     * Use with an empty hand opens the store's window, sneaking too; use with seeds in the hand
     * still puts them in and opens nothing.
     */
    @GameTest(template = "empty")
    public static void emptyHandOpensWindowSeedsInHandGoIn(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        Opener player = new Opener(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, seed(flax(6), 10));
        helper.useBlock(storePos, player);
        if (player.opened != null) {
            throw new GameTestAssertException("Use with seeds opened " + player.opened);
        }
        assertSeeds("store after use with seeds", Map.of(label(flax(6)), 10), store.contents());
        if (!player.getMainHandItem().isEmpty()) {
            throw new GameTestAssertException("The hand still holds " + player.getMainHandItem());
        }
        helper.useBlock(storePos, player);
        if (!(player.opened instanceof SeedStoreMenu)) {
            throw new GameTestAssertException("Use with an empty hand opened " + player.opened);
        }
        player.opened = null;
        player.setShiftKeyDown(true);
        helper.useBlock(storePos, player);
        if (!(player.opened instanceof SeedStoreMenu)) {
            throw new GameTestAssertException("Sneaking use with an empty hand opened " + player.opened);
        }
        if (!store.selection().id().equals("hold")) {
            throw new GameTestAssertException("Opening the window changed the condition to " + store.selection());
        }
        helper.succeed();
    }

    // ---- What the window lists ----

    /**
     * 100 seeds of one genome lie in two slots, 64 and 36; the window's list, built from the
     * menu's store slots (the server's menu here; the client's copy is filled from the same
     * slots, which this test does not exercise), shows them as one genome of 100, beside 5 of
     * another and 3 without a genome.
     */
    @GameTest(template = "empty")
    public static void windowListsSeedsByGenomeWithCount(GameTestHelper helper) {
        SeedStoreBlockEntity store = placeStore(helper);
        insertAll(store.storage(), seed(flax(9), 64));
        insertAll(store.storage(), seed(flax(5), 5));
        insertAll(store.storage(), seed(flax(9), 36));
        insertAll(store.storage(), seed(null, 3));
        SeedStoreMenu menu = menu(helper, store);
        List<Entry> entries = SeedStoreView.entries(menu.storeStacks());
        List<String> listed = entries.stream().map(entry -> label(entry.genome()) + " x" + entry.count()).toList();
        List<String> expected = List.of(label(flax(9)) + " x100", label(flax(5)) + " x5", label(null) + " x3");
        if (!listed.equals(expected)) {
            throw new GameTestAssertException("The window lists " + listed + ", expected " + expected);
        }
        helper.succeed();
    }

    // ---- By hand ----

    /**
     * Taking by hand ignores the condition (hold): a left click takes a full stack of 64 of the
     * 100 to the cursor, a right click on a genome of 5 takes 3; putting all the cursor holds
     * back and then putting one at a time leaves the cursor and the store as counted.
     */
    @GameTest(template = "empty")
    public static void handTakesAndPutsSeedsPastTheCondition(GameTestHelper helper) {
        SeedStoreBlockEntity store = placeStore(helper);
        insertAll(store.storage(), seed(flax(9), 100));
        insertAll(store.storage(), seed(flax(5), 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SeedStoreMenu menu = menu(helper, store, player);

        press(menu, player, SeedStoreMenu.takeStack(slotOf(store, flax(9))));
        assertStack("cursor after taking a stack", seed(flax(9), 64), menu.getCarried());
        assertSeeds("store", Map.of(label(flax(9)), 36, label(flax(5)), 5), store.contents());

        press(menu, player, SeedStoreMenu.PUT_ALL);
        assertStack("cursor after putting all", ItemStack.EMPTY, menu.getCarried());
        press(menu, player, SeedStoreMenu.takeHalf(slotOf(store, flax(5))));
        assertStack("cursor after taking half of 5", seed(flax(5), 3), menu.getCarried());
        if (menu.clickMenuButton(player, SeedStoreMenu.takeStack(slotOf(store, flax(9))))) {
            throw new GameTestAssertException("Taking with a full cursor was done");
        }
        press(menu, player, SeedStoreMenu.PUT_ONE);
        assertStack("cursor after putting one", seed(flax(5), 2), menu.getCarried());
        assertSeeds("store", Map.of(label(flax(9)), 100, label(flax(5)), 3), store.contents());
        helper.succeed();
    }

    /**
     * Shift-click on a genome in the window moves a stack of it to the inventory; Shift-click
     * on seeds in the inventory puts them back in; Shift-click on a stick keeps it out.
     */
    @GameTest(template = "empty")
    public static void shiftClickMovesSeedsBetweenStoreAndInventory(GameTestHelper helper) {
        SeedStoreBlockEntity store = placeStore(helper);
        insertAll(store.storage(), seed(flax(9), 70));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SeedStoreMenu menu = menu(helper, store, player);

        press(menu, player, SeedStoreMenu.takeToInventory(slotOf(store, flax(9))));
        assertSeeds("inventory after Shift-click in the window", Map.of(label(flax(9)), 64), inventory(player));
        assertSeeds("store", Map.of(label(flax(9)), 6), store.contents());

        int seedSlot = menuSlotHolding(menu, seed(flax(9), 1));
        menu.clicked(seedSlot, 0, ClickType.QUICK_MOVE, player);
        assertSeeds("inventory after Shift-click on the seeds", Map.of(), inventory(player));
        assertSeeds("store", Map.of(label(flax(9)), 70), store.contents());

        player.getInventory().add(new ItemStack(Items.STICK));
        int stickSlot = menuSlotHolding(menu, new ItemStack(Items.STICK));
        menu.clicked(stickSlot, 0, ClickType.QUICK_MOVE, player);
        if (player.getInventory().countItem(Items.STICK) != 1) {
            throw new GameTestAssertException("The stick left the inventory");
        }
        assertSeeds("store", Map.of(label(flax(9)), 70), store.contents());
        helper.succeed();
    }

    // ---- The condition ----

    /**
     * The condition set by the window's button is the store's and the menu reports it to the
     * client; a hopper under the store then takes fibre flax 7 and 9, the seeds giving
     * strong_flax, and leaves 5 and wild flax 9.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void conditionSetInWindowGovernsTheHopper(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 2, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        for (Individual genome : List.of(flax(5), flax(7), flax(9), wildFlax(9))) {
            insertAll(store.storage(), seed(genome, 1));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SeedStoreMenu menu = menu(helper, store, player);
        List<Selection> selections = SeedStoreBlockEntity.selections();
        int index = indexOf(selections, "variety/strong_flax");
        press(menu, player, SeedStoreMenu.select(index));
        if (!store.selection().id().equals("variety/strong_flax")) {
            throw new GameTestAssertException("The window's button set the condition " + store.selection());
        }
        if (menu.selectionIndex() != index) {
            throw new GameTestAssertException("The menu reports condition " + menu.selectionIndex() + ", not " + index);
        }
        helper.setBlock(storePos.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        IItemHandler hopper = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(storePos.below()), Direction.UP);
        helper.runAtTickTime(80, () -> {
            assertSeeds("hopper", Map.of(label(flax(7)), 1, label(flax(9)), 1), contents(hopper));
            assertSeeds("store", Map.of(label(flax(5)), 1, label(wildFlax(9)), 1), store.contents());
            helper.succeed();
        });
    }

    /**
     * The store remembers per slot that a seed is held. A hopper under a store on hold finds
     * nothing to take; once the window's button sets a condition the seed passes, the seed it
     * had been refused comes out, and the one still failing stays.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void heldSeedComesOutOnceWindowSetsAPassingCondition(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 2, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        SeedStoreBlockEntity store = store(helper, storePos);
        insertAll(store.storage(), seed(flax(5), 1));
        insertAll(store.storage(), seed(flax(9), 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SeedStoreMenu menu = menu(helper, store, player);
        helper.setBlock(storePos.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        IItemHandler hopper = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(storePos.below()), Direction.UP);
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    assertSeeds("hopper under the store on hold", Map.of(), contents(hopper));
                    press(menu, player, SeedStoreMenu.select(indexOf(SeedStoreBlockEntity.selections(),
                            "variety/strong_flax")));
                })
                .thenExecuteAfter(40, () -> {
                    assertSeeds("hopper", Map.of(label(flax(9)), 1), contents(hopper));
                    assertSeeds("store", Map.of(label(flax(5)), 1), store.contents());
                })
                .thenSucceed();
    }

    // ---- Filter and sort ----

    /**
     * The window's filter by species, by trait value and its sort by species, trait and count,
     * over one set of genomes. Each filter keeps the same genomes whatever the sort, and each
     * sort orders the same way whatever the filter.
     */
    @GameTest(template = "empty")
    public static void filterAndSortWorkIndependently(GameTestHelper helper) {
        // label → count: fibre 5 x10, fibre 8 x3, fibre 9 x7, wild 9 x1, oil 4 x20, no genome x2
        List<ItemStack> slots = List.of(seed(flax(5), 10), seed(oil(4), 20), seed(flax(8), 3), seed(null, 2),
                seed(wildFlax(9), 1), seed(flax(9), 7));
        List<Entry> entries = SeedStoreView.entries(slots);
        String fibre5 = label(flax(5));
        String fibre8 = label(flax(8));
        String fibre9 = label(flax(9));
        String wild9 = label(wildFlax(9));
        String oil4 = label(oil(4));
        String none = label(null);

        Filter any = Filter.NONE;
        Filter fibre = new Filter("fiber_flax", null, Compare.AT_LEAST, 0);
        Filter strong = new Filter(null, "fiber_strength", Compare.AT_LEAST, 8);
        Filter fibreStrong = new Filter("fiber_flax", "fiber_strength", Compare.AT_LEAST, 8);
        Filter exactlyNine = new Filter(null, "fiber_strength", Compare.EXACTLY, 9);
        Filter atMostFive = new Filter(null, "fiber_strength", Compare.AT_MOST, 5);
        Sort bySpecies = new Sort(Key.SPECIES, null, false);
        Sort byTraitDown = new Sort(Key.TRAIT, "fiber_strength", true);
        Sort byCountDown = new Sort(Key.COUNT, null, true);
        Sort byCountUp = new Sort(Key.COUNT, null, false);

        // Species by id: fiber_flax, oil_flax, wild_flax; no genome last.
        expect(entries, any, bySpecies, List.of(fibre5, fibre8, fibre9, oil4, wild9, none));
        expect(entries, any, byTraitDown, List.of(fibre9, wild9, fibre8, fibre5, oil4, none));
        expect(entries, any, byCountDown, List.of(oil4, fibre5, fibre9, fibre8, wild9, none));
        expect(entries, any, byCountUp, List.of(wild9, fibre8, fibre9, fibre5, oil4, none));
        expect(entries, fibre, bySpecies, List.of(fibre5, fibre8, fibre9));
        expect(entries, fibre, byCountDown, List.of(fibre5, fibre9, fibre8));
        expect(entries, strong, bySpecies, List.of(fibre8, fibre9, wild9));
        expect(entries, strong, byCountDown, List.of(fibre9, fibre8, wild9));
        expect(entries, fibreStrong, byTraitDown, List.of(fibre9, fibre8));
        expect(entries, exactlyNine, bySpecies, List.of(fibre9, wild9));
        expect(entries, atMostFive, byTraitDown, List.of(fibre5, oil4));
        helper.succeed();
    }

    // ---- Old saves ----

    /**
     * A store saved before the window, written here in the form #30 gave it ({@code items}
     * with {@code Slot}, {@code selection} by id), reads back with every seed in its slot and
     * its condition, and the window lists them.
     */
    @GameTest(template = "empty")
    public static void storeSavedBeforeTheWindowReadsBack(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        CompoundTag saved = new CompoundTag();
        saved.putString("id", "farmingevolution:seed_store");
        ListTag items = new ListTag();
        items.add(item(helper, 3, seed(flax(9), 40)));
        items.add(item(helper, 7, seed(flax(5), 12)));
        items.add(item(helper, 9, seed(flax(9), 2)));
        saved.put("items", items);
        saved.putString("selection", "trait/fiber_strength/8");
        BlockEntity read = BlockEntity.loadStatic(helper.absolutePos(storePos), helper.getBlockState(storePos), saved,
                helper.getLevel().registryAccess());
        if (!(read instanceof SeedStoreBlockEntity loaded)) {
            throw new GameTestAssertException("The old save reads back as " + read);
        }
        if (!loaded.selection().id().equals("trait/fiber_strength/8")) {
            throw new GameTestAssertException("The old save's condition reads back as " + loaded.selection());
        }
        for (int[] slotCount : new int[][] {{3, 40}, {7, 12}, {9, 2}}) {
            int count = loaded.storage().getStackInSlot(slotCount[0]).getCount();
            if (count != slotCount[1]) {
                throw new GameTestAssertException("Slot " + slotCount[0] + " reads back with " + count + " seeds, not "
                        + slotCount[1]);
            }
        }
        List<String> listed = SeedStoreView.entries(loaded.contents()).stream()
                .map(entry -> label(entry.genome()) + " x" + entry.count()).toList();
        List<String> expected = List.of(label(flax(9)) + " x42", label(flax(5)) + " x12");
        if (!listed.equals(expected)) {
            throw new GameTestAssertException("The window lists the old save as " + listed + ", expected " + expected);
        }
        helper.succeed();
    }

    // ---- Helpers ----

    /**
     * A server player that keeps the menu the block opens for it instead of sending it to a
     * client: a mock player in the level would log in, and KubeJS then sends its data to a
     * client that is not there.
     */
    private static final class Opener extends FakePlayer {
        @Nullable
        private AbstractContainerMenu opened;

        Opener(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "seed_store_opener"));
        }

        @Override
        public OptionalInt openMenu(@Nullable MenuProvider provider) {
            return openMenu(provider, (Consumer<RegistryFriendlyByteBuf>) null);
        }

        @Override
        public OptionalInt openMenu(@Nullable MenuProvider provider,
                @Nullable Consumer<RegistryFriendlyByteBuf> extraData) {
            if (provider == null) {
                return OptionalInt.empty();
            }
            opened = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }
    }

    private static CompoundTag item(GameTestHelper helper, int slot, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Slot", slot);
        return (CompoundTag) stack.save(helper.getLevel().registryAccess(), tag);
    }

    private static void expect(List<Entry> entries, Filter filter, Sort sort, List<String> expected) {
        List<String> shown = SeedStoreView.view(entries, filter, sort).stream().map(entry -> label(entry.genome())).toList();
        if (!shown.equals(expected)) {
            throw new GameTestAssertException("With " + filter + " and " + sort + " the window shows " + shown
                    + ", expected " + expected);
        }
    }

    private static int indexOf(List<Selection> selections, String id) {
        for (int i = 0; i < selections.size(); i++) {
            if (selections.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new GameTestAssertException("No condition " + id);
    }

    private static void press(SeedStoreMenu menu, Player player, int button) {
        if (!menu.clickMenuButton(player, button)) {
            throw new GameTestAssertException("The menu refused button " + button);
        }
    }

    private static int slotOf(SeedStoreBlockEntity store, Individual genome) {
        ItemStack like = seed(genome, 1);
        for (int slot = 0; slot < SeedStoreBlockEntity.SLOTS; slot++) {
            if (ItemStack.isSameItemSameComponents(store.storage().getStackInSlot(slot), like)) {
                return slot;
            }
        }
        throw new GameTestAssertException("The store holds no " + label(genome));
    }

    private static int menuSlotHolding(SeedStoreMenu menu, ItemStack like) {
        for (int slot = SeedStoreMenu.STORE_SLOTS; slot < menu.slots.size(); slot++) {
            if (ItemStack.isSameItemSameComponents(menu.getSlot(slot).getItem(), like)) {
                return slot;
            }
        }
        throw new GameTestAssertException("No slot of the inventory holds " + like);
    }

    private static SeedStoreBlockEntity placeStore(GameTestHelper helper) {
        BlockPos storePos = new BlockPos(1, 1, 1);
        helper.setBlock(storePos, SeedStore.BLOCK.get());
        return store(helper, storePos);
    }

    private static SeedStoreMenu menu(GameTestHelper helper, SeedStoreBlockEntity store) {
        return menu(helper, store, helper.makeMockPlayer(GameType.SURVIVAL));
    }

    private static SeedStoreMenu menu(GameTestHelper helper, SeedStoreBlockEntity store, Player player) {
        return new SeedStoreMenu(1, player.getInventory(), store);
    }

    private static SeedStoreBlockEntity store(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof SeedStoreBlockEntity store) {
            return store;
        }
        throw new GameTestAssertException("No seed store at " + pos);
    }

    private static Individual flax(int strength) {
        return withStrength(BREEDING.wild("fiber_flax", Sex.NONE), strength);
    }

    private static Individual wildFlax(int strength) {
        return withStrength(BREEDING.wild("wild_flax", Sex.NONE), strength);
    }

    private static Individual oil(int strength) {
        return withStrength(BREEDING.wild("oil_flax", Sex.NONE), strength);
    }

    private static Individual withStrength(Individual individual, int strength) {
        return individual.with("fiber_strength", Chromosome.pure(new Allele.Numeric(strength)));
    }

    private static ItemStack seed(@Nullable Individual genome, int count) {
        ItemStack stack = new ItemStack(FarmingEvolution.SEEDS.get(), count);
        if (genome != null) {
            stack.set(FarmingEvolution.GENOME, genome);
        }
        return stack;
    }

    /** A genome in short: {@code fiber_flax 9}, or {@code no genome}. */
    private static String label(@Nullable Individual genome) {
        return genome == null ? "no genome" : genome.species() + " " + genome.trait("fiber_strength");
    }

    private static void insertAll(IItemHandler handler, ItemStack stack) {
        ItemStack left = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        if (!left.isEmpty()) {
            throw new GameTestAssertException("Could not put " + left + " into " + handler);
        }
    }

    private static List<ItemStack> contents(IItemHandler handler) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                stacks.add(handler.getStackInSlot(slot));
            }
        }
        return stacks;
    }

    private static List<ItemStack> inventory(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(FarmingEvolution.SEEDS.get())) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /** That the seeds are, by genome label, these counts; any split into stacks. */
    private static void assertSeeds(String where, Map<String, Integer> expected, List<ItemStack> actual) {
        Map<String, Integer> got = new TreeMap<>();
        for (ItemStack stack : actual) {
            if (!stack.is(FarmingEvolution.SEEDS.get())) {
                throw new GameTestAssertException("In " + where + " lies " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            got.merge(label(stack.get(FarmingEvolution.GENOME)), stack.getCount(), Integer::sum);
        }
        if (!got.equals(new TreeMap<>(expected))) {
            throw new GameTestAssertException("Expected in " + where + " " + new TreeMap<>(expected) + ", got " + got);
        }
    }

    private static void assertStack(String where, ItemStack expected, ItemStack actual) {
        if (expected.getCount() != actual.getCount()
                || !expected.isEmpty() && !ItemStack.isSameItemSameComponents(expected, actual)) {
            throw new GameTestAssertException("Expected in " + where + " " + expected.getCount() + " "
                    + label(expected.get(FarmingEvolution.GENOME)) + ", got " + actual.getCount() + " "
                    + label(actual.get(FarmingEvolution.GENOME)));
        }
    }
}
