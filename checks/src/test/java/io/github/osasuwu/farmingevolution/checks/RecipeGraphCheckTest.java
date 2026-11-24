package io.github.osasuwu.farmingevolution.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.osasuwu.farmingevolution.checks.RecipeGraphCheck.Dump;
import io.github.osasuwu.farmingevolution.checks.RecipeGraphCheck.Settings;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeGraphCheckTest {
    /** Every item the dumps of these tests name; the game would list its whole item registry. */
    private static final String ITEMS = """
            "items": ["pack:seeds", "pack:fiber", "pack:thread", "pack:rope", "pack:belt", "pack:resin",
                      "pack:glue", "pack:cloth", "other:gear", "other:cog"]
            """;

    private static Dump dump(String recipes) {
        return RecipeGraphCheck.parseDump(new StringReader("{" + ITEMS + ", \"recipes\": [" + recipes + "]}"));
    }

    private static String recipe(String id, String inputs, String outputs) {
        return "{\"id\": \"" + id + "\", \"type\": \"test:craft\", \"inputs\": " + inputs
                + ", \"outputs\": " + outputs + "}";
    }

    /** Recipes with an id in the namespace "pack" are the required ones; the starting resources are given. */
    private static Settings settings(String... start) {
        StringBuilder json = new StringBuilder("{\"required\": [\"pack:*\"], \"start\": [");
        for (int i = 0; i < start.length; i++) {
            json.append(i == 0 ? "" : ", ").append("{\"item\": \"").append(start[i]).append("\", \"reason\": \"test\"}");
        }
        return RecipeGraphCheck.parseSettings(new StringReader(json.append("]}").toString()));
    }

    @Test
    void chainFromStartingResourceThroughRecipesHasNoProblems() {
        Dump dump = dump(String.join(",",
                recipe("pack:thread", "[[\"pack:fiber\"]]", "[\"pack:thread\"]"),
                recipe("pack:belt", "[[\"pack:thread\"], [\"pack:fiber\"]]", "[\"pack:belt\"]")));
        assertEquals(List.of(), RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void inputNothingProducesIsAProblem() {
        Dump dump = dump(recipe("pack:belt", "[[\"pack:fiber\"], [\"pack:resin\"]]", "[\"pack:belt\"]"));
        assertEquals(
                List.of("recipe pack:belt: input pack:resin is out of reach",
                        "pack:resin: no recipe makes it and it is not a starting resource"),
                RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void inputMadeOnlyFromSomethingOutOfReachIsTracedToItsCause() {
        // Glue needs fibre, which is in reach, and resin, which is not: one input in reach is not enough.
        Dump dump = dump(String.join(",",
                recipe("other:glue", "[[\"pack:fiber\"], [\"pack:resin\"]]", "[\"pack:glue\"]"),
                recipe("pack:belt", "[[\"pack:glue\"]]", "[\"pack:belt\"]")));
        assertEquals(
                List.of("recipe pack:belt: input pack:glue is out of reach",
                        "pack:resin: no recipe makes it and it is not a starting resource"),
                RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void twoRecipesNeedingEachOtherAreACycleWithoutEntry() {
        Dump dump = dump(String.join(",",
                recipe("pack:rope", "[[\"pack:cloth\"]]", "[\"pack:rope\"]"),
                recipe("pack:cloth", "[[\"pack:rope\"]]", "[\"pack:cloth\"]")));
        assertEquals(
                List.of("recipe pack:cloth: input pack:rope is out of reach",
                        "recipe pack:rope: input pack:cloth is out of reach",
                        "cycle without an entry: pack:cloth <- recipe pack:cloth <- pack:rope"
                                + " <- recipe pack:rope <- pack:cloth"),
                RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void cycleWithAnEntryHasNoProblems() {
        // Rope and cloth turn into each other, and rope is also made from a starting resource.
        Dump dump = dump(String.join(",",
                recipe("pack:rope", "[[\"pack:cloth\"]]", "[\"pack:rope\"]"),
                recipe("pack:cloth", "[[\"pack:rope\"]]", "[\"pack:cloth\"]"),
                recipe("pack:rope_from_fiber", "[[\"pack:fiber\"]]", "[\"pack:rope\"]")));
        assertEquals(List.of(), RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void inputWithAlternativesNeedsOneOfThemInReach() {
        // A tag ingredient is dumped as its items: any one of them makes the input.
        String belt = recipe("pack:belt", "[[\"pack:resin\", \"pack:thread\"]]", "[\"pack:belt\"]");
        assertEquals(List.of(), RecipeGraphCheck.problems(dump(belt), settings("pack:thread")));
        assertEquals(
                List.of("recipe pack:belt: input pack:resin | pack:thread is out of reach",
                        "pack:resin: no recipe makes it and it is not a starting resource",
                        "pack:thread: no recipe makes it and it is not a starting resource"),
                RecipeGraphCheck.problems(dump(belt), settings("pack:fiber")));
    }

    @Test
    void inputThatAcceptsNoItemIsAProblem() {
        // An ingredient that names an empty tag is dumped with no items.
        Dump dump = dump(recipe("pack:belt", "[[\"pack:fiber\"], []]", "[\"pack:belt\"]"));
        assertEquals(List.of("recipe pack:belt: input 2 accepts no item (an empty tag?)"),
                RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void onlyRequiredRecipesAreChecked() {
        // The recipe of another namespace has an input out of reach and nothing required needs it.
        Dump dump = dump(String.join(",",
                recipe("other:gear", "[[\"other:cog\"]]", "[\"other:gear\"]"),
                recipe("pack:thread", "[[\"pack:fiber\"]]", "[\"pack:thread\"]")));
        assertEquals(List.of(), RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void recipeNamedByItsIdIsRequired() {
        Dump dump = dump(String.join(",",
                recipe("other:gear", "[[\"other:cog\"]]", "[\"other:gear\"]"),
                recipe("pack:thread", "[[\"pack:fiber\"]]", "[\"pack:thread\"]")));
        Settings settings = RecipeGraphCheck.parseSettings(new StringReader("""
                {"required": ["pack:*", "other:gear"],
                 "start": [{"item": "pack:fiber", "reason": "test"}]}
                """));
        assertEquals(
                List.of("recipe other:gear: input other:cog is out of reach",
                        "other:cog: no recipe makes it and it is not a starting resource"),
                RecipeGraphCheck.problems(dump, settings));
    }

    @Test
    void requiredIdThatIsNoRecipeOfTheGameIsAProblem() {
        Dump dump = dump(recipe("pack:thread", "[[\"pack:fiber\"]]", "[\"pack:thread\"]"));
        Settings settings = RecipeGraphCheck.parseSettings(new StringReader("""
                {"required": ["pack:*", "other:gear"],
                 "start": [{"item": "pack:fiber", "reason": "test"}]}
                """));
        assertEquals(List.of("required recipe other:gear is not a recipe of the game"),
                RecipeGraphCheck.problems(dump, settings));
    }

    @Test
    void starInARequiredPatternStandsForAnyRunOfCharactersAndTheRestIsLiteral() {
        // KubeJS names a recipe a script adds <namespace of the type>:kjs/<hash>.
        Dump dump = dump(String.join(",",
                recipe("other:kjs/a1", "[[\"other:cog\"]]", "[\"other:gear\"]"),
                recipe("other:kjsx/a1", "[[\"pack:resin\"]]", "[\"pack:glue\"]"),
                recipe("other:milling/kjs", "[[\"pack:resin\"]]", "[\"pack:glue\"]"),
                recipe("otherxkjs/a1", "[[\"pack:resin\"]]", "[\"pack:glue\"]")));
        Settings settings = RecipeGraphCheck.parseSettings(new StringReader("""
                {"required": ["*:kjs/*", "other.kjsx/a1"], "start": [{"item": "pack:fiber", "reason": "test"}]}
                """));
        assertEquals(List.of("other:kjs/a1"),
                RecipeGraphCheck.required(dump, settings).stream().map(RecipeGraphCheck.Recipe::id).toList());
    }

    @Test
    void noRequiredRecipeAtAllIsAProblem() {
        Dump dump = dump(recipe("other:gear", "[[\"pack:fiber\"]]", "[\"other:gear\"]"));
        assertEquals(List.of("no required recipe: nothing was checked"),
                RecipeGraphCheck.problems(dump, settings("pack:fiber")));
    }

    @Test
    void startingResourceThatIsNotAnItemOfTheGameIsAProblem() {
        Dump dump = dump(recipe("pack:thread", "[[\"pack:fiber\"]]", "[\"pack:thread\"]"));
        assertEquals(List.of("starting resource pack:fibre is not an item of the game"),
                RecipeGraphCheck.problems(dump, settings("pack:fiber", "pack:fibre")));
    }

    @Test
    void recipeWhoseInputsTheGameCouldNotListMakesNothingAndFailsWhenRequired() {
        // A recipe without "inputs" is one the dump could not read; "unreadable" says why.
        String unreadable = "{\"id\": \"ID\", \"type\": \"test:special\", \"unreadable\": \"no item ingredients\","
                + " \"outputs\": [\"pack:glue\"]}";
        Dump required = dump(unreadable.replace("ID", "pack:glue"));
        assertEquals(List.of("recipe pack:glue: its inputs cannot be read (no item ingredients)"),
                RecipeGraphCheck.problems(required, settings("pack:fiber")));

        Dump source = dump(String.join(",",
                unreadable.replace("ID", "other:glue"),
                recipe("pack:belt", "[[\"pack:glue\"]]", "[\"pack:belt\"]")));
        assertEquals(
                List.of("recipe pack:belt: input pack:glue is out of reach",
                        "pack:glue: no recipe makes it and it is not a starting resource"),
                RecipeGraphCheck.problems(source, settings("pack:fiber")));
    }

    @Test
    void startingResourceWithoutAReasonIsRejected() {
        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> RecipeGraphCheck.parseSettings(new StringReader("""
                        {"required": ["pack:*"], "start": [{"item": "pack:fiber", "reason": " "}]}
                        """)));
        assertEquals("starting resource pack:fiber: no reason given", rejected.getMessage());
    }

    @Test
    void startingResourcesNoRequiredRecipeNeedsAreListedAsUnused() {
        Dump dump = dump(String.join(",",
                recipe("other:thread", "[[\"pack:fiber\"]]", "[\"pack:thread\"]"),
                recipe("pack:belt", "[[\"pack:thread\", \"pack:rope\"]]", "[\"pack:belt\"]"),
                recipe("other:gear", "[[\"pack:seeds\"]]", "[\"other:gear\"]")));
        // Fibre is needed only through thread, made by a recipe that is not required itself;
        // rope is an alternative of a required input; seeds feed only a recipe nothing required
        // needs; resin feeds nothing.
        assertEquals(List.of("pack:resin", "pack:seeds"),
                RecipeGraphCheck.unusedStart(dump, settings("pack:fiber", "pack:rope", "pack:seeds", "pack:resin")));
    }
}
