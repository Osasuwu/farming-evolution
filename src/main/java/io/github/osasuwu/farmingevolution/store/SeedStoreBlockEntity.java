package io.github.osasuwu.farmingevolution.store;

import com.mojang.logging.LogUtils;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Selection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

/**
 * What a seed store holds: {@link #SLOTS} slots of seeds and the condition its output follows.
 *
 * <p>A slot holds one stack of seeds; seeds of different genomes do not stack, so a slot is one
 * genome. The stacks are kept as they came: the genome component on them is never rewritten,
 * only read to decide the condition.
 *
 * <p>Saved form: {@code {items: [{Slot: n, id, count, components}], selection: "<id>"}}, the
 * stacks in the game's own format, where the genome is the component
 * {@code farmingevolution:genome} as on any seed. A selection pack data no longer has becomes
 * {@code hold}, with a line in the log; the seeds are kept.
 */
public final class SeedStoreBlockEntity extends BlockEntity {
    /** How many genomes one store keeps: a chest keeps 27. */
    public static final int SLOTS = 128;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ITEMS_TAG = "items";
    private static final String SLOT_TAG = "Slot";
    private static final String SELECTION_TAG = "selection";

    /** Every condition, in the order the player steps through them. */
    private static final List<Selection> SELECTIONS = FarmingEvolution.BREEDING.selections();
    private static final Selection HOLD = SELECTIONS.getFirst();

    private final Storage storage = new Storage();
    private Selection selection = HOLD;

    public SeedStoreBlockEntity(BlockPos pos, BlockState state) {
        super(SeedStore.BLOCK_ENTITY.get(), pos, state);
    }

    /** The item handler of the store, on every side. */
    public ItemStackHandler storage() {
        return storage;
    }

    public Selection selection() {
        return selection;
    }

    /** Steps to the next condition, or to the previous one; the list wraps around. */
    public Selection step(boolean back) {
        int index = SELECTIONS.indexOf(selection);
        int size = SELECTIONS.size();
        selection = SELECTIONS.get(Math.floorMod(index + (back ? -1 : 1), size));
        storage.forgetVerdicts();
        setChanged();
        return selection;
    }

    /**
     * Whether the seeds of the stack pass the current condition. The engine decides it from the
     * genome; a seed with no genome has nothing to decide by and passes only {@code all}.
     */
    public boolean passes(ItemStack stack) {
        Individual genome = stack.get(FarmingEvolution.GENOME);
        return genome == null ? selection.id().equals("all") : selection.passes(genome);
    }

    /** The stacks the store holds, in slot order. */
    public List<ItemStack> contents() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag items = new ListTag();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag item = new CompoundTag();
                item.putInt(SLOT_TAG, slot);
                items.add(stack.save(registries, item));
            }
        }
        tag.put(ITEMS_TAG, items);
        tag.putString(SELECTION_TAG, selection.id());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOTS; slot++) {
            storage.setStackWithoutNotice(slot, ItemStack.EMPTY);
        }
        ListTag items = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            int slot = item.getInt(SLOT_TAG);
            Optional<ItemStack> stack = ItemStack.parse(registries, item);
            if (slot < 0 || slot >= SLOTS || stack.isEmpty()) {
                LOGGER.error("Seed store at {}: unreadable slot {} dropped: {}", getBlockPos(), slot, item);
                continue;
            }
            storage.setStackWithoutNotice(slot, stack.get());
        }
        String id = tag.getString(SELECTION_TAG);
        selection = SELECTIONS.stream().filter(known -> known.id().equals(id)).findFirst().orElseGet(() -> {
            if (!id.isEmpty()) {
                LOGGER.warn("Seed store at {}: unknown selection {}, replaced with {}", getBlockPos(), id, HOLD.id());
            }
            return HOLD;
        });
        storage.forgetVerdicts();
    }

    /**
     * Takes seeds only, any number of genomes; lets out only the seeds that pass the condition.
     * Its own class, not {@link ItemStackHandler}: Create mounts only that exact class as storage
     * of a contraption, which would read the store past its condition.
     *
     * <p>A hopper under a store that lets nothing out tries every slot every tick, so the verdict
     * of each slot is kept until its stack or the condition changes.
     */
    private final class Storage extends ItemStackHandler {
        private static final byte UNKNOWN = 0;
        private static final byte PASSES = 1;
        private static final byte HELD = 2;

        private final byte[] verdicts = new byte[SLOTS];

        Storage() {
            super(SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(FarmingEvolution.SEEDS.get());
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlotIndex(slot);
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (verdicts[slot] == UNKNOWN) {
                verdicts[slot] = passes(stack) ? PASSES : HELD;
            }
            if (verdicts[slot] == HELD) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }

        void setStackWithoutNotice(int slot, ItemStack stack) {
            stacks.set(slot, stack);
            verdicts[slot] = UNKNOWN;
        }

        void forgetVerdicts() {
            Arrays.fill(verdicts, UNKNOWN);
        }

        @Override
        protected void onContentsChanged(int slot) {
            verdicts[slot] = UNKNOWN;
            setChanged();
        }
    }
}
