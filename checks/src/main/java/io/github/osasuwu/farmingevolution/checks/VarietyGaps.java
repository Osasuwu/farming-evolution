package io.github.osasuwu.farmingevolution.checks;

import io.github.osasuwu.farmingevolution.breeding.ChromosomeType;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Species;
import io.github.osasuwu.farmingevolution.breeding.Variety;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * The gaps between the varieties of every species: the trait values at which a plant fits no
 * variety and gives the unsorted product of its species. Gaps are allowed, so this is a list
 * for the author of pack data and never a failure.
 *
 * <p>A gap is a region written like the region of a variety: a range of values for each trait
 * it narrows. The gaps of a species do not overlap and together hold every plant without a
 * variety. A trait that no variety of the species bounds does not split anything and is never
 * named.
 */
final class VarietyGaps {
    private VarietyGaps() {
    }

    /** One line for every gap of every species, in pack data order; a species without gaps says so. */
    static List<String> lines(PackData pack) {
        List<String> lines = new ArrayList<>();
        for (Species species : pack.species().values()) {
            List<Variety> varieties = pack.varieties().values().stream()
                    .filter(variety -> variety.species().equals(species.id())).toList();
            String of = "species " + species.id() + ": ";
            if (varieties.isEmpty()) {
                lines.add(of + "no varieties");
                continue;
            }
            List<ChromosomeType.Trait> traits = species.wild().keySet().stream()
                    .filter(id -> varieties.stream().anyMatch(
                            variety -> variety.thresholds().containsKey(id) || variety.ceilings().containsKey(id)))
                    .map(id -> (ChromosomeType.Trait) pack.chromosomes().get(id)).toList();
            List<int[][]> gaps = gaps(traits, varieties);
            if (gaps.isEmpty()) {
                lines.add(of + "no gaps");
            }
            for (int[][] gap : gaps) {
                List<String> ranges = new ArrayList<>();
                for (int i = 0; i < traits.size(); i++) {
                    ChromosomeType.Trait trait = traits.get(i);
                    if (gap[i][0] != trait.min() || gap[i][1] != trait.max()) {
                        ranges.add(trait.id() + " " + (gap[i][0] == gap[i][1] ? "" + gap[i][0] : gap[i][0] + ".." + gap[i][1]));
                    }
                }
                lines.add(of + "no variety for " + String.join(", ", ranges));
            }
        }
        return lines;
    }

    /**
     * The regions no variety covers; a region holds the lowest and the highest value of every
     * trait. The bounds of the varieties cut each trait's range into pieces, a cell takes one
     * piece of every trait, and a cell lies inside a variety whole or not at all.
     */
    private static List<int[][]> gaps(List<ChromosomeType.Trait> traits, List<Variety> varieties) {
        List<int[][]> pieces = new ArrayList<>();
        for (ChromosomeType.Trait trait : traits) {
            // Where a piece starts: at the range's min, at a "not below" bound, right after a "not above" one.
            TreeSet<Integer> starts = new TreeSet<>(List.of(trait.min()));
            for (Variety variety : varieties) {
                if (variety.thresholds().containsKey(trait.id())) {
                    starts.add(variety.thresholds().get(trait.id()));
                }
                if (variety.ceilings().containsKey(trait.id())) {
                    starts.add(variety.ceilings().get(trait.id()) + 1);
                }
            }
            List<int[]> ofTrait = new ArrayList<>();
            for (int start : starts) {
                Integer next = starts.higher(start);
                ofTrait.add(new int[] {start, next == null ? trait.max() : next - 1});
            }
            pieces.add(ofTrait.toArray(int[][]::new));
        }

        List<int[][]> gaps = new ArrayList<>();
        int[] cell = new int[traits.size()];
        while (true) {
            int[][] region = new int[traits.size()][];
            for (int i = 0; i < cell.length; i++) {
                region[i] = pieces.get(i)[cell[i]].clone();
            }
            if (varieties.stream().noneMatch(variety -> holds(variety, traits, region))) {
                gaps.add(region);
            }
            // The next cell, the last trait turning fastest.
            int turn = cell.length - 1;
            while (turn >= 0 && ++cell[turn] == pieces.get(turn).length) {
                cell[turn--] = 0;
            }
            if (turn < 0) {
                break;
            }
        }
        // Fewer, larger regions: cells that differ in one trait only and touch in it are joined,
        // trait by trait from the last to the first.
        for (int trait = traits.size() - 1; trait >= 0; trait--) {
            gaps = joined(gaps, trait);
        }
        gaps.sort(Comparator.comparing(region -> Arrays.stream(region).mapToInt(range -> range[0]).toArray(),
                Arrays::compare));
        return gaps;
    }

    private static boolean holds(Variety variety, List<ChromosomeType.Trait> traits, int[][] region) {
        for (int i = 0; i < region.length; i++) {
            String trait = traits.get(i).id();
            if (region[i][0] < variety.thresholds().getOrDefault(trait, Integer.MIN_VALUE)
                    || region[i][1] > variety.ceilings().getOrDefault(trait, Integer.MAX_VALUE)) {
                return false;
            }
        }
        return true;
    }

    /** Joins the regions that are the same in every trait but the given one and touch in it. */
    private static List<int[][]> joined(List<int[][]> regions, int trait) {
        List<int[][]> sorted = new ArrayList<>(regions);
        sorted.sort(Comparator.comparingInt(region -> region[trait][0]));
        List<int[][]> joined = new ArrayList<>();
        for (int[][] region : sorted) {
            int[][] before = joined.stream().filter(one -> one[trait][1] + 1 == region[trait][0]
                    && sameBut(one, region, trait)).findFirst().orElse(null);
            if (before == null) {
                joined.add(region);
            } else {
                before[trait][1] = region[trait][1];
            }
        }
        return joined;
    }

    private static boolean sameBut(int[][] first, int[][] second, int trait) {
        for (int i = 0; i < first.length; i++) {
            if (i != trait && !Arrays.equals(first[i], second[i])) {
                return false;
            }
        }
        return true;
    }
}
