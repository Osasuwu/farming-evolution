package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Raw-material requirements in the game (CONCEPT.md, "Селекция → Сорт → Требование к сырью"):
 * a requirement is an item tag that holds the products of the varieties passing it. The
 * requirement is the one of the mod's pack data
 * (src/main/resources/farmingevolution/pack.json): strong_fiber, a fiber_flax with strength
 * not below 7. Of the varieties there, strong_flax passes it; strong_hemp and select_hemp
 * have the same strength but are of another species, long_hemp does not bound the strength.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RequirementGameTests {
    private RequirementGameTests() {
    }

    /**
     * Every item tag of the mod with everything in it, items of other mods included. So the
     * products of the other varieties, the products of no variety (fiber_flax_fiber among
     * them), the seeds and the prototype's fibre are in no requirement tag.
     */
    @GameTest(template = "empty")
    public static void requirementTagHoldsProductsOfPassingVarietiesAndNothingElse(GameTestHelper helper) {
        Map<String, List<String>> tags = new TreeMap<>();
        BuiltInRegistries.ITEM.getTags()
                .filter(tag -> tag.getFirst().location().getNamespace().equals(FarmingEvolution.MOD_ID))
                .forEach(tag -> tags.put(tag.getFirst().location().toString(), tag.getSecond().stream()
                        .map(item -> BuiltInRegistries.ITEM.getKey(item.value()).toString()).sorted().toList()));
        Map<String, List<String>> expected =
                Map.of("farmingevolution:requirement/strong_fiber", List.of("farmingevolution:strong_flax"));
        if (!tags.equals(expected)) {
            throw new GameTestAssertException("Expected the item tags " + expected + ", got " + tags);
        }
        helper.succeed();
    }

    /**
     * The milling recipe of kubejs/server_scripts/strong_fiber.js names the tag of strong_fiber
     * as its ingredient. A millstone takes only what some milling recipe accepts: it refuses the
     * product of a variety that does not pass the requirement and the product of no variety of
     * the same species, takes the product of the passing variety and mills it into one string.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void recipeNamingRequirementTagTakesProductOfPassingVarietyOnly(GameTestHelper helper) {
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

        for (String refused : List.of("strong_hemp", "long_hemp", "select_hemp", "fiber_flax_fiber", "hemp_fiber")) {
            ItemStack notAccepted = ItemHandlerHelper.insertItem(millstone, new ItemStack(item(refused)), false);
            if (notAccepted.getCount() != 1) {
                throw new GameTestAssertException("The millstone accepted " + refused);
            }
        }
        ItemStack notAccepted = ItemHandlerHelper.insertItem(millstone, new ItemStack(item("strong_flax")), false);
        if (!notAccepted.isEmpty()) {
            throw new GameTestAssertException("The millstone did not accept strong_flax");
        }

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

    private static Item item(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path);
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Item " + id + " is not registered"));
    }

    private static Block createBlock(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Block " + id + " is not registered: is Create installed?"));
    }
}
