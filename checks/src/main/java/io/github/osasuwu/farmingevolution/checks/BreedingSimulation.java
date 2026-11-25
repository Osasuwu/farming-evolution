package io.github.osasuwu.farmingevolution.checks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Requirement;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.breeding.Variety;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Breeds lines without the game and reports how many generations and how much game time each
 * selection method needs to reach every variety of the scenario's pack data. A line that does
 * not reach its variety within its method's time limit fails the check pipeline. So does a
 * raw-material requirement that a method cannot meet: see {@link #requirements}. The gaps
 * between the varieties are printed and fail nothing: see {@link VarietyGaps}.
 *
 * <p>The model (issue #104; CONCEPT.md, AC7). A line is a field of the method's number of
 * plants, sown first with wild seeds of the variety's species. Every generation each plant
 * ripens, is pollinated and gives the scenario's number of seeds; each seed is a cross of the
 * plant with its father. Methods differ in the father:
 *
 * <ul>
 * <li>{@code self}: the plant itself. Selection: every plant stands alone and pollinates itself.
 * <li>{@code wind}: for every seed, one of the other plants of the field at random; all of them
 * stand next to each other. A field of one plant pollinates itself.
 * </ul>
 *
 * The breeder sees only the expressed values: the seeds are ranked by them and the best ones,
 * as many as the field holds, are sown next; equal seeds keep the order of their harvest. The
 * old plants are not kept. The line has reached the variety once a sown plant lies in its
 * region: harvesting that plant gives the variety's product.
 *
 * <p>A threshold is either bound of the variety's region. A plant that expresses a value above
 * a "not above" bound is outside the region, and the ranking leads a trait to the region from
 * both sides: up to a "not below" bound and down to a "not above" one.
 */
public final class BreedingSimulation {
    /** Where the father of a seed comes from. */
    enum Pollination {
        /** The plant itself: selection. */
        SELF,
        /** Another plant of the field at random, for every seed: wind. */
        WIND
    }

    /**
     * A selection method as the scenario describes it.
     *
     * @param plants       how many plants the field holds every generation
     * @param minutesLimit the game time a line may take, in minutes of play
     */
    record Method(String id, Pollination pollination, int plants, int minutesLimit) {
    }

    /**
     * What to simulate: every variety of the pack by every method.
     *
     * @param runs                 how many independent lines are bred for each variety and method
     * @param seedsPerPlant        how many seeds a ripe plant gives
     * @param minutesPerGeneration game time from a sown seed to a ripe plant, in minutes of play
     */
    record Scenario(PackData pack, long seed, int runs, int seedsPerPlant, int minutesPerGeneration,
            List<Method> methods) {
        /** The most generations a line of the method may take: whole generations within its time limit. */
        int generationLimit(Method method) {
            return method.minutesLimit() / minutesPerGeneration;
        }
    }

    /**
     * The lines of one variety bred by one method.
     *
     * @param runs for every line, the generations it took, or empty when it did not get there
     */
    record Outcome(String variety, Method method, int minutesPerGeneration, List<OptionalInt> runs) {
        String line() {
            List<Integer> reached = runs.stream().filter(OptionalInt::isPresent).map(OptionalInt::getAsInt).sorted().toList();
            String text = "variety " + variety + ", method " + method.id() + ": ";
            if (!reached.isEmpty()) {
                text += "median " + time(reached.get(reached.size() / 2))
                        + ", worst " + time(reached.get(reached.size() - 1)) + "; ";
            }
            return text + reached.size() + " of " + runs.size() + " lines reached the thresholds within "
                    + method.minutesLimit() + " min";
        }

        /** Why this outcome fails the pipeline, or empty when every line reached the variety. */
        Optional<String> failure() {
            long missed = runs.stream().filter(OptionalInt::isEmpty).count();
            return missed == 0 ? Optional.empty() : Optional.of("variety " + variety + ", method " + method.id() + ": "
                    + missed + " of " + runs.size() + " lines did not reach the thresholds within "
                    + method.minutesLimit() + " min");
        }

        private String time(int generations) {
            return generations + " generations (" + generations * minutesPerGeneration + " min)";
        }
    }

    /**
     * How one method gets the raw material a requirement asks for.
     *
     * @param passing   the varieties that pass the requirement, in pack data order
     * @param reachedBy the lines of the passing variety that the method reaches soonest, or
     *                  empty when it reaches none of them with every line
     */
    record RequirementOutcome(String requirement, Method method, List<String> passing, Optional<Outcome> reachedBy) {
        RequirementOutcome {
            passing = List.copyOf(passing);
        }

        String line() {
            String text = "requirement " + requirement + ", method " + method.id() + ": ";
            if (reachedBy.isEmpty()) {
                return text + "out of reach";
            }
            Outcome outcome = reachedBy.get();
            return text + "variety " + outcome.variety() + ", median " + outcome.time(median(outcome))
                    + ", worst " + outcome.time(worst(outcome));
        }

        /** Why this outcome fails the pipeline, or empty when the method reaches a passing variety. */
        Optional<String> failure() {
            return reachedBy.isPresent() ? Optional.empty() : Optional.of("requirement " + requirement
                    + ", method " + method.id() + ": none of the varieties that pass it ("
                    + String.join(", ", passing) + ") was reached by every line within "
                    + method.minutesLimit() + " min");
        }
    }

    private BreedingSimulation() {
    }

    /**
     * Every requirement by every method, from the outcomes of the varieties. No lines are bred
     * here: a line that has reached a variety that passes the requirement gives its raw
     * material. Of the passing varieties that every line reached, the method is credited with
     * the one of the lowest median, then of the lowest worst line, then the first in pack data.
     */
    static List<RequirementOutcome> requirements(Scenario scenario, List<Outcome> outcomes) {
        List<RequirementOutcome> reached = new ArrayList<>();
        for (Requirement requirement : scenario.pack().requirements().values()) {
            List<String> passing = scenario.pack().passing(requirement).stream().map(Variety::id).toList();
            for (Method method : scenario.methods()) {
                Outcome soonest = null;
                for (String variety : passing) {
                    for (Outcome outcome : outcomes) {
                        if (outcome.variety().equals(variety) && outcome.method().equals(method)
                                && outcome.failure().isEmpty() && (soonest == null
                                || median(outcome) < median(soonest)
                                || median(outcome) == median(soonest) && worst(outcome) < worst(soonest))) {
                            soonest = outcome;
                        }
                    }
                }
                reached.add(new RequirementOutcome(requirement.id(), method, passing, Optional.ofNullable(soonest)));
            }
        }
        return reached;
    }

    /** The middle one of the sorted generation counts of an outcome whose every line got there. */
    private static int median(Outcome outcome) {
        return outcome.runs().stream().mapToInt(OptionalInt::getAsInt).sorted().toArray()[outcome.runs().size() / 2];
    }

    private static int worst(Outcome outcome) {
        return outcome.runs().stream().mapToInt(OptionalInt::getAsInt).max().getAsInt();
    }

    /**
     * Reads a scenario. The pack data is either written in the scenario or named by a path,
     * relative to {@code directory}: the mod's own pack data, so the simulation breeds what the
     * game breeds. Throws IllegalArgumentException for a scenario that would simulate nothing,
     * that lacks a key or that the model cannot breed, and PackDataException for broken pack data.
     */
    static Scenario parse(Reader json, Path directory) {
        JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
        String entry = "scenario: ";
        PackData pack = pack(get(root, "pack", entry), directory, entry);
        if (pack.varieties().isEmpty()) {
            throw new IllegalArgumentException(entry + "pack data has no varieties to breed");
        }
        for (Variety variety : pack.varieties().values()) {
            // A plant of a two-sexed species could not pollinate itself, nor every neighbour.
            if (pack.species(variety.species()).twoSexed()) {
                throw new IllegalArgumentException(entry + "variety " + variety.id() + ": species "
                        + variety.species() + " has sexes, and the simulation breeds plants that pollinate"
                        + " themselves or each other");
            }
        }
        int runs = get(root, "runs", entry).getAsInt();
        if (runs < 1) {
            throw new IllegalArgumentException(entry + "runs must be at least 1, got " + runs);
        }
        int seedsPerPlant = get(root, "seeds_per_plant", entry).getAsInt();
        if (seedsPerPlant < 1) {
            throw new IllegalArgumentException(entry + "seeds_per_plant must be at least 1, got " + seedsPerPlant);
        }
        int minutesPerGeneration = get(root, "minutes_per_generation", entry).getAsInt();
        if (minutesPerGeneration < 1) {
            throw new IllegalArgumentException(entry + "minutes_per_generation must be at least 1, got "
                    + minutesPerGeneration);
        }
        List<Method> methods = new ArrayList<>();
        for (JsonElement element : get(root, "methods", entry).getAsJsonArray()) {
            JsonObject method = element.getAsJsonObject();
            String id = get(method, "id", entry + "method: ").getAsString();
            String of = entry + "method " + id + ": ";
            String pollinated = get(method, "pollination", of).getAsString();
            Pollination pollination = switch (pollinated) {
                case "self" -> Pollination.SELF;
                case "wind" -> Pollination.WIND;
                default -> throw new IllegalArgumentException(
                        of + "pollination must be \"self\" or \"wind\", got \"" + pollinated + "\"");
            };
            int plants = get(method, "plants", of).getAsInt();
            if (plants < 1) {
                throw new IllegalArgumentException(of + "plants must be at least 1, got " + plants);
            }
            int minutesLimit = get(method, "minutes_limit", of).getAsInt();
            // A limit shorter than one generation would never be hit: the stage could not fail, and could not stop.
            if (minutesLimit < minutesPerGeneration) {
                throw new IllegalArgumentException(of + "minutes_limit must be at least one generation ("
                        + minutesPerGeneration + " min), got " + minutesLimit);
            }
            methods.add(new Method(id, pollination, plants, minutesLimit));
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(entry + "no methods");
        }
        return new Scenario(pack, get(root, "seed", entry).getAsLong(), runs, seedsPerPlant, minutesPerGeneration,
                methods);
    }

    private static PackData pack(JsonElement pack, Path directory, String entry) {
        if (pack.isJsonPrimitive()) {
            Path file = directory.resolve(pack.getAsString());
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return PackData.parse(reader);
            } catch (IOException e) {
                throw new IllegalArgumentException(entry + "cannot read pack data " + file + ": " + e.getMessage(), e);
            }
        }
        return PackData.parse(new StringReader(pack.toString()));
    }

    private static JsonElement get(JsonObject json, String name, String entry) {
        JsonElement value = json.get(name);
        if (value == null) {
            throw new IllegalArgumentException(entry + "missing key " + name);
        }
        return value;
    }

    /** Every variety bred by every method. The same scenario gives the same outcomes. */
    static List<Outcome> run(Scenario scenario) {
        List<Outcome> outcomes = new ArrayList<>();
        for (Variety variety : scenario.pack().varieties().values()) {
            for (Method method : scenario.methods()) {
                // One generator per outcome: adding a variety or a method leaves the others' numbers alone.
                Random random = new Random(scenario.seed());
                List<OptionalInt> runs = new ArrayList<>();
                for (int run = 0; run < scenario.runs(); run++) {
                    runs.add(generations(scenario, variety, method, random));
                }
                outcomes.add(new Outcome(variety.id(), method, scenario.minutesPerGeneration(), runs));
            }
        }
        return outcomes;
    }

    /**
     * Breeds one line from a field of wild plants and counts the generations until a sown plant
     * lies in the variety's region. Empty when the method's time limit is not enough.
     */
    static OptionalInt generations(Scenario scenario, Variety variety, Method method, RandomGenerator random) {
        Breeding breeding = new Breeding(scenario.pack());
        List<Individual> field = new ArrayList<>();
        for (int i = 0; i < method.plants(); i++) {
            field.add(breeding.wild(variety.species(), Sex.NONE));
        }
        int limit = scenario.generationLimit(method);
        for (int generation = 0; ; generation++) {
            if (reached(field, variety)) {
                return OptionalInt.of(generation);
            }
            if (generation == limit) {
                return OptionalInt.empty();
            }
            field = sown(harvest(breeding, field, method.pollination(), scenario.seedsPerPlant(), random),
                    variety, method.plants());
        }
    }

    /**
     * The seeds of a ripe field, plant by plant in field order: each a cross of the plant with
     * its father, the plant itself or, by wind, another plant of the field at random.
     */
    static List<Individual> harvest(
            Breeding breeding, List<Individual> field, Pollination pollination, int seedsPerPlant, RandomGenerator random) {
        List<Individual> seeds = new ArrayList<>();
        for (int mother = 0; mother < field.size(); mother++) {
            for (int i = 0; i < seedsPerPlant; i++) {
                int father = mother;
                if (pollination == Pollination.WIND && field.size() > 1) {
                    // One of the others: skip the mother's own place.
                    father = random.nextInt(field.size() - 1);
                    if (father >= mother) {
                        father++;
                    }
                }
                seeds.add(breeding.cross(field.get(mother), field.get(father), Environment.NONE, random));
            }
        }
        return seeds;
    }

    /**
     * The seeds sown next: the best ones by their expressed values, as many as the field holds.
     * Equal seeds keep harvest order.
     */
    static List<Individual> sown(List<Individual> seeds, Variety variety, int plants) {
        List<Individual> ranked = new ArrayList<>(seeds);
        ranked.sort(Comparator.comparingInt((Individual one) -> rank(one, variety)).reversed());
        return List.copyOf(ranked.subList(0, Math.min(plants, ranked.size())));
    }

    /**
     * How close a seed is to the variety by what the breeder sees, its expressed values: every
     * value counted up to its threshold, so a surplus in one trait does not pay for another, and
     * every value loses what it has above its "not above" bound, so selection leads a trait down
     * to the bound and holds it there.
     */
    private static int rank(Individual individual, Variety variety) {
        int rank = 0;
        for (Map.Entry<String, Integer> threshold : variety.thresholds().entrySet()) {
            rank += Math.min(threshold.getValue(), individual.trait(threshold.getKey()));
        }
        for (Map.Entry<String, Integer> ceiling : variety.ceilings().entrySet()) {
            rank -= Math.max(0, individual.trait(ceiling.getKey()) - ceiling.getValue());
        }
        return rank;
    }

    /**
     * Whether the line has reached the variety: harvesting one of the sown plants gives its
     * product, so that plant is of the variety's species, expresses every threshold and no value
     * above a "not above" bound.
     */
    static boolean reached(List<Individual> field, Variety variety) {
        return field.stream().anyMatch(plant -> plant.species().equals(variety.species())
                && variety.thresholds().entrySet().stream()
                        .allMatch(threshold -> plant.trait(threshold.getKey()) >= threshold.getValue())
                && variety.ceilings().entrySet().stream()
                        .allMatch(ceiling -> plant.trait(ceiling.getKey()) <= ceiling.getValue()));
    }

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args[0]);
        Scenario scenario;
        List<Outcome> outcomes;
        try (Reader json = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            scenario = parse(json, file.toAbsolutePath().getParent());
            outcomes = run(scenario);
        } catch (RuntimeException e) {
            System.err.println("Breeding simulation cannot read " + file + ": " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("Breeding simulation (" + file + "):");
        outcomes.forEach(outcome -> System.out.println("  " + outcome.line()));
        List<RequirementOutcome> requirements = requirements(scenario, outcomes);
        requirements.forEach(requirement -> System.out.println("  " + requirement.line()));
        // A list for the author of pack data: gaps are allowed and fail nothing.
        System.out.println("Gaps between varieties:");
        VarietyGaps.lines(scenario.pack()).forEach(gap -> System.out.println("  " + gap));
        List<String> outOfReach = requirements.stream().flatMap(requirement -> requirement.failure().stream()).toList();
        if (!outOfReach.isEmpty()) {
            System.err.println("Breeding simulation failed, requirements out of reach:");
            outOfReach.forEach(failure -> System.err.println("  " + failure));
        }
        List<String> failures = outcomes.stream().flatMap(outcome -> outcome.failure().stream()).toList();
        if (!failures.isEmpty()) {
            System.err.println("Breeding simulation failed:");
            failures.forEach(failure -> System.err.println("  " + failure));
            System.exit(1);
        }
        if (!outOfReach.isEmpty()) {
            System.exit(1);
        }
    }
}
