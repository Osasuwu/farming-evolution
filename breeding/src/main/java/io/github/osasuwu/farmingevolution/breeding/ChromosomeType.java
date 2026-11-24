package io.github.osasuwu.farmingevolution.breeding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** What a chromosome may hold and how one of its alleles passes to an offspring. */
public sealed interface ChromosomeType {
    String id();

    /** Reads an allele written in pack data; throws IllegalArgumentException when this type has no such allele. */
    Allele allele(String text);

    /** The alleles an offspring may receive in place of the given one, with their odds. */
    Distribution<Allele> transmit(Allele allele);

    /** A numeric trait: any whole value from min to max. */
    record Trait(String id, int min, int max, double shiftChance) implements ChromosomeType {
        public boolean holds(int value) {
            return value >= min && value <= max;
        }

        @Override
        public Allele allele(String text) {
            int value;
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(text + " is not a whole number");
            }
            if (!holds(value)) {
                throw new IllegalArgumentException(value + " is outside " + min + ".." + max);
            }
            return new Allele.Numeric(value);
        }

        /**
         * The growth rule: a passed value shifts by one with the trait's shift chance, up or down
         * with equal odds, and stays inside min..max. A shift up is how a line passes both parents.
         */
        @Override
        public Distribution<Allele> transmit(Allele allele) {
            int value = ((Allele.Numeric) allele).value();
            Map<Allele, Double> odds = new LinkedHashMap<>();
            odds.put(allele, 1 - shiftChance);
            odds.merge(new Allele.Numeric(Math.min(max, value + 1)), shiftChance / 2, Double::sum);
            odds.merge(new Allele.Numeric(Math.max(min, value - 1)), shiftChance / 2, Double::sum);
            return Distribution.of(odds);
        }
    }

    /** A closed list of named alleles. */
    record Categorical(String id, Map<String, Allele.Named> alleles) implements ChromosomeType {
        public Categorical {
            alleles = Collections.unmodifiableMap(new LinkedHashMap<>(alleles));
        }

        @Override
        public Allele allele(String text) {
            Allele.Named allele = alleles.get(text);
            if (allele == null) {
                throw new IllegalArgumentException("has no allele " + text);
            }
            return allele;
        }

        @Override
        public Distribution<Allele> transmit(Allele allele) {
            return Distribution.certain(allele);
        }
    }
}
