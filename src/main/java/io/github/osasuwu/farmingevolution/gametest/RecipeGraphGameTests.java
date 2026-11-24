package io.github.osasuwu.farmingevolution.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.recipe.RecipeGraphDump;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the mod writes out for the recipe graph stage of the check pipeline (README.md, "Граф
 * рецептов"), read from the server the tests run on: it has Create, KubeJS and the scripts of
 * kubejs/, the same recipes the stage checks.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeGraphGameTests {
    private RecipeGraphGameTests() {
    }

    /**
     * One recipe of each kind the dump has to read. A tag ingredient is dumped as the items of
     * the tag: the requirement tag the mod builds on start (strong_fiber holds strong_flax
     * only) and a tag of the game. A recipe of Create with several results names them all. A
     * blank slot of a shaped recipe is not an input. A harvest stands for what the mod makes
     * without a recipe. A recipe computed in code has no inputs to list and says so.
     */
    @GameTest(template = "empty")
    public static void dumpNamesInputsAndOutputsOfRecipesOfEveryKind(GameTestHelper helper) {
        Map<String, String> dumped = new TreeMap<>();
        List<String> requirementRecipes = new ArrayList<>();
        JsonObject dump = RecipeGraphDump.of(helper.getLevel().getServer());
        for (JsonElement element : dump.getAsJsonArray("recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            String id = recipe.get("id").getAsString();
            recipe.remove("id");
            dumped.put(id, recipe.toString());
            if (recipe.toString().contains("\"farmingevolution:strong_flax\"") && id.contains(":kjs/")) {
                requirementRecipes.add(recipe.toString());
            }
        }

        // kubejs/server_scripts/strong_fiber.js; KubeJS gives the recipe an id of its own.
        expect("the recipes of KubeJS that take strong_flax", requirementRecipes.toString(), "[" + json("""
                {"type": "create:milling", "inputs": [["farmingevolution:strong_flax"]], "outputs": ["minecraft:string"]}
                """) + "]");
        expect("minecraft:barrel", dumped.get("minecraft:barrel"), json("""
                {"type": "minecraft:crafting",
                 "inputs": [%1$s, %2$s, %1$s, %1$s, %1$s, %1$s, %2$s, %1$s],
                 "outputs": ["minecraft:barrel"]}
                """.formatted(PLANKS, SLABS)));
        expect("create:crushing/raw_iron", dumped.get("create:crushing/raw_iron"), json("""
                {"type": "create:crushing", "inputs": [["minecraft:raw_iron"]],
                 "outputs": ["create:crushed_raw_iron", "create:experience_nugget"]}
                """));
        expect("farmingevolution:harvest/strong_flax", dumped.get("farmingevolution:harvest/strong_flax"), json("""
                {"type": "farmingevolution:harvest", "inputs": [["farmingevolution:seeds"]],
                 "outputs": ["farmingevolution:strong_flax"]}
                """));
        expect("minecraft:firework_rocket", dumped.get("minecraft:firework_rocket"), json("""
                {"type": "minecraft:crafting", "unreadable": "a recipe computed in code, it lists no ingredients",
                 "outputs": ["minecraft:firework_rocket"]}
                """));

        List<String> items = new ArrayList<>();
        dump.getAsJsonArray("items").forEach(item -> items.add(item.getAsString()));
        for (String item : List.of("farmingevolution:seeds", "farmingevolution:strong_flax", "minecraft:string",
                "create:millstone")) {
            if (!items.contains(item)) {
                throw new GameTestAssertException("The dump does not list the item " + item);
            }
        }
        if (items.contains("minecraft:water") || items.contains("farmingevolution:requirement/strong_fiber")) {
            throw new GameTestAssertException("The dump lists as an item what is not one");
        }
        helper.succeed();
    }

    /** The items of the tag minecraft:planks in Minecraft 1.21.1. */
    private static final String PLANKS = """
            ["minecraft:acacia_planks", "minecraft:bamboo_planks", "minecraft:birch_planks", "minecraft:cherry_planks",
             "minecraft:crimson_planks", "minecraft:dark_oak_planks", "minecraft:jungle_planks",
             "minecraft:mangrove_planks", "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:warped_planks"]
            """;

    /** The items of the tag minecraft:wooden_slabs in Minecraft 1.21.1. */
    private static final String SLABS = """
            ["minecraft:acacia_slab", "minecraft:bamboo_slab", "minecraft:birch_slab", "minecraft:cherry_slab",
             "minecraft:crimson_slab", "minecraft:dark_oak_slab", "minecraft:jungle_slab", "minecraft:mangrove_slab",
             "minecraft:oak_slab", "minecraft:spruce_slab", "minecraft:warped_slab"]
            """;

    /** The text in the form the dump has: no whitespace between the tokens. */
    private static String json(String text) {
        return JsonParser.parseString(text).toString();
    }

    private static void expect(String what, String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new GameTestAssertException("Expected " + what + " to be dumped as " + expected + ", got " + actual);
        }
    }
}
