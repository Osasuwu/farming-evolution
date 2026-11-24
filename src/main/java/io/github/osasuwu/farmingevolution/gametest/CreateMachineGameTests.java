package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's items inside Create machines. Create is reached through registries and the
 * item capability, the way any other mod or pipe would reach it, so the mod does not
 * compile against it.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateMachineGameTests {
    private CreateMachineGameTests() {
    }

    /**
     * A creative motor drives a millstone; one fibre goes in through the millstone's item
     * handler and one string is taken out of it. The milling recipe is the pack's KubeJS
     * script kubejs/server_scripts/fiber.js, so the test also needs KubeJS to have loaded it.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void millstoneTurnsFiberIntoString(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(1, 1, 1);
        BlockPos millstonePos = motorPos.above();
        helper.setBlock(motorPos, createBlock("creative_motor").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(millstonePos, createBlock("millstone"));

        IItemHandler millstone = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(millstonePos), Direction.UP);
        if (millstone == null) {
            throw new GameTestAssertException("The millstone exposes no item handler");
        }
        ItemStack notAccepted = ItemHandlerHelper.insertItem(
                millstone, new ItemStack(FarmingEvolution.FIBER.get()), false);
        if (!notAccepted.isEmpty()) {
            throw new GameTestAssertException("The millstone did not accept the fibre");
        }

        // What has been taken out so far: kept across ticks, so a wrong output is reported
        // as such on every retry until the timeout, not as "nothing came out".
        List<ItemStack> output = new ArrayList<>();
        helper.succeedWhen(() -> {
            for (int slot = 0; slot < millstone.getSlots(); slot++) {
                ItemStack taken = millstone.extractItem(slot, Integer.MAX_VALUE, false);
                if (!taken.isEmpty()) {
                    output.add(taken);
                }
            }
            if (output.isEmpty()) {
                throw new GameTestAssertException("Nothing came out of the millstone yet");
            }
            if (output.size() != 1 || !output.get(0).is(Items.STRING) || output.get(0).getCount() != 1) {
                throw new GameTestAssertException("Expected 1 string out of the millstone, got " + output);
            }
        });
    }

    private static Block createBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Block " + id + " is not registered: is Create installed?"));
    }
}
