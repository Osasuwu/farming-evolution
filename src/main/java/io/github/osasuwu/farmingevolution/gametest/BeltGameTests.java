package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The belt of Create (CONCEPT.md, "Вертикальный срез", AC3 and AC4): the pack's script
 * kubejs/server_scripts/belt.js removes every recipe of create:belt_connector and adds one,
 * six fibres that pass the raw-material requirement strong_fiber in two rows of three. Of the
 * mod's pack data strong_flax is the only variety that passes it; fiber_flax_fiber is the
 * fibre of a flax of no variety, the wild type.
 *
 * <p>The fibre going through another machine of Create, the millstone, is
 * RequirementGameTests.recipeNamingRequirementTagTakesProductOfPassingVarietyOnly.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeltGameTests {
    private static final ResourceLocation BELT = ResourceLocation.fromNamespaceAndPath("create", "belt_connector");
    private static final ResourceLocation BELT_RECIPE =
            ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "belt_connector");
    private static final int FIBRES_PER_BELT = 6;

    private BeltGameTests() {
    }

    /** Six fibres of the passing variety on a crafting grid, two rows of three, are one belt. */
    @GameTest(template = "empty")
    public static void sixFibresOfPassingVarietyCraftOneBelt(GameTestHelper helper) {
        ItemStack result = craft(helper, Collections.nCopies(FIBRES_PER_BELT, modItem("strong_flax")));
        if (!result.is(item(BELT)) || result.getCount() != 1) {
            throw new GameTestAssertException("Expected 1 belt out of 6 strong_flax, got " + result);
        }
        helper.succeed();
    }

    /**
     * No other item of the mod makes a belt: the fibre of a flax of no variety, the products of
     * the varieties that do not pass the requirement, the seeds. Which items those are is read
     * from the registry, so an item added later is covered. One wild fibre among five passing
     * ones spoils the recipe too.
     */
    @GameTest(template = "empty")
    public static void noOtherItemOfTheModCraftsBelt(GameTestHelper helper) {
        Item passing = modItem("strong_flax");
        List<Item> others = BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FarmingEvolution.MOD_ID))
                .filter(item -> item != passing)
                .toList();
        Item wild = modItem("fiber_flax_fiber");
        if (!others.contains(wild)) {
            throw new GameTestAssertException("The wild fibre is not among the mod's items: " + others);
        }
        List<String> crafted = new ArrayList<>();
        for (Item other : others) {
            ItemStack result = craft(helper, Collections.nCopies(FIBRES_PER_BELT, other));
            if (!result.isEmpty()) {
                crafted.add(BuiltInRegistries.ITEM.getKey(other) + " -> " + result);
            }
        }
        if (!crafted.isEmpty()) {
            throw new GameTestAssertException("Items that are not the required fibre craft something: " + crafted);
        }

        List<Item> mixed = new ArrayList<>(Collections.nCopies(FIBRES_PER_BELT - 1, passing));
        mixed.add(wild);
        ItemStack result = craft(helper, mixed);
        if (!result.isEmpty()) {
            throw new GameTestAssertException("Five strong_flax and one wild fibre craft " + result);
        }
        helper.succeed();
    }

    /**
     * The recipe Create ships, six dried kelp, makes nothing, and the pack's recipe is the only
     * one of any type whose result is the belt.
     */
    @GameTest(template = "empty")
    public static void packRecipeIsTheOnlyRecipeOfBelt(GameTestHelper helper) {
        ItemStack fromKelp = craft(helper, Collections.nCopies(FIBRES_PER_BELT, Items.DRIED_KELP));
        if (!fromKelp.isEmpty()) {
            throw new GameTestAssertException("Six dried kelp still craft " + fromKelp);
        }
        Item belt = item(BELT);
        List<ResourceLocation> recipes = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()).is(belt))
                .map(RecipeHolder::id)
                .sorted()
                .toList();
        if (!recipes.equals(List.of(BELT_RECIPE))) {
            throw new GameTestAssertException("Expected the only recipe of the belt to be " + BELT_RECIPE + ", got " + recipes);
        }
        helper.succeed();
    }

    /**
     * Six mechanical crafters, each given one fibre of the passing variety through its item
     * handler, assemble one belt and put it into the barrel under them.
     */
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void mechanicalCraftersAssembleBeltFromPassingFibre(GameTestHelper helper) {
        IItemHandler barrel = buildCraftersAndFill(helper, modItem("strong_flax"));
        Item belt = item(BELT);
        helper.succeedWhen(() -> {
            List<ItemStack> ejected = ejectedItems(helper);
            if (!ejected.isEmpty()) {
                throw new GameTestAssertException("The crafters threw out " + ejected);
            }
            List<ItemStack> stored = contents(barrel);
            if (stored.isEmpty()) {
                throw new GameTestAssertException("Nothing came out of the crafters yet");
            }
            if (stored.size() != 1 || !stored.get(0).is(belt) || stored.get(0).getCount() != 1) {
                throw new GameTestAssertException("Expected 1 belt in the barrel, got " + stored);
            }
        });
    }

    /**
     * The same crafters given the fibre of a flax of no variety find no recipe: they throw the
     * six fibres back out and the barrel stays empty.
     */
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void mechanicalCraftersThrowWildFibreBack(GameTestHelper helper) {
        Item wild = modItem("fiber_flax_fiber");
        IItemHandler barrel = buildCraftersAndFill(helper, wild);
        helper.succeedWhen(() -> {
            List<ItemStack> stored = contents(barrel);
            if (!stored.isEmpty()) {
                throw new GameTestAssertException("The crafters made " + stored + " out of the wild fibre");
            }
            List<ItemStack> ejected = ejectedItems(helper);
            if (ejected.isEmpty()) {
                throw new GameTestAssertException("The crafters have not finished yet");
            }
            int fibres = 0;
            for (ItemStack stack : ejected) {
                if (!stack.is(wild)) {
                    throw new GameTestAssertException("The crafters threw out " + ejected);
                }
                fibres += stack.getCount();
            }
            if (fibres != FIBRES_PER_BELT) {
                throw new GameTestAssertException("Expected the 6 wild fibres back, got " + ejected);
            }
        });
    }

    /**
     * Two rows of three crafters facing north, a barrel under the middle one. The upper row
     * hands down, the lower row hands to its middle crafter, which hands to the barrel. A
     * creative motor turns a cogwheel under the lower west crafter; crafters mesh like cogwheels.
     * Every crafter gets one item; the crafters start by themselves once none is empty.
     */
    private static IItemHandler buildCraftersAndFill(GameTestHelper helper, Item item) {
        BlockPos barrelPos = new BlockPos(1, 0, 1);
        helper.setBlock(barrelPos, Blocks.BARREL);
        helper.setBlock(new BlockPos(0, 0, 2), createBlock("creative_motor").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(0, 0, 1), createBlock("cogwheel").defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z));

        BlockState crafter = createBlock("mechanical_crafter").defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        // "left" and "right" are those of a player looking at the front of the crafters.
        String[][] pointing = {{"left", "down", "right"}, {"down", "down", "down"}};
        List<BlockPos> crafters = new ArrayList<>();
        for (int row = 0; row < pointing.length; row++) {
            for (int x = 0; x < pointing[row].length; x++) {
                BlockPos pos = new BlockPos(x, 1 + row, 1);
                helper.setBlock(pos, withValue(crafter, "pointing", pointing[row][x]));
                crafters.add(pos);
            }
        }
        for (BlockPos pos : crafters) {
            ItemStack notAccepted = ItemHandlerHelper.insertItem(handler(helper, pos), new ItemStack(item), false);
            if (!notAccepted.isEmpty()) {
                throw new GameTestAssertException("The crafter at " + pos + " did not accept " + item);
            }
        }
        return handler(helper, barrelPos);
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
    private static List<ItemStack> ejectedItems(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1)).stream()
                .map(ItemEntity::getItem)
                .toList();
    }

    /** What the crafting table gives for the items laid out in rows of three, or an empty stack. */
    private static ItemStack craft(GameTestHelper helper, List<Item> items) {
        CraftingInput input = CraftingInput.of(3, items.size() / 3, items.stream().map(ItemStack::new).toList());
        return helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, String property, String value) {
        @SuppressWarnings("unchecked")
        Property<T> found = (Property<T>) state.getBlock().getStateDefinition().getProperty(property);
        if (found == null) {
            throw new GameTestAssertException(state.getBlock() + " has no property " + property);
        }
        Optional<T> parsed = found.getValue(value);
        if (parsed.isEmpty()) {
            throw new GameTestAssertException("Property " + property + " of " + state.getBlock() + " has no value " + value);
        }
        return state.setValue(found, parsed.get());
    }

    private static Item modItem(String path) {
        return item(ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path));
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
