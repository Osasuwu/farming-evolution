package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

/**
 * Outcomes with their odds. The engine rolls by sampling the same objects it reports
 * as odds, so the two cannot drift apart.
 */
public final class Distribution<T> {
    private final Map<T, Double> odds;

    private Distribution(Map<T, Double> odds) {
        odds.values().removeIf(chance -> chance <= 0);
        this.odds = Collections.unmodifiableMap(odds);
    }

    /** Outcomes in the given order; the odds must add up to 1. */
    public static <T> Distribution<T> of(Map<T, Double> odds) {
        return new Distribution<>(new LinkedHashMap<>(odds));
    }

    public static <T> Distribution<T> certain(T outcome) {
        return of(Map.of(outcome, 1.0));
    }

    /** The outcome of two independent rolls put through a rule that may roll again. */
    public static <A, B, R> Distribution<R> combine(
            Distribution<A> first, Distribution<B> second, BiFunction<A, B, Distribution<R>> rule) {
        Map<R, Double> combined = new LinkedHashMap<>();
        first.odds.forEach((a, chanceA) -> second.odds.forEach((b, chanceB) ->
                rule.apply(a, b).odds.forEach((r, chance) ->
                        combined.merge(r, chanceA * chanceB * chance, Double::sum))));
        return new Distribution<>(combined);
    }

    /** Every outcome with its chance, in a stable order. */
    public Map<T, Double> odds() {
        return odds;
    }

    public double chance(T outcome) {
        return odds.getOrDefault(outcome, 0.0);
    }

    public T sample(RandomGenerator random) {
        double roll = random.nextDouble();
        T last = null;
        for (Map.Entry<T, Double> entry : odds.entrySet()) {
            last = entry.getKey();
            roll -= entry.getValue();
            if (roll < 0) {
                return last;
            }
        }
        return last; // the odds added up to a hair under 1
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Distribution<?> that && odds.equals(that.odds);
    }

    @Override
    public int hashCode() {
        return odds.hashCode();
    }

    @Override
    public String toString() {
        return odds.toString();
    }
}
