package io.github.osasuwu.farmingevolution.breeding;

/** One of the two values a chromosome holds. */
public sealed interface Allele {
    /** An allele from a closed list in pack data; a species is one of these. */
    record Named(String id, boolean dominant) implements Allele {
    }

    /** A value of a numeric trait. Not from a list: breeding moves it. */
    record Numeric(int value) implements Allele {
    }
}
