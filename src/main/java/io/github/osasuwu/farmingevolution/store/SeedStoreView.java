package io.github.osasuwu.farmingevolution.store;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.ChromosomeType;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Selection;
import io.github.osasuwu.farmingevolution.plant.BreederGogglesItem;
import io.github.osasuwu.farmingevolution.plant.Readout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What the window of the seed store lists (issue #91): the stored seeds grouped by genome, each
 * group with how many seeds it holds, narrowed by a {@link Filter} and ordered by a {@link Sort}.
 * The filter and the sort know nothing of each other: any filter goes with any sort.
 *
 * <p>Plain logic over item stacks, no client class: the window runs it on the client, the
 * GameTests on the server.
 *
 * <p>A trait is read as the player sees the plant: by the expressed allele, the way the
 * conditions of the store and the varieties read it. The species, too, is the expressed one.
 */
public final class SeedStoreView {
    private SeedStoreView() {
    }

    /**
     * One genome in the store.
     *
     * @param stack one seed of the genome, as stored: the genome is never rewritten
     * @param count how many seeds of the genome the store holds, over all its slots
     * @param slot the first slot holding the genome: what the window names when it asks the
     *        server for seeds of this genome
     */
    public record Entry(ItemStack stack, int count, int slot) {
        /** The genome, or null for seeds without one. */
        @Nullable
        public Individual genome() {
            return stack.get(FarmingEvolution.GENOME);
        }

        /** The expressed species, or empty for seeds without a genome. */
        public Optional<String> species() {
            Individual genome = genome();
            return genome == null ? Optional.empty() : Optional.of(genome.species());
        }

        /** The expressed value of a numeric trait, or empty when the seed has no such trait. */
        public Optional<Integer> trait(String trait) {
            Individual genome = genome();
            if (genome == null) {
                return Optional.empty();
            }
            Chromosome chromosome = genome.genome().get(trait);
            return chromosome != null && chromosome.active() instanceof Allele.Numeric numeric
                    ? Optional.of(numeric.value()) : Optional.empty();
        }
    }

    /** How a trait filter compares the expressed value with its own. */
    public enum Compare {
        AT_LEAST, EXACTLY, AT_MOST;

        boolean holds(int value, int limit) {
            return switch (this) {
                case AT_LEAST -> value >= limit;
                case EXACTLY -> value == limit;
                case AT_MOST -> value <= limit;
            };
        }
    }

    /**
     * Which genomes the window shows. Both parts are optional and independent.
     *
     * @param species the expressed species a seed must have, or null for any; seeds without a
     *        genome pass only null
     * @param trait the numeric trait compared, or null for no trait filter; seeds without the
     *        trait do not pass a trait filter
     */
    public record Filter(@Nullable String species, @Nullable String trait, Compare compare, int value) {
        public static final Filter NONE = new Filter(null, null, Compare.AT_LEAST, 0);

        public boolean passes(Entry entry) {
            if (species != null && !entry.species().filter(species::equals).isPresent()) {
                return false;
            }
            return trait == null || entry.trait(trait).filter(value -> compare.holds(value, this.value)).isPresent();
        }
    }

    /** What the window orders the genomes by. */
    public enum Key {
        /** The expressed species, by id. */
        SPECIES,
        /** How many seeds the store holds of the genome. */
        COUNT,
        /** The expressed value of the sort's trait. */
        TRAIT
    }

    /**
     * The order of the genomes. Descending turns over the key only; ties are always broken the
     * same way, by the expressed species, then by the expressed value of every numeric trait,
     * then by the count and the first slot, so the order does not jump between two refreshes.
     * The carried allele breaks no tie (issue #121): genomes that differ only in it keep the
     * order of their slots. Seeds without a genome, and with a trait sort the seeds without the
     * trait, go last either way.
     *
     * @param trait the trait of {@link Key#TRAIT}; ignored by the other keys
     */
    public record Sort(Key key, @Nullable String trait, boolean descending) {
        public static final Sort DEFAULT = new Sort(Key.SPECIES, null, false);

        Comparator<Entry> comparator() {
            Comparator<Entry> primary = switch (key) {
                case SPECIES -> Comparator.comparing(entry -> entry.species().orElse(""));
                case COUNT -> Comparator.comparingInt(Entry::count);
                case TRAIT -> Comparator.comparingInt(entry -> trait == null ? 0 : entry.trait(trait).orElse(0));
            };
            if (descending) {
                primary = primary.reversed();
            }
            Comparator<Entry> lastFirst = Comparator.comparing(entry -> missing(this, entry));
            return lastFirst.thenComparing(primary).thenComparing(TIES);
        }
    }

    private static boolean missing(Sort sort, Entry entry) {
        if (entry.genome() == null) {
            return true;
        }
        return sort.key() == Key.TRAIT && (sort.trait() == null || entry.trait(sort.trait()).isEmpty());
    }

    private static final Comparator<Entry> TIES = ties();

    private static Comparator<Entry> ties() {
        Comparator<Entry> ties = Comparator.comparing(entry -> entry.species().orElse(""));
        for (String trait : traits()) {
            ties = ties.thenComparingInt(entry -> entry.trait(trait).orElse(0));
        }
        return ties.thenComparingInt(Entry::count).thenComparingInt(Entry::slot);
    }

    /** The numeric traits of pack data, in its order: what a trait filter or sort can name. */
    public static List<String> traits() {
        List<String> traits = new ArrayList<>();
        for (ChromosomeType type : FarmingEvolution.PACK.chromosomes().values()) {
            if (type instanceof ChromosomeType.Trait trait) {
                traits.add(trait.id());
            }
        }
        return traits;
    }

    /** The lowest and highest value of a numeric trait of pack data. */
    public static int[] range(String trait) {
        if (FarmingEvolution.PACK.chromosomes().get(trait) instanceof ChromosomeType.Trait type) {
            return new int[] {type.min(), type.max()};
        }
        throw new IllegalArgumentException("no numeric trait " + trait);
    }

    /** The species of pack data, by id: what a species filter can name. */
    public static List<String> species() {
        return FarmingEvolution.PACK.species().keySet().stream().sorted().toList();
    }

    /**
     * The stored seeds grouped by genome, in the order of their first slots. Two stacks are of
     * one genome when they are the same item with the same components, as the game stacks them.
     */
    public static List<Entry> entries(List<ItemStack> slots) {
        List<ItemStack> firsts = new ArrayList<>();
        List<int[]> groups = new ArrayList<>();
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int group = -1;
            for (int i = 0; i < firsts.size(); i++) {
                if (ItemStack.isSameItemSameComponents(firsts.get(i), stack)) {
                    group = i;
                    break;
                }
            }
            if (group < 0) {
                firsts.add(stack.copyWithCount(1));
                groups.add(new int[] {slot, stack.getCount()});
            } else {
                groups.get(group)[1] += stack.getCount();
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < firsts.size(); i++) {
            entries.add(new Entry(firsts.get(i), groups.get(i)[1], groups.get(i)[0]));
        }
        return entries;
    }

    /**
     * The tooltip of a cell of the window: the lines of the seed's own tooltip, with the genome
     * under the name as the goggles show it, then how many seeds the store holds, whether they
     * pass the condition and how to take them. With the goggles on, the seed's tooltip has the
     * genome already ({@link BreederGogglesItem#onTooltip}), so it is not added twice.
     *
     * @param itemLines the tooltip of the cell's seed, as the game makes it for this player
     * @param player the player looking, or null when there is none
     */
    public static List<Component> tooltip(List<Component> itemLines, Entry entry, @Nullable Player player,
            Selection selection) {
        List<Component> lines = new ArrayList<>(itemLines);
        Individual genome = entry.genome();
        if (genome != null && (player == null || !BreederGogglesItem.worn(player))) {
            lines.addAll(Math.min(1, lines.size()), Readout.seed(genome));
        }
        lines.add(text("stored", "Stored: %s", Component.literal(String.valueOf(entry.count()))
                .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        lines.add(SeedStoreBlockEntity.passes(selection, entry.stack())
                ? text("passes", "Passes the condition").withStyle(ChatFormatting.GREEN)
                : text("held", "Held by the condition").withStyle(ChatFormatting.RED));
        lines.add(text("take_hint", "Click: a stack · Right-click: half · Shift-click: to inventory")
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static MutableComponent text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("screen." + FarmingEvolution.MOD_ID + ".seed_store." + key, fallback, args);
    }

    /** The entries the filter passes, in the order of the sort. */
    public static List<Entry> view(List<Entry> entries, Filter filter, Sort sort) {
        return entries.stream().filter(filter::passes).sorted(sort.comparator()).toList();
    }
}
