package io.github.osasuwu.farmingevolution.checks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The recipe graph stage of the check pipeline. Reads the recipes the game wrote out (the dump
 * of the mod's RecipeGraphDump) and the settings of the pack (pack/recipe-graph.json), and
 * reports every input of a required recipe that cannot be made from the starting resources,
 * with its cause: an item nothing produces, or recipes that need each other with no way in.
 *
 * <p>An item is in reach when it is a starting resource or an output of a recipe all of whose
 * inputs are in reach; an input is in reach when one of the items it accepts is.
 */
public final class RecipeGraphCheck {
    private RecipeGraphCheck() {
    }

    /**
     * A recipe of the game. Every input is the set of items it accepts. {@code unreadable} is
     * present when the game could not list the inputs; such a recipe makes nothing here.
     */
    public record Recipe(String id, String type, List<Set<String>> inputs, Set<String> outputs,
            Optional<String> unreadable) {
    }

    /** What the game wrote out: every item id and every recipe. */
    public record Dump(Set<String> items, List<Recipe> recipes) {
    }

    /**
     * The settings of the pack: which recipes are required and the starting resources, each
     * with the reason it is one. A required entry is a recipe id, or a pattern of ids in which
     * {@code *} stands for any run of characters.
     */
    public record Settings(Set<String> required, Map<String, String> start) {
    }

    public static Dump parseDump(Reader json) {
        JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
        List<Recipe> recipes = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            List<Set<String>> inputs = new ArrayList<>();
            if (recipe.has("inputs")) {
                recipe.getAsJsonArray("inputs").forEach(input -> inputs.add(strings(input.getAsJsonArray())));
            }
            Optional<String> unreadable = recipe.has("unreadable")
                    ? Optional.of(recipe.get("unreadable").getAsString())
                    : recipe.has("inputs") ? Optional.empty() : Optional.of("the dump lists no inputs");
            recipes.add(new Recipe(recipe.get("id").getAsString(), recipe.get("type").getAsString(), inputs,
                    strings(recipe.getAsJsonArray("outputs")), unreadable));
        }
        return new Dump(strings(root.getAsJsonArray("items")), recipes);
    }

    /** @throws IllegalArgumentException when the settings are malformed; the message says what is wrong */
    public static Settings parseSettings(Reader json) {
        JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
        if (!root.has("required") || !root.has("start")) {
            throw new IllegalArgumentException("the settings need \"required\" and \"start\"");
        }
        Map<String, String> start = new TreeMap<>();
        for (JsonElement element : root.getAsJsonArray("start")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("item")) {
                throw new IllegalArgumentException("starting resource without \"item\": " + entry);
            }
            String item = entry.get("item").getAsString();
            String reason = entry.has("reason") ? entry.get("reason").getAsString().trim() : "";
            if (reason.isEmpty()) {
                throw new IllegalArgumentException("starting resource " + item + ": no reason given");
            }
            if (start.put(item, reason) != null) {
                throw new IllegalArgumentException("starting resource " + item + " is listed twice");
            }
        }
        return new Settings(strings(root.getAsJsonArray("required")), start);
    }

    private static Set<String> strings(JsonArray array) {
        Set<String> strings = new TreeSet<>();
        array.forEach(element -> strings.add(element.getAsString()));
        return strings;
    }

    /** The required recipes of the dump, in the order of their ids. */
    public static List<Recipe> required(Dump dump, Settings settings) {
        return dump.recipes().stream()
                .filter(recipe -> settings.required().stream().anyMatch(pattern -> matches(pattern, recipe.id())))
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    /** Whether the id fits the pattern: {@code *} is any run of characters, the rest is literal. */
    static boolean matches(String pattern, String id) {
        StringBuilder regex = new StringBuilder();
        for (String literal : pattern.split("\\*", -1)) {
            regex.append(regex.isEmpty() ? "" : ".*").append(Pattern.quote(literal));
        }
        return id.matches(regex.toString());
    }

    /** Every item in reach of the starting resources. */
    static Set<String> reachable(Dump dump, Settings settings) {
        Set<String> reachable = new TreeSet<>(settings.start().keySet());
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Recipe recipe : dump.recipes()) {
                if (recipe.unreadable().isEmpty() && recipe.inputs().stream().allMatch(input -> inReach(input, reachable))) {
                    grew |= reachable.addAll(recipe.outputs());
                }
            }
        }
        return reachable;
    }

    private static boolean inReach(Set<String> input, Set<String> reachable) {
        return input.stream().anyMatch(reachable::contains);
    }

    /**
     * What fails the stage, one line each; empty when every input of every required recipe is in
     * reach. First the faults of the settings, then the inputs out of reach, then their causes.
     */
    public static List<String> problems(Dump dump, Settings settings) {
        List<String> problems = new ArrayList<>();
        settings.start().keySet().stream().filter(item -> !dump.items().contains(item))
                .forEach(item -> problems.add("starting resource " + item + " is not an item of the game"));
        // A pattern may match nothing; an id written out in full names a recipe that must exist.
        settings.required().stream()
                .filter(id -> !id.contains("*") && dump.recipes().stream().noneMatch(recipe -> recipe.id().equals(id)))
                .forEach(id -> problems.add("required recipe " + id + " is not a recipe of the game"));
        List<Recipe> required = required(dump, settings);
        if (required.isEmpty()) {
            problems.add("no required recipe: nothing was checked");
        }
        if (!problems.isEmpty()) {
            return problems;
        }

        Set<String> reachable = reachable(dump, settings);
        Set<String> outOfReach = new TreeSet<>();
        for (Recipe recipe : required) {
            if (recipe.unreadable().isPresent()) {
                problems.add("recipe " + recipe.id() + ": its inputs cannot be read (" + recipe.unreadable().get() + ")");
                continue;
            }
            for (int i = 0; i < recipe.inputs().size(); i++) {
                Set<String> input = recipe.inputs().get(i);
                if (input.isEmpty()) {
                    problems.add("recipe " + recipe.id() + ": input " + (i + 1) + " accepts no item (an empty tag?)");
                } else if (!inReach(input, reachable)) {
                    problems.add("recipe " + recipe.id() + ": input " + String.join(" | ", input) + " is out of reach");
                    outOfReach.addAll(input);
                }
            }
        }
        problems.addAll(causes(dump, reachable, outOfReach));
        return problems;
    }

    /**
     * Why the given items are out of reach. Following, from each of them, the inputs that block
     * the recipes making it ends either at an item nothing makes or back at an item already
     * passed: recipes that need each other, with no way in from the starting resources.
     */
    private static List<String> causes(Dump dump, Set<String> reachable, Set<String> outOfReach) {
        // For every item on the way: the items blocking it, each with the recipe it blocks.
        Map<String, Map<String, String>> blockers = new TreeMap<>();
        List<String> pending = new ArrayList<>(outOfReach);
        while (!pending.isEmpty()) {
            String item = pending.remove(pending.size() - 1);
            if (blockers.containsKey(item)) {
                continue;
            }
            Map<String, String> blocking = new TreeMap<>();
            for (Recipe recipe : dump.recipes()) {
                if (recipe.unreadable().isPresent() || !recipe.outputs().contains(item)) {
                    continue;
                }
                recipe.inputs().stream().filter(input -> !inReach(input, reachable))
                        .forEach(input -> input.forEach(blocker -> blocking.putIfAbsent(blocker, recipe.id())));
            }
            blockers.put(item, blocking);
            pending.addAll(blocking.keySet());
        }

        List<String> causes = new ArrayList<>();
        blockers.forEach((item, blocking) -> {
            if (blocking.isEmpty()) {
                causes.add(item + ": no recipe makes it and it is not a starting resource");
            }
        });
        Set<String> done = new TreeSet<>();
        Set<Set<String>> reported = new java.util.HashSet<>();
        for (String item : blockers.keySet()) {
            findCycles(item, blockers, new LinkedHashMap<>(), done, reported, causes);
        }
        return causes;
    }

    /**
     * Depth-first walk over the blockers; {@code path} holds the items of the current branch,
     * each with the recipe that leads from it to the next one. An item met again on its own
     * branch closes a cycle, reported once whatever item the walk entered it by.
     */
    private static void findCycles(String item, Map<String, Map<String, String>> blockers, Map<String, String> path,
            Set<String> done, Set<Set<String>> reported, List<String> causes) {
        if (path.containsKey(item)) {
            List<String> cycle = new ArrayList<>(path.keySet());
            cycle = cycle.subList(cycle.indexOf(item), cycle.size());
            if (reported.add(Set.copyOf(cycle))) {
                // Start the line at the first item by id, so that it does not depend on the entry.
                int first = cycle.indexOf(cycle.stream().sorted().findFirst().orElseThrow());
                StringBuilder line = new StringBuilder("cycle without an entry: ");
                for (int i = 0; i < cycle.size(); i++) {
                    String member = cycle.get((first + i) % cycle.size());
                    line.append(member).append(" <- recipe ").append(path.get(member)).append(" <- ");
                }
                causes.add(line.append(cycle.get(first)).toString());
            }
            return;
        }
        if (done.contains(item)) {
            return;
        }
        blockers.get(item).forEach((blocker, recipe) -> {
            path.put(item, recipe);
            findCycles(blocker, blockers, path, done, reported, causes);
            path.remove(item);
        });
        done.add(item);
    }

    /**
     * The starting resources no required recipe needs, directly or through other recipes: a
     * list for the author of the settings, it fails nothing.
     */
    public static List<String> unusedStart(Dump dump, Settings settings) {
        Set<String> needed = new TreeSet<>();
        List<String> pending = new ArrayList<>();
        required(dump, settings).forEach(recipe -> recipe.inputs().forEach(pending::addAll));
        while (!pending.isEmpty()) {
            String item = pending.remove(pending.size() - 1);
            if (!needed.add(item)) {
                continue;
            }
            dump.recipes().stream()
                    .filter(recipe -> recipe.unreadable().isEmpty() && recipe.outputs().contains(item))
                    .forEach(recipe -> recipe.inputs().forEach(pending::addAll));
        }
        return settings.start().keySet().stream().filter(item -> !needed.contains(item)).toList();
    }

    public static void main(String[] args) throws IOException {
        Path dumpFile = Path.of(args[0]);
        Path settingsFile = Path.of(args[1]);
        if (!Files.isRegularFile(dumpFile)) {
            System.err.println("Recipe graph check: the game wrote no recipe dump at " + dumpFile);
            System.exit(1);
        }
        Dump dump;
        Settings settings;
        try (Reader dumpJson = Files.newBufferedReader(dumpFile, StandardCharsets.UTF_8);
                Reader settingsJson = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
            dump = parseDump(dumpJson);
            settings = parseSettings(settingsJson);
        } catch (RuntimeException e) {
            System.err.println("Recipe graph check cannot read " + dumpFile + " or " + settingsFile + ": " + e.getMessage());
            System.exit(1);
            return;
        }
        List<Recipe> required = required(dump, settings);
        System.out.println("Recipe graph (" + dumpFile + "): " + dump.recipes().size() + " recipes, "
                + required.size() + " required, " + settings.start().size() + " starting resources");
        required.forEach(recipe -> System.out.println("  required: " + recipe.id() + " (" + recipe.type() + ")"));
        List<String> problems = problems(dump, settings);
        if (!problems.isEmpty()) {
            System.err.println("Recipe graph check failed (starting resources: " + settingsFile + "):");
            problems.forEach(problem -> System.err.println("  " + problem));
            System.exit(1);
        }
        unusedStart(dump, settings).forEach(item -> System.out.println("  starting resource not needed: " + item));
        System.out.println("Every input of the required recipes is in reach of the starting resources.");
    }
}
