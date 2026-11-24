package io.github.osasuwu.farmingevolution.checks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
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
 * not reach its variety within the generation limit fails the check pipeline. So does a
 * raw-material requirement that a method cannot meet: see {@link #requirements}. The gaps
 * between the varieties are printed and fail nothing: see {@link VarietyGaps}.
 *
 * <p>The model. A line starts from two wild individuals of the variety's species. Every
 * generation the two parents give the method's number of offspring, and the two best of them
 * (the best female and the best male of a two-sexed species) become the next parents; the old
 * parents are not kept. The line has reached the variety once both parents express every
 * threshold. Methods differ in what the breeder sees when ranking the offspring.
 *
 * <p>A threshold is either bound of the variety's region. A parent that expresses a value above
 * a "not above" bound has not reached the variety, and the ranking leads a trait to the region
 * from both sides: up to a "not below" bound and down to a "not above" one.
 */
public final class BreedingSimulation {
    /** What a selection method lets the breeder see of an individual. */
    enum Sight {
        /** Only the expressed value of a trait: selection without an analyzer. */
        EXPRESSED,
        /** Both alleles of a trait: crossing with an analyzer. */
        ALLELES
    }

    /**
     * A selection method as the scenario describes it.
     *
     * @param offspring            how many offspring one generation gives
     * @param minutesPerGeneration game time one generation takes, in minutes of play
     */
    record Method(String id, Sight sight, int offspring, int minutesPerGeneration) {
    }

    /**
     * What to simulate: every variety of the pack by every method.
     *
     * @param runs            how many independent lines are bred for each variety and method
     * @param generationLimit the most generations a line may take
     */
    record Scenario(PackData pack, long seed, int runs, int generationLimit, List<Method> methods) {
    }

    /**
     * The lines of one variety bred by one method.
     *
     * @param runs for every line, the generations it took, or empty when it did not get there
     */
    record Outcome(String variety, Method method, int generationLimit, List<OptionalInt> runs) {
        String line() {
            List<Integer> reached = runs.stream().filter(OptionalInt::isPresent).map(OptionalInt::getAsInt).sorted().toList();
            String text = "variety " + variety + ", method " + method.id() + ": ";
            if (!reached.isEmpty()) {
                text += "median " + time(reached.get(reached.size() / 2))
                        + ", worst " + time(reached.get(reached.size() - 1)) + "; ";
            }
            return text + reached.size() + " of " + runs.size() + " lines reached the thresholds within "
                    + generationLimit + " generations";
        }

        /** Why this outcome fails the pipeline, or empty when every line reached the variety. */
        Optional<String> failure() {
            long missed = runs.stream().filter(OptionalInt::isEmpty).count();
            return missed == 0 ? Optional.empty() : Optional.of("variety " + variety + ", method " + method.id() + ": "
                    + missed + " of " + runs.size() + " lines did not reach the thresholds within "
                    + generationLimit + " generations");
        }

        private String time(int generations) {
            return generations + " generations (" + generations * method.minutesPerGeneration() + " min)";
        }
    }

    /**
     * How one method gets the raw material a requirement asks for.
     *
     * @param passing   the varieties that pass the requirement, in pack data order
     * @param reachedBy the lines of the passing variety that the method reaches soonest, or
     *                  empty when it reaches none of them with every line
     */
    record RequirementOutcome(
            String requirement, Method method, int generationLimit, List<String> passing, Optional<Outcome> reachedBy) {
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
                    + generationLimit + " generations");
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
                reached.add(new RequirementOutcome(requirement.id(), method, scenario.generationLimit(), passing,
                        Optional.ofNullable(soonest)));
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
     * Reads a scenario. Throws IllegalArgumentException for one that would simulate nothing or
     * that lacks a key, and PackDataException for broken pack data.
     */
    static Scenario parse(Reader json) {
        JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
        String entry = "scenario: ";
        PackData pack = PackData.parse(new StringReader(get(root, "pack", entry).toString()));
        if (pack.varieties().isEmpty()) {
            throw new IllegalArgumentException(entry + "pack data has no varieties to breed");
        }
        int runs = get(root, "runs", entry).getAsInt();
        if (runs < 1) {
            throw new IllegalArgumentException(entry + "runs must be at least 1, got " + runs);
        }
        int generationLimit = get(root, "generation_limit", entry).getAsInt();
        if (generationLimit < 1) {
            throw new IllegalArgumentException(entry + "generation_limit must be at least 1, got " + generationLimit);
        }
        List<Method> methods = new ArrayList<>();
        for (JsonElement element : get(root, "methods", entry).getAsJsonArray()) {
            JsonObject method = element.getAsJsonObject();
            String id = get(method, "id", entry + "method: ").getAsString();
            String of = entry + "method " + id + ": ";
            String sees = get(method, "sees", of).getAsString();
            Sight sight = switch (sees) {
                case "expressed" -> Sight.EXPRESSED;
                case "alleles" -> Sight.ALLELES;
                default -> throw new IllegalArgumentException(
                        of + "sees must be \"expressed\" or \"alleles\", got \"" + sees + "\"");
            };
            methods.add(new Method(id, sight, get(method, "offspring", of).getAsInt(),
                    get(method, "minutes_per_generation", of).getAsInt()));
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(entry + "no methods");
        }
        return new Scenario(pack, get(root, "seed", entry).getAsLong(), runs, generationLimit, methods);
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
                    runs.add(generations(scenario.pack(), variety, method, scenario.generationLimit(), random));
                }
                outcomes.add(new Outcome(variety.id(), method, scenario.generationLimit(), runs));
            }
        }
        return outcomes;
    }

    /**
     * Breeds one line from wild founders and counts the generations until both parents express
     * the variety. Empty when the limit is not enough or the line cannot go on.
     */
    static OptionalInt generations(
            PackData pack, Variety variety, Method method, int generationLimit, RandomGenerator random) {
        Breeding breeding = new Breeding(pack);
        boolean twoSexed = pack.species(variety.species()).twoSexed();
        List<Individual> parents = twoSexed
                ? List.of(breeding.wild(variety.species(), Sex.FEMALE), breeding.wild(variety.species(), Sex.MALE))
                : List.of(breeding.wild(variety.species(), Sex.NONE), breeding.wild(variety.species(), Sex.NONE));
        for (int generation = 0; ; generation++) {
            if (reached(parents, variety)) {
                return OptionalInt.of(generation);
            }
            if (generation == generationLimit) {
                return OptionalInt.empty();
            }
            List<Individual> offspring = new ArrayList<>();
            for (int i = 0; i < method.offspring(); i++) {
                offspring.add(breeding.cross(parents.get(0), parents.get(1), Environment.NONE, random));
            }
            parents = parents(offspring, variety, method.sight(), twoSexed);
            if (parents.isEmpty()) {
                return OptionalInt.empty();
            }
        }
    }

    /**
     * The two offspring that breed next: the best two by what the method sees, or the best
     * female and the best male. Equal ones keep birth order. Empty when there is no pair.
     */
    static List<Individual> parents(List<Individual> offspring, Variety variety, Sight sight, boolean twoSexed) {
        List<Individual> ranked = new ArrayList<>(offspring);
        ranked.sort(Comparator.comparingInt((Individual one) -> rank(one, variety, sight)).reversed());
        if (!twoSexed) {
            return ranked.size() < 2 ? List.of() : List.copyOf(ranked.subList(0, 2));
        }
        Optional<Individual> female = ranked.stream().filter(one -> one.sex() == Sex.FEMALE).findFirst();
        Optional<Individual> male = ranked.stream().filter(one -> one.sex() == Sex.MALE).findFirst();
        return female.isPresent() && male.isPresent() ? List.of(female.get(), male.get()) : List.of();
    }

    /**
     * How close an individual is to the variety as the method sees it: every visible value
     * counted up to its threshold, so a surplus in one trait does not pay for another.
     */
    private static int rank(Individual individual, Variety variety, Sight sight) {
        int rank = 0;
        for (Map.Entry<String, Integer> threshold : variety.thresholds().entrySet()) {
            Chromosome chromosome = individual.genome().get(threshold.getKey());
            rank += Math.min(threshold.getValue(), ((Allele.Numeric) chromosome.active()).value());
            if (sight == Sight.ALLELES) {
                rank += Math.min(threshold.getValue(), ((Allele.Numeric) chromosome.inactive()).value());
            }
        }
        // The other side of the region: every visible value loses what it has above its
        // "not above" bound, so selection leads a trait down to the bound and holds it there.
        for (Map.Entry<String, Integer> ceiling : variety.ceilings().entrySet()) {
            Chromosome chromosome = individual.genome().get(ceiling.getKey());
            rank -= Math.max(0, ((Allele.Numeric) chromosome.active()).value() - ceiling.getValue());
            if (sight == Sight.ALLELES) {
                rank -= Math.max(0, ((Allele.Numeric) chromosome.inactive()).value() - ceiling.getValue());
            }
        }
        return rank;
    }

    /**
     * Whether the line has reached the variety: harvesting either parent gives its product, so
     * each is of the variety's species and expresses every threshold.
     */
    static boolean reached(List<Individual> parents, Variety variety) {
        // A parent that expresses a value above a "not above" bound is outside the region.
        for (Individual parent : parents) {
            for (Map.Entry<String, Integer> ceiling : variety.ceilings().entrySet()) {
                if (parent.trait(ceiling.getKey()) > ceiling.getValue()) {
                    return false;
                }
            }
        }
        return parents.stream().allMatch(parent -> parent.species().equals(variety.species())
                && variety.thresholds().entrySet().stream()
                        .allMatch(threshold -> parent.trait(threshold.getKey()) >= threshold.getValue()));
    }

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args[0]);
        Scenario scenario;
        List<Outcome> outcomes;
        try (Reader json = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            scenario = parse(json);
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
