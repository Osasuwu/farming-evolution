package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.CrossOdds;
import io.github.osasuwu.farmingevolution.breeding.Distribution;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Hybridization;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.climate.Climate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * What the analyzer tells the player, as lines of chat. Nothing here computes odds: the lines
 * are made from the {@link CrossOdds} the engine reports, the same object the harvest rolls
 * from ({@link PlantBlockEntity.Parents}).
 *
 * <p>Every line is a translatable text whose fallback is the English text, so a server without
 * the mod's language file shows it too. The colours are those of the breeder's goggles
 * ({@link Readout}): a header in aqua, names in grey, the expressed allele in yellow, the
 * carried one in gold, a product and a hybridization that happens in green, a refusal and a
 * hybridization that does not happen in red. Colours are styles only: the text of a line is
 * the same with or without them.
 */
final class Analysis {
    private static final String KEY = "message." + FarmingEvolution.MOD_ID + ".analyzer.";

    private Analysis() {
    }

    /**
     * Both alleles of every chromosome of an individual, the expressed one first, and the
     * product it gives when it is a ripe plant.
     *
     * @param what "plant" or "seed"
     */
    static List<Component> individual(String what, Individual individual) {
        List<Component> lines = new ArrayList<>();
        lines.add(header(what, "seed".equals(what) ? "Seed, alleles (expressed | carried):"
                : "Plant, alleles (expressed | carried):"));
        individual.genome().forEach((type, chromosome) -> lines.add(text("chromosome", "  %s: %s | %s",
                Readout.chromosomeName(type), Readout.allele(chromosome.active(), true),
                Readout.allele(chromosome.inactive(), false)).withStyle(Readout.LABEL)));
        lines.add(text("product", "  gives %s", Readout.product(individual)).withStyle(Readout.LABEL));
        return lines;
    }

    /** Whose pollen a plant was pollinated with, or that it pollinates itself. */
    static Component pollinatedBy(PlantBlockEntity.Parents parents) {
        return parents.first().equals(parents.second())
                ? text("self", "  pollinates itself").withStyle(Readout.LABEL)
                : text("pollinated", "  pollinated by %s", brief(parents.second())).withStyle(Readout.LABEL);
    }

    /**
     * The odds of the offspring of a pair, one line for every chromosome. An outcome is the
     * two alleles the offspring receives; which of two values of a trait is expressed is a coin
     * toss, so the two orders of a pair are one outcome here, with their chances added.
     */
    static List<Component> odds(String header, String fallback, CrossOdds odds) {
        List<Component> lines = new ArrayList<>();
        lines.add(header(header, fallback));
        odds.chromosomes().forEach((type, distribution) -> lines.add(text("odds", "  %s: %s",
                Readout.chromosomeName(type), outcomes(distribution)).withStyle(Readout.LABEL)));
        if (!odds.sex().equals(Distribution.certain(Sex.NONE))) {
            lines.add(text("sex", "  sex: %s", odds.sex().odds().entrySet().stream()
                    .map(entry -> entry.getKey().name().toLowerCase(Locale.ROOT) + " " + percent(entry.getValue()))
                    .collect(Collectors.joining(", "))).withStyle(Readout.LABEL));
        }
        return lines;
    }

    /**
     * The climate of the place a pair crosses in, when {@code withHeader}, and for every
     * hybridization of the pair that depends on the climate whether it happens there, in words:
     * the result and the conditions where it happens, the conditions alone where it does not.
     * A pair with no such hybridization is told so. The readings are those the cross reads.
     */
    static List<Component> climate(PlantBlockEntity.Parents parents, boolean withHeader) {
        List<Component> lines = new ArrayList<>();
        Environment environment = parents.environment();
        if (withHeader) {
            lines.add(header("climate", "Climate here: %s °C, humidity %s %%",
                    Readout.value(reading(environment, Climate.TEMPERATURE)),
                    Readout.value(reading(environment, Climate.HUMIDITY))));
        }
        List<Hybridization> rules = FarmingEvolution.BREEDING.hybridizations(parents.first(), parents.second()).stream()
                .filter(rule -> !rule.condition().climate().isEmpty())
                .toList();
        if (rules.isEmpty()) {
            lines.add(text("no_climate_hybrid", "  no hybrid of this pair depends on the climate").withStyle(Readout.LABEL));
        }
        for (Hybridization rule : rules) {
            Component first = species(parents.first());
            Component second = species(parents.second());
            Component needs = Component.literal(needs(rule.condition()));
            lines.add(rule.condition().holds(environment)
                    ? text("hybrid_here", "  %s × %s can give %s here (needs %s)", first, second,
                            Component.literal(rule.result()).withStyle(Readout.PRODUCT), needs).withStyle(Readout.PRODUCT)
                    // The result is not named where the hybridization does not happen: the player
                    // learns where to look, not a species that cannot come up here.
                    : text("no_hybrid_here", "  no hybrid here: %s × %s gives a new species only where %s",
                            first, second, needs).withStyle(Readout.MISSING));
        }
        return lines;
    }

    /** The outcomes of one chromosome, the likeliest first: {@code 9 & 3 64.0%, ...}. */
    static Component outcomes(Distribution<Chromosome> distribution) {
        Map<String, Double> pairs = new LinkedHashMap<>();
        distribution.odds().forEach((chromosome, chance) -> pairs.merge(pair(chromosome), chance, Double::sum));
        MutableComponent line = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Double> entry : pairs.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())).toList()) {
            if (!first) {
                line.append(Component.literal(", ").withStyle(Readout.LABEL));
            }
            first = false;
            line.append(Component.literal(entry.getKey()).withStyle(Readout.EXPRESSED))
                    .append(Component.literal(" " + percent(entry.getValue())).withStyle(ChatFormatting.WHITE));
        }
        return line;
    }

    /** One line naming an individual: its alleles, chromosome by chromosome. */
    static Component brief(Individual individual) {
        return Component.literal(individual.genome().entrySet().stream()
                .map(entry -> entry.getKey() + " " + Readout.alleleText(entry.getValue().active())
                        + " | " + Readout.alleleText(entry.getValue().inactive()))
                .collect(Collectors.joining(", ")));
    }

    static MutableComponent text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(KEY + key, fallback, args);
    }

    /** A line that says what the lines below it are about. */
    static MutableComponent header(String key, String fallback, Object... args) {
        return text(key, fallback, args).withStyle(Readout.HEADER);
    }

    /** The expressed species of an individual, in the colour of an expressed allele. */
    private static Component species(Individual individual) {
        Chromosome species = individual.genome().get(Breeding.SPECIES);
        return Readout.allele(species.active(), true);
    }

    /** The climate conditions of a hybridization: {@code temperature 4–16 °C, humidity ≥ 65 %}. */
    private static String needs(Hybridization.Condition condition) {
        return condition.climate().entrySet().stream()
                .map(entry -> entry.getKey() + " " + range(entry.getValue(), unit(entry.getKey())))
                .collect(Collectors.joining(", "));
    }

    private static String range(Hybridization.Range range, String unit) {
        boolean low = range.min() != Double.NEGATIVE_INFINITY;
        boolean high = range.max() != Double.POSITIVE_INFINITY;
        if (low && high) {
            return bound(range.min()) + "–" + bound(range.max()) + unit;
        }
        return low ? "≥ " + bound(range.min()) + unit : "≤ " + bound(range.max()) + unit;
    }

    private static String unit(String reading) {
        return switch (reading) {
            case Climate.TEMPERATURE -> " °C";
            case Climate.HUMIDITY -> " %";
            default -> "";
        };
    }

    /** A bound of pack data as written there: 20, not 20.0; 2.5 stays 2.5. */
    private static String bound(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String reading(Environment environment, String name) {
        Double value = environment.climate().get(name);
        return value == null ? "?" : Readout.number(value);
    }

    /** A pair of alleles without their order: the higher value or the first id comes first. */
    private static String pair(Chromosome chromosome) {
        Allele a = chromosome.active();
        Allele b = chromosome.inactive();
        boolean swap = a instanceof Allele.Numeric x && b instanceof Allele.Numeric y
                ? x.value() < y.value()
                : Readout.alleleText(a).compareTo(Readout.alleleText(b)) > 0;
        return swap ? Readout.alleleText(b) + " & " + Readout.alleleText(a)
                : Readout.alleleText(a) + " & " + Readout.alleleText(b);
    }

    private static String percent(double chance) {
        return String.format(Locale.ROOT, "%.1f%%", chance * 100);
    }
}
