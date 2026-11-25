package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.climate.Climate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the breeder's goggles show (issue #90), as lines of text: the plant looked at, the
 * genome of a seed and the climate of a place. Never the odds of the offspring: those are the
 * analyzer's ({@link Analysis}).
 *
 * <p>The lines are made wherever the goggles are drawn, on the client; nothing here reads a
 * client class, so the server can make them too (the GameTests do). The colours are the ones
 * the analyzer uses for the same parts: a name of a chromosome or reading in grey, the
 * expressed allele in yellow, the carried one in gold, a product in green.
 */
public final class Readout {
    /** A header: what a block of lines is about. */
    public static final ChatFormatting HEADER = ChatFormatting.AQUA;

    /** The name of a chromosome, a reading or a state, and the words around values. */
    public static final ChatFormatting LABEL = ChatFormatting.GRAY;

    /** The expressed allele, a reading's value. */
    public static final ChatFormatting EXPRESSED = ChatFormatting.YELLOW;

    /** The carried allele. */
    public static final ChatFormatting CARRIED = ChatFormatting.GOLD;

    /** A product, and a hybridization that happens. */
    public static final ChatFormatting PRODUCT = ChatFormatting.GREEN;

    /** Something that does not happen or is missing. */
    public static final ChatFormatting MISSING = ChatFormatting.RED;

    private static final String KEY = "goggles." + FarmingEvolution.MOD_ID + ".";

    private Readout() {
    }

    /**
     * The plant looked at: the species with both alleles, the product a ripe plant gives and
     * how ripe it is. A plant without a genome says so and nothing else.
     */
    public static List<Component> plant(PlantBlockEntity plant) {
        if (plant.genome().isEmpty()) {
            return List.of(text("no_genome", "no genome").withStyle(MISSING));
        }
        Individual genome = FarmingEvolution.BREEDING.restore(plant.genome().get(), replaced -> { });
        List<Component> lines = new ArrayList<>();
        Chromosome species = genome.genome().get(Breeding.SPECIES);
        if (species != null) {
            lines.add(chromosome(Breeding.SPECIES, species));
        }
        lines.add(text("product", "gives %s", product(genome)).withStyle(LABEL));
        lines.add(ripeness(plant.getBlockState()));
        return lines;
    }

    /**
     * What the goggles show of the plant looked at, as its panel in the HUD has it: the lines of
     * {@link #plant} and, under a plant with a genome, the {@link #legend} of its alleles.
     */
    public static List<Component> plantPanel(PlantBlockEntity plant) {
        List<Component> lines = new ArrayList<>(plant(plant));
        if (plant.genome().isPresent()) {
            lines.add(legend());
        }
        return lines;
    }

    /** The genome of a seed: every chromosome with both alleles, the expressed one first. */
    public static List<Component> genome(Individual genome) {
        List<Component> lines = new ArrayList<>();
        FarmingEvolution.BREEDING.restore(genome, replaced -> { }).genome()
                .forEach((type, chromosome) -> lines.add(chromosome(type, chromosome)));
        return lines;
    }

    /** The genome of a seed as its tooltip shows it: the lines of {@link #genome}, then the {@link #legend}. */
    public static List<Component> seed(Individual genome) {
        List<Component> lines = genome(genome);
        lines.add(legend());
        return lines;
    }

    /**
     * What the two values of {@code species: fiber_flax | wild_flax} or {@code fiber_strength:
     * 7 | 4} are (issue #105): two alleles, one from each parent, the expressed one on the left.
     * The words stand where the values stand and wear their colours: {@code expressed} in the
     * colour of the expressed allele, {@code carried} in that of the carried one. The goggles
     * (seed tooltip, plant panel), the seed store and the analyzer show this same line.
     */
    public static Component legend() {
        return text("legend", "alleles: %s | %s, one from each parent",
                text("expressed", "expressed").withStyle(EXPRESSED),
                text("carried", "carried").withStyle(CARRIED)).withStyle(LABEL);
    }

    /** The climate of a place: temperature and humidity, one decimal each. */
    public static List<Component> climate(Climate climate) {
        return List.of(
                text("temperature", "temperature: %s °C", value(number(climate.temperature()))).withStyle(LABEL),
                text("humidity", "humidity: %s %%", value(number(climate.humidity()))).withStyle(LABEL));
    }

    /** One chromosome: {@code species: fiber_flax | fiber_flax}. */
    static Component chromosome(String type, Chromosome chromosome) {
        return text("chromosome", "%s: %s | %s", chromosomeName(type), allele(chromosome.active(), true),
                allele(chromosome.inactive(), false)).withStyle(LABEL);
    }

    /** The name of a chromosome, translated where the language file names it. */
    static MutableComponent chromosomeName(String type) {
        return Component.translatableWithFallback("chromosome." + FarmingEvolution.MOD_ID + "." + type, type)
                .withStyle(LABEL);
    }

    /** An allele, in the colour of an expressed or a carried one. */
    static MutableComponent allele(Allele allele, boolean expressed) {
        return Component.literal(alleleText(allele)).withStyle(expressed ? EXPRESSED : CARRIED);
    }

    static String alleleText(Allele allele) {
        return switch (allele) {
            case Allele.Numeric numeric -> String.valueOf(numeric.value());
            case Allele.Named named -> named.id();
        };
    }

    /** The name of the product a ripe plant of the genome gives, in the product's colour. */
    static MutableComponent product(Individual genome) {
        return new ItemStack(FarmingEvolution.PRODUCTS.of(genome)).getHoverName().copy().withStyle(PRODUCT);
    }

    /** A value of a reading, in the colour of a value. */
    static MutableComponent value(String value) {
        return Component.literal(value).withStyle(EXPRESSED);
    }

    /** A reading with one decimal, the way the goggles and the analyzer write it. */
    static String number(double reading) {
        return String.format(Locale.ROOT, "%.1f", reading);
    }

    private static Component ripeness(BlockState state) {
        if (!(state.getBlock() instanceof PlantBlock block)) {
            return text("growing_unknown", "growing").withStyle(LABEL);
        }
        if (block.isMaxAge(state)) {
            return text("ripe", "ripe").withStyle(PRODUCT);
        }
        return text("growing", "growing: stage %s of %s", value(String.valueOf(block.getAge(state) + 1)),
                value(String.valueOf(block.getMaxAge() + 1))).withStyle(LABEL);
    }

    private static MutableComponent text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(KEY + key, fallback, args);
    }
}
