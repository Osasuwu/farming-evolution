package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Items the mod no longer has, in a world saved while it had them. Neither Minecraft nor
 * NeoForge 21.1 keeps a table of registered items in the save, so a world opens whatever
 * items it names; a stack of an item that is not registered is dropped while its container
 * is read. The test reads such a container the way the game reads a saved chunk.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RemovedItemGameTests {
    private RemovedItemGameTests() {
    }

    /**
     * farmingevolution:fiber, the plant fibre of the prototype, was removed in #56. A chest
     * saved with it next to a string loads: the fibre's slot comes back empty, the string stays.
     */
    @GameTest(template = "empty")
    public static void chestSavedWithRemovedFiberLoadsWithoutIt(GameTestHelper helper) {
        ResourceLocation fiber = ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "fiber");
        if (BuiltInRegistries.ITEM.containsKey(fiber)) {
            throw new GameTestAssertException(fiber + " is still registered");
        }

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos);
        ListTag items = new ListTag();
        items.add(savedStack(0, fiber.toString()));
        items.add(savedStack(1, "minecraft:string"));
        CompoundTag saved = new CompoundTag();
        saved.put("Items", items);
        chest.loadWithComponents(saved, helper.getLevel().registryAccess());

        ItemStack first = chest.getItem(0);
        ItemStack second = chest.getItem(1);
        if (!first.isEmpty()) {
            throw new GameTestAssertException("Expected the slot of the removed fibre to be empty, got " + first);
        }
        if (!second.is(Items.STRING) || second.getCount() != 1) {
            throw new GameTestAssertException("Expected the string to stay in its slot, got " + second);
        }
        helper.succeed();
    }

    /** One stack of a container as the game saves it: slot, item id and count. */
    private static CompoundTag savedStack(int slot, String id) {
        CompoundTag stack = new CompoundTag();
        stack.putByte("Slot", (byte) slot);
        stack.putString("id", id);
        stack.putInt("count", 1);
        return stack;
    }
}
