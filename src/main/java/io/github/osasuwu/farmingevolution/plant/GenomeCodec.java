package io.github.osasuwu.farmingevolution.plant;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.logging.LogUtils;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.Locale;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = LogUtils.getLogger();

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

    /**
     * The codec of a genome kept in a save. The save may be older than pack data: what pack
     * data no longer has is replaced with the wild type on reading ({@link Breeding#restore}),
     * and every replacement is a line in the log.
     */
    public static Codec<Individual> saved(Breeding breeding) {
        return INDIVIDUAL.xmap(
                genome -> breeding.restore(genome, replaced -> LOGGER.warn("Genome from a save: {}", replaced)),
                genome -> genome);
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
