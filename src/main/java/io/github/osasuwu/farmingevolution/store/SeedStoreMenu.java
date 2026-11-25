package io.github.osasuwu.farmingevolution.store;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The menu of the seed store's window (issue #91).
 *
 * <p>Its first {@link SeedStoreBlockEntity#SLOTS} slots are the store's slots. They are hidden:
 * the game sends their stacks to the client as it does a chest's, but the window does not draw
 * them; it draws the stacks grouped by genome ({@link SeedStoreView}), and the player cannot
 * click them by the game's own rules. After them come the player's inventory and hotbar.
 *
 * <p>Everything the player does to the store goes to the server as a button of the menu, the
 * game's own packet, and is done here: {@link #select set the condition}, {@link #takeStack take a
 * stack} or {@link #takeHalf half of one} to the cursor, {@link #takeToInventory move a stack}
 * to the inventory (Shift-click), put what the cursor holds in ({@link #PUT_ALL}, {@link #PUT_ONE}).
 * A Shift-click on seeds in the inventory puts them in ({@link #quickMoveStack}). Taking by hand
 * ignores the condition: the condition is for automation.
 *
 * <p>Filters and sorting are the window's alone and never reach the server.
 */
public final class SeedStoreMenu extends AbstractContainerMenu {
    public static final int STORE_SLOTS = SeedStoreBlockEntity.SLOTS;
    private static final int INVENTORY_START = STORE_SLOTS;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int SLOTS_END = HOTBAR_START + 9;

    private static final int SELECT = 0;
    private static final int TAKE_STACK = 100_000;
    private static final int TAKE_HALF = 200_000;
    private static final int TAKE_TO_INVENTORY = 300_000;
    /** Puts everything the cursor holds into the store. */
    public static final int PUT_ALL = 400_000;
    /** Puts one seed the cursor holds into the store. */
    public static final int PUT_ONE = 400_001;

    /** Where the window draws the player's inventory: the game needs it for the slots. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 140;

    @Nullable
    private final SeedStoreBlockEntity store;
    private final IItemHandler storage;
    private final ContainerLevelAccess access;
    private final DataSlot selection;
    private int version;

    /** The client's menu: the store's slots and condition arrive from the server. */
    public SeedStoreMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    /** The server's menu, over the store the player opened. */
    public SeedStoreMenu(int id, Inventory inventory, @Nullable SeedStoreBlockEntity store) {
        super(SeedStore.MENU.get(), id);
        this.store = store;
        this.storage = store != null ? store.storage() : new ItemStackHandler(STORE_SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                version++;
            }
        };
        this.access = store != null && store.getLevel() != null
                ? ContainerLevelAccess.create(store.getLevel(), store.getBlockPos())
                : ContainerLevelAccess.NULL;
        for (int slot = 0; slot < STORE_SLOTS; slot++) {
            addSlot(new HiddenSlot(storage, slot));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }
        this.selection = addDataSlot(store != null ? new DataSlot() {
            @Override
            public int get() {
                return SeedStoreBlockEntity.selections().indexOf(store.selection());
            }

            @Override
            public void set(int value) {
            }
        } : DataSlot.standalone());
    }

    /** The button that sets the condition with this index in {@link SeedStoreBlockEntity#selections()}. */
    public static int select(int index) {
        return SELECT + index;
    }

    /** The button that takes up to a stack of the genome in the store slot to the cursor. */
    public static int takeStack(int slot) {
        return TAKE_STACK + slot;
    }

    /** The button that takes half of what a stack of the genome in the store slot would be, rounded up. */
    public static int takeHalf(int slot) {
        return TAKE_HALF + slot;
    }

    /** The button that moves up to a stack of the genome in the store slot to the inventory. */
    public static int takeToInventory(int slot) {
        return TAKE_TO_INVENTORY + slot;
    }

    /** The index of the store's condition in {@link SeedStoreBlockEntity#selections()}, as the client knows it. */
    public int selectionIndex() {
        return selection.get();
    }

    /** The stacks of the store's slots, as the client knows them. */
    public List<ItemStack> storeStacks() {
        List<ItemStack> stacks = new ArrayList<>(STORE_SLOTS);
        for (int slot = 0; slot < STORE_SLOTS; slot++) {
            stacks.add(slots.get(slot).getItem());
        }
        return stacks;
    }

    /** Grows when the client learns of a change in the store's slots. */
    public int version() {
        return version;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (store == null || id < 0) {
            return false;
        }
        if (id >= SELECT && id < TAKE_STACK) {
            List<io.github.osasuwu.farmingevolution.breeding.Selection> selections = SeedStoreBlockEntity.selections();
            if (id - SELECT >= selections.size()) {
                return false;
            }
            store.select(selections.get(id - SELECT));
            return true;
        }
        if (id == PUT_ALL || id == PUT_ONE) {
            return put(id == PUT_ALL);
        }
        int slot = id % 100_000;
        int action = id - slot;
        if (slot >= STORE_SLOTS || !getCarried().isEmpty()) {
            return false;
        }
        ItemStack like = store.storage().getStackInSlot(slot);
        if (like.isEmpty()) {
            return false;
        }
        int full = like.getMaxStackSize();
        switch (action) {
            case TAKE_STACK -> setCarried(store.takeByHand(like, full));
            case TAKE_HALF -> setCarried(store.takeByHand(like, (Math.min(full, store.count(like)) + 1) / 2));
            case TAKE_TO_INVENTORY -> {
                ItemStack taken = store.takeByHand(like, full);
                moveItemStackTo(taken, INVENTORY_START, SLOTS_END, true);
                if (!taken.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(store.storage(), taken, false);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean put(boolean all) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || !carried.is(FarmingEvolution.SEEDS.get())) {
            return false;
        }
        ItemStack put = carried.copyWithCount(all ? carried.getCount() : 1);
        ItemStack left = ItemHandlerHelper.insertItemStacked(store.storage(), put, false);
        carried.shrink(put.getCount() - left.getCount());
        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        return true;
    }

    /**
     * Shift-click in the inventory: seeds go into the store, as much as fits; anything else moves
     * between the inventory and the hotbar, as in a furnace. The client leaves it to the server.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (store == null || index < INVENTORY_START || index >= SLOTS_END) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.is(FarmingEvolution.SEEDS.get())) {
            ItemStack left = ItemHandlerHelper.insertItemStacked(store.storage(), stack.copy(), false);
            slot.setByPlayer(left);
            return ItemStack.EMPTY;
        }
        ItemStack moving = stack.copy();
        boolean moved = index < HOTBAR_START
                ? moveItemStackTo(moving, HOTBAR_START, SLOTS_END, false)
                : moveItemStackTo(moving, INVENTORY_START, HOTBAR_START, false);
        if (moved) {
            slot.setByPlayer(moving);
        }
        return ItemStack.EMPTY;
    }

    /** Double-click gathering never pulls from the store's hidden slots. */
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof HiddenSlot);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SeedStore.BLOCK.get());
    }

    /** A slot of the store: synced to the client, never drawn, never clicked by the game's rules. */
    private static final class HiddenSlot extends SlotItemHandler {
        HiddenSlot(IItemHandler storage, int slot) {
            super(storage, slot, -10_000, -10_000);
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
