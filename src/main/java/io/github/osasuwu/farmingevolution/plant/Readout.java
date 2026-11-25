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
 * expressed allele in yellow, a product in green. The goggles show the expressed allele alone
 * (issue #121); the carried one, in gold, is the analyzer's, a debug tool.
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
     * The plant looked at: the expressed value of every chromosome, the species first, then
     * the product a ripe plant gives and how ripe it is. A plant without a genome says so and
     * nothing else.
     */
    public static List<Component> plant(PlantBlockEntity plant) {
        if (plant.genome().isEmpty()) {
            return List.of(text("no_genome", "no genome").withStyle(MISSING));
        }
        Individual genome = FarmingEvolution.BREEDING.restore(plant.genome().get(), replaced -> { });
        List<Component> lines = new ArrayList<>(expressed(genome));
        lines.add(text("product", "gives %s", product(genome)).withStyle(LABEL));
        lines.add(ripeness(plant.getBlockState()));
        return lines;
    }

    /**
     * What the goggles show of the plant looked at, as its panel in the HUD has it under its
     * header: the lines of {@link #plant}.
     */
    public static List<Component> plantPanel(PlantBlockEntity plant) {
        return plant(plant);
    }

    /**
     * The genome of a seed as its tooltip shows it: the expressed value of every chromosome, the
     * species first. The carried allele is not shown (issue #121): the first hour sees what a
     * plant expresses, the carried allele is for the microscope of the laboratory.
     */
    public static List<Component> seed(Individual genome) {
        return expressed(FarmingEvolution.BREEDING.restore(genome, replaced -> { }));
    }

    /** One line per chromosome of a restored genome, the species first: {@code fiber_strength: 7}. */
    private static List<Component> expressed(Individual genome) {
        List<Component> lines = new ArrayList<>();
        Chromosome species = genome.genome().get(Breeding.SPECIES);
        if (species != null) {
            lines.add(expressed(Breeding.SPECIES, species));
        }
        genome.genome().forEach((type, chromosome) -> {
            if (!type.equals(Breeding.SPECIES)) {
                lines.add(expressed(type, chromosome));
            }
        });
        return lines;
    }

    /** One chromosome by its expressed allele alone: {@code species: fiber_flax}. */
    private static Component expressed(String type, Chromosome chromosome) {
        return text("expressed_chromosome", "%s: %s", chromosomeName(type), allele(chromosome.active(), true))
                .withStyle(LABEL);
    }

    /** The climate panel of the HUD: its header, then the lines of {@link #climate}. */
    public static List<Component> climatePanel(Climate climate) {
        List<Component> lines = new ArrayList<>();
        lines.add(text("climate_header", "Climate here").withStyle(HEADER));
        lines.addAll(climate(climate));
        return lines;
    }

    /** The climate of a place: temperature and humidity, one decimal each. */
    public static List<Component> climate(Climate climate) {
        return List.of(
                text("temperature", "temperature: %s °C", value(number(climate.temperature()))).withStyle(LABEL),
                text("humidity", "humidity: %s %%", value(number(climate.humidity()))).withStyle(LABEL));
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
