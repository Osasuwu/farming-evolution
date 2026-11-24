package io.github.osasuwu.farmingevolution.plant;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.Locale;

/**
 * How a genome is written into a save and sent over the network. The genome itself is the
 * inheritance engine's {@link Individual}; the engine knows nothing about the game, so the
 * codec lives here.
 *
 * <p>Written form: {@code {sex: "none", chromosomes: {<type id>: {active: A, inactive: A}}}},
 * where an allele of a numeric trait is a whole number and a named allele is
 * {@code {id: "...", dominant: true}}.
 */
public final class GenomeCodec {
    private static final Codec<Sex> SEX = Codec.STRING.comapFlatMap(GenomeCodec::sex, sex -> sex.name().toLowerCase(Locale.ROOT));

    private static final Codec<Allele.Named> NAMED = RecordCodecBuilder.create(named -> named.group(
            Codec.STRING.fieldOf("id").forGetter(Allele.Named::id),
            Codec.BOOL.optionalFieldOf("dominant", false).forGetter(Allele.Named::dominant)
    ).apply(named, Allele.Named::new));

    private static final Codec<Allele> ALLELE = Codec.either(Codec.INT, NAMED).xmap(
            either -> either.map(Allele.Numeric::new, named -> named),
            allele -> switch (allele) {
                case Allele.Numeric numeric -> Either.left(numeric.value());
                case Allele.Named named -> Either.right(named);
            });

    private static final Codec<Chromosome> CHROMOSOME = RecordCodecBuilder.create(chromosome -> chromosome.group(
            ALLELE.fieldOf("active").forGetter(Chromosome::active),
            ALLELE.fieldOf("inactive").forGetter(Chromosome::inactive)
    ).apply(chromosome, Chromosome::new));

    public static final Codec<Individual> INDIVIDUAL = RecordCodecBuilder.create(individual -> individual.group(
            SEX.fieldOf("sex").forGetter(Individual::sex),
            Codec.unboundedMap(Codec.STRING, CHROMOSOME).fieldOf("chromosomes").forGetter(Individual::genome)
    ).apply(individual, Individual::new));

    private GenomeCodec() {
    }

    private static DataResult<Sex> sex(String text) {
        for (Sex sex : Sex.values()) {
            if (sex.name().toLowerCase(Locale.ROOT).equals(text)) {
                return DataResult.success(sex);
            }
        }
        return DataResult.error(() -> "Unknown sex " + text);
    }
}
