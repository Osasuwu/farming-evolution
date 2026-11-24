package io.github.osasuwu.farmingevolution.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.plant.Products;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Writes out what the recipe graph stage of the check pipeline reads (README.md, "Граф
 * рецептов"): every item id and every recipe of a started server as plain item ids, tags
 * already resolved to their items. A dedicated or GameTest server is enough, no client.
 *
 * <p>Besides the recipes of the game the dump holds what the mod makes without a recipe: a
 * harvest turns seeds into the product of a variety or of a species.
 */
public final class RecipeGraphDump {
    /** The system property that names the file to write; without it nothing is written. */
    public static final String FILE_PROPERTY = "farmingevolution.recipeGraphDump";

    /** The type of the entries that stand for a harvest. */
    public static final String HARVEST_TYPE = FarmingEvolution.MOD_ID + ":harvest";

    private RecipeGraphDump() {
    }

    /** Writes the dump once the server has started, when {@link #FILE_PROPERTY} is set. */
    public static void onServerStarted(ServerStartedEvent event) {
        String file = System.getProperty(FILE_PROPERTY);
        if (file == null) {
            return;
        }
        try {
            Path path = Path.of(file);
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, of(event.getServer()).toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("The recipe graph dump could not be written to " + file, e);
        }
    }

    /**
     * The dump: {@code items} is every item id; {@code recipes} holds an entry for each recipe
     * with its {@code id}, {@code type}, {@code outputs} (item ids) and {@code inputs}, a list
     * with the item ids each input accepts. A recipe whose inputs the game does not list has
     * {@code unreadable} with the reason in place of {@code inputs}.
     */
    public static JsonObject of(MinecraftServer server) {
        JsonArray items = new JsonArray();
        BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).sorted().forEach(items::add);

        JsonArray recipes = new JsonArray();
        server.getRecipeManager().getRecipes().stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .forEach(holder -> recipes.add(entry(server, holder)));
        harvests().forEach(recipes::add);

        JsonObject dump = new JsonObject();
        dump.add("items", items);
        dump.add("recipes", recipes);
        return dump;
    }

    private static JsonObject entry(MinecraftServer server, RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        JsonObject entry = new JsonObject();
        entry.addProperty("id", holder.id().toString());
        entry.addProperty("type", String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType())));

        // Blank slots of a shaped recipe are empty ingredients; they are not inputs.
        List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
        if (ingredients.isEmpty()) {
            entry.addProperty("unreadable", recipe.isSpecial()
                    ? "a recipe computed in code, it lists no ingredients"
                    : "the recipe lists no item ingredients");
        } else {
            JsonArray inputs = new JsonArray();
            for (Ingredient ingredient : ingredients) {
                JsonArray accepted = new JsonArray();
                // An ingredient that names an empty tag accepts nothing; the game shows a barrier for it.
                if (!ingredient.hasNoItems()) {
                    Set<String> ids = new TreeSet<>();
                    for (ItemStack stack : ingredient.getItems()) {
                        ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    }
                    ids.forEach(accepted::add);
                }
                inputs.add(accepted);
            }
            entry.add("inputs", inputs);
        }

        Set<String> outputs = new TreeSet<>();
        ItemStack result = recipe.getResultItem(server.registryAccess());
        if (!result.isEmpty()) {
            outputs.add(BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
        }
        // A recipe of another mod may have several results and name only the first one above.
        // Its data form lists them all under "results", the way Create writes its recipes.
        Recipe.CODEC.encodeStart(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), recipe)
                .result()
                .filter(JsonElement::isJsonObject)
                .map(json -> json.getAsJsonObject().get("results"))
                .filter(results -> results != null && results.isJsonArray())
                .ifPresent(results -> results.getAsJsonArray().forEach(element -> itemId(element).ifPresent(outputs::add)));
        JsonArray outputIds = new JsonArray();
        outputs.forEach(outputIds::add);
        entry.add("outputs", outputIds);
        return entry;
    }

    /** The item a result of a recipe's data form names, if it names an item: a fluid result does not. */
    private static java.util.Optional<String> itemId(JsonElement result) {
        if (!result.isJsonObject() || !result.getAsJsonObject().has("id")
                || !result.getAsJsonObject().get("id").isJsonPrimitive()) {
            return java.util.Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(result.getAsJsonObject().get("id").getAsString());
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
                ? java.util.Optional.of(id.toString())
                : java.util.Optional.empty();
    }

    /** One entry for every product: a harvest of a plant grown from seeds gives it (PlantBlock). */
    private static List<JsonObject> harvests() {
        String seeds = FarmingEvolution.SEEDS.getId().toString();
        Set<String> products = new TreeSet<>();
        FarmingEvolution.PACK.varieties().keySet().forEach(id -> products.add(Products.varietyItem(id)));
        FarmingEvolution.PACK.species().keySet().forEach(id -> products.add(Products.speciesItem(id)));
        return products.stream().map(product -> {
            JsonArray accepted = new JsonArray();
            accepted.add(seeds);
            JsonArray inputs = new JsonArray();
            inputs.add(accepted);
            JsonArray outputs = new JsonArray();
            outputs.add(FarmingEvolution.MOD_ID + ":" + product);
            JsonObject entry = new JsonObject();
            entry.addProperty("id", FarmingEvolution.MOD_ID + ":harvest/" + product);
            entry.addProperty("type", HARVEST_TYPE);
            entry.add("inputs", inputs);
            entry.add("outputs", outputs);
            return entry;
        }).toList();
    }
}
