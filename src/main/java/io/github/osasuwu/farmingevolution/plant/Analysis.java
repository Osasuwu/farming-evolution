package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.CrossOdds;
import io.github.osasuwu.farmingevolution.breeding.Distribution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What the analyzer tells the player, as lines of chat. Nothing here computes odds: the lines
 * are made from the {@link CrossOdds} the engine reports, the same object the harvest rolls
 * from ({@link PlantBlockEntity.Parents}).
 *
 * <p>Every line is a translatable text whose fallback is the English text, so a server without
 * the mod's language file shows it too.
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
        lines.add(text(what, "seed".equals(what) ? "Seed, alleles (expressed | carried):"
                : "Plant, alleles (expressed | carried):"));
        individual.genome().forEach((type, chromosome) -> lines.add(text("chromosome", "  %s: %s | %s",
                chromosomeName(type), allele(chromosome.active()), allele(chromosome.inactive()))));
        lines.add(text("product", "  gives %s",
                new ItemStack(FarmingEvolution.PRODUCTS.of(individual)).getHoverName()));
        return lines;
    }

    /** Whose pollen a plant was pollinated with, or that it pollinates itself. */
    static Component pollinatedBy(PlantBlockEntity.Parents parents) {
        return parents.first().equals(parents.second())
                ? text("self", "  pollinates itself")
                : text("pollinated", "  pollinated by %s", brief(parents.second()));
    }

    /**
     * The odds of the offspring of a pair, one line for every chromosome. An outcome is the
     * two alleles the offspring receives; which of two values of a trait is expressed is a coin
     * toss, so the two orders of a pair are one outcome here, with their chances added.
     */
    static List<Component> odds(String header, String fallback, CrossOdds odds) {
        List<Component> lines = new ArrayList<>();
        lines.add(text(header, fallback));
        odds.chromosomes().forEach((type, distribution) ->
                lines.add(text("odds", "  %s: %s", chromosomeName(type), outcomes(distribution))));
        if (!odds.sex().equals(Distribution.certain(Sex.NONE))) {
            lines.add(text("sex", "  sex: %s", odds.sex().odds().entrySet().stream()
                    .map(entry -> entry.getKey().name().toLowerCase(Locale.ROOT) + " " + percent(entry.getValue()))
                    .collect(Collectors.joining(", "))));
        }
        return lines;
    }

    /** The outcomes of one chromosome, the likeliest first: {@code 9 & 3 64.0%, ...}. */
    static String outcomes(Distribution<Chromosome> distribution) {
        Map<String, Double> pairs = new LinkedHashMap<>();
        distribution.odds().forEach((chromosome, chance) -> pairs.merge(pair(chromosome), chance, Double::sum));
        return pairs.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> entry.getKey() + " " + percent(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    /** One line naming an individual: its alleles, chromosome by chromosome. */
    static Component brief(Individual individual) {
        return Component.literal(individual.genome().entrySet().stream()
                .map(entry -> entry.getKey() + " " + allele(entry.getValue().active())
                        + " | " + allele(entry.getValue().inactive()))
                .collect(Collectors.joining(", ")));
    }

    static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(KEY + key, fallback, args);
    }

    /** A pair of alleles without their order: the higher value or the first id comes first. */
    private static String pair(Chromosome chromosome) {
        Allele a = chromosome.active();
        Allele b = chromosome.inactive();
        boolean swap = a instanceof Allele.Numeric x && b instanceof Allele.Numeric y
                ? x.value() < y.value()
                : allele(a).compareTo(allele(b)) > 0;
        return swap ? allele(b) + " & " + allele(a) : allele(a) + " & " + allele(b);
    }

    private static String allele(Allele allele) {
        return switch (allele) {
            case Allele.Numeric numeric -> String.valueOf(numeric.value());
            case Allele.Named named -> named.id();
        };
    }

    private static Component chromosomeName(String type) {
        return Component.translatableWithFallback("chromosome." + FarmingEvolution.MOD_ID + "." + type, type);
    }

    private static String percent(double chance) {
        return String.format(Locale.ROOT, "%.1f%%", chance * 100);
    }
}
