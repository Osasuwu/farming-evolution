package io.github.osasuwu.farmingevolution.breeding;

import java.util.function.Predicate;

/**
 * A condition a machine selects seeds or animals by, for instance the seed store of the mod.
 * The engine builds them ({@link Breeding#selections()}), so a machine holds no thresholds of
 * its own: a variety or requirement condition is decided by the same code that picks the
 * variety of a harvest.
 *
 * <p>Ids, also what a save keeps:
 * <ul>
 * <li>{@code hold} — passes nothing;
 * <li>{@code variety/<id>} — the individual gives the product of this variety: it is the
 * narrowest variety it fits ({@link Breeding#variety});
 * <li>{@code requirement/<id>} — its variety passes the raw-material requirement;
 * <li>{@code trait/<chromosome>/<n>} — it expresses the numeric trait at n or above; an
 * individual whose species has no such chromosome does not pass;
 * <li>{@code all} — passes everything.
 * </ul>
 */
public final class Selection {
    private final String id;
    private final Predicate<Individual> test;

    Selection(String id, Predicate<Individual> test) {
        this.id = id;
        this.test = test;
    }

    public String id() {
        return id;
    }

    /** Whether the individual passes the condition. */
    public boolean passes(Individual individual) {
        return test.test(individual);
    }

    @Override
    public String toString() {
        return id;
    }
}
