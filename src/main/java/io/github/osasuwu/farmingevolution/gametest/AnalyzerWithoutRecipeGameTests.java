package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The analyzer is a debug tool without a recipe (issue #124; CONCEPT.md, "Что видит игрок"): a
 * player gets it from the creative tab or by command, never by crafting. Its pollen still goes
 * before the wind (WindPollinationGameTests.analyzerPollenGoesBeforeWind); that it is in the
 * creative tab is CreativeTabGameTests.tabHoldsEveryItemOfTheModAndHasNameAndIcon.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnalyzerWithoutRecipeGameTests {
    /** A recipe the pack's KubeJS scripts add, as the analyzer's recipe was added before #124. */
    private static final ResourceLocation BELT_RECIPE =
            ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "belt_connector");

    private AnalyzerWithoutRecipeGameTests() {
    }

    /**
     * No recipe of any type, of the game, of Create, of the mod or of a KubeJS script, has the
     * analyzer as its result. The pack's scripts did run: the belt's recipe they add is there,
     * so an empty answer is not a recipe manager that never saw them.
     */
    @GameTest(template = "empty")
    public static void noRecipeMakesTheAnalyzer(GameTestHelper helper) {
        Collection<RecipeHolder<?>> recipes = helper.getLevel().getRecipeManager().getRecipes();
        if (recipes.stream().noneMatch(recipe -> recipe.id().equals(BELT_RECIPE))) {
            throw new GameTestAssertException("The recipes of the pack's KubeJS scripts are not loaded: no " + BELT_RECIPE);
        }
        List<ResourceLocation> analyzerRecipes = recipes.stream()
                .filter(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess())
                        .is(FarmingEvolution.ANALYZER.get()))
                .map(RecipeHolder::id)
                .sorted()
                .toList();
        if (!analyzerRecipes.isEmpty()) {
            throw new GameTestAssertException("Expected no recipe of the analyzer, got " + analyzerRecipes);
        }
        helper.succeed();
    }
}
