package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Selection;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.Readout;
import io.github.osasuwu.farmingevolution.store.SeedStoreBlockEntity;
import io.github.osasuwu.farmingevolution.store.SeedStoreView;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Entry;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Filter;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Key;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Sort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The first hour sees only what a plant expresses (issue #121, slice 1 of #104): the goggles,
 * the seed tooltip, the tooltip of a cell of the seed store and its sort show the species and
 * the expressed value of each trait; the carried allele is shown by no number, colour or mark,
 * and no legend of two alleles stands under them. Every genome here is a heterozygote, so a
 * shown carried allele would be a value the expressed one is not.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExpressedOnlyGameTests {
    private static final BlockPos PLANT = new BlockPos(1, 2, 1);

    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("5c1d7e2a-3b84-4f09-a6d2-81e4c07b9a35"), "fe-expressed");

    /** The colour the carried allele was shown in before #121 (issue #105); nothing may wear it now. */
    private static final TextColor GOLD = TextColor.fromLegacyFormat(ChatFormatting.GOLD);

    private ExpressedOnlyGameTests() {
    }

    /**
     * The panel of the plant looked at shows, for a fiber_flax | wild_flax plant of strength
     * 7 | 4, a line per chromosome with the expressed value alone, then the product and the
     * ripeness: no 4, no wild_flax, nothing in the carried allele's colour, no legend.
     */
    @GameTest(template = "empty")
    public static void plantInGogglesShowsOnlyTheExpressedValueOfEachChromosome(GameTestHelper helper) {
        Individual genome = heterozygote(7, 4);
        plant(helper, genome);
        String product = new ItemStack(FarmingEvolution.PRODUCTS.of(genome)).getHoverName().getString();

        List<Component> panel = Readout.plantPanel(plantAt(helper));
        List<String> expected = List.of("species: fiber_flax", "fiber_strength: 7", "gives " + product,
                "growing: stage 1 of 8");
        if (!strings(panel).equals(expected)) {
            throw new GameTestAssertException("Expected the panel " + expected + ", got " + strings(panel));
        }
        expectNoCarriedColour(panel);
        helper.succeed();
    }

    /**
     * With the goggles on, the tooltip of a fiber_flax | wild_flax seed of strength 7 | 4 is
     * its name, its species and its expressed strength, and nothing else.
     */
    @GameTest(template = "empty")
    public static void seedTooltipInGogglesShowsSpeciesAndOnlyExpressedValues(GameTestHelper helper) {
        ItemStack seed = seed(heterozygote(7, 4), 1);
        Player player = goggled(helper);
        List<Component> tooltip = tooltip(helper, seed, player);
        List<String> expected = List.of(seed.getHoverName().getString(), "species: fiber_flax", "fiber_strength: 7");
        if (!strings(tooltip).equals(expected)) {
            throw new GameTestAssertException("Expected the tooltip " + expected + ", got " + strings(tooltip));
        }
        expectNoCarriedColour(tooltip);
        helper.succeed();
    }

    /**
     * The tooltip of a cell of the seed store, without the goggles: the name, the species and
     * the expressed strength, then the count and the condition. With the goggles the genome is
     * there once, not twice. Two cells of genomes that differ only in what they carry
     * (strength 7 | 4 and 7 | 9, species fiber_flax | wild_flax and fiber_flax | oil_flax) read
     * the same: no mark tells the lines apart.
     */
    @GameTest(template = "empty")
    public static void storeCellTooltipShowsSpeciesAndOnlyExpressedValues(GameTestHelper helper) {
        Individual first = heterozygote(7, 4);
        Individual second = first
                .with(Breeding.SPECIES, new Chromosome(new Allele.Named("fiber_flax", true), new Allele.Named("oil_flax", true)))
                .with("fiber_strength", new Chromosome(new Allele.Numeric(7), new Allele.Numeric(9)));
        List<Entry> entries = SeedStoreView.entries(List.of(seed(first, 3), seed(second, 3)));
        if (entries.size() != 2) {
            throw new GameTestAssertException("Expected two cells for two genomes, got " + entries.size());
        }
        Selection all = selection("all");
        Selection hold = selection("hold");
        Player bare = new FakePlayer(helper.getLevel(), PROFILE);
        String name = entries.getFirst().stack().getHoverName().getString();

        List<Component> passing = cellTooltip(helper, entries.getFirst(), bare, all);
        List<String> expectedPassing = List.of(name, "species: fiber_flax", "fiber_strength: 7", "Stored: 3",
                "Passes the condition", "Click: a stack · Right-click: half · Shift-click: to inventory");
        if (!strings(passing).equals(expectedPassing)) {
            throw new GameTestAssertException("Expected the cell tooltip " + expectedPassing + ", got " + strings(passing));
        }
        expectNoCarriedColour(passing);
        List<String> held = strings(cellTooltip(helper, entries.getFirst(), bare, hold));
        if (!held.get(4).equals("Held by the condition")) {
            throw new GameTestAssertException("Expected the seeds held by the condition hold, got " + held);
        }

        Player goggled = goggled(helper);
        List<String> withGoggles = strings(cellTooltip(helper, entries.getFirst(), goggled, all));
        if (!withGoggles.equals(expectedPassing)) {
            throw new GameTestAssertException("Expected the cell tooltip with goggles " + expectedPassing + ", got " + withGoggles);
        }

        for (Player player : List.of(bare, goggled)) {
            List<String> one = strings(cellTooltip(helper, entries.get(0), player, all));
            List<String> other = strings(cellTooltip(helper, entries.get(1), player, all));
            if (!one.equals(other)) {
                throw new GameTestAssertException("Two genomes that differ only in the carried alleles read apart: "
                        + one + " and " + other);
            }
        }
        helper.succeed();
    }

    /**
     * Genomes that differ only in what they carry stay apart, as stacks and as cells, and with
     * every key and direction they keep the order of their first slots: the carried allele
     * breaks no tie. Each set is tried in both slot orders, so an order by the carried allele,
     * up or down, fails one of them.
     */
    @GameTest(template = "empty")
    public static void storeKeepsGenomesApartAndBreaksNoTieByTheCarriedAllele(GameTestHelper helper) {
        Individual carriesFour = heterozygote(7, 4);
        Individual carriesNine = carriesFour.with("fiber_strength",
                new Chromosome(new Allele.Numeric(7), new Allele.Numeric(9)));
        Individual carriesOil = carriesFour.with(Breeding.SPECIES,
                new Chromosome(new Allele.Named("fiber_flax", true), new Allele.Named("oil_flax", true)));
        List<List<Individual>> pairs = List.of(List.of(carriesFour, carriesNine), List.of(carriesNine, carriesFour),
                List.of(carriesFour, carriesOil), List.of(carriesOil, carriesFour));
        List<Sort> sorts = new ArrayList<>();
        for (boolean descending : List.of(false, true)) {
            sorts.add(new Sort(Key.SPECIES, null, descending));
            sorts.add(new Sort(Key.COUNT, null, descending));
            sorts.add(new Sort(Key.TRAIT, "fiber_strength", descending));
        }
        for (List<Individual> pair : pairs) {
            ItemStack a = seed(pair.get(0), 2);
            ItemStack b = seed(pair.get(1), 2);
            if (ItemStack.isSameItemSameComponents(a, b)) {
                throw new GameTestAssertException("Seeds of " + pair.get(0) + " and " + pair.get(1) + " stack together");
            }
            List<Entry> entries = SeedStoreView.entries(List.of(a, b));
            if (entries.size() != 2) {
                throw new GameTestAssertException("Expected two cells for " + pair + ", got " + entries.size());
            }
            for (Sort sort : sorts) {
                List<Individual> shown = SeedStoreView.view(entries, Filter.NONE, sort).stream().map(Entry::genome).toList();
                if (!shown.equals(pair)) {
                    throw new GameTestAssertException("With " + sort + " the cells of " + pair + " are ordered " + shown);
                }
            }
        }
        helper.succeed();
    }

    /** The climate panel of the goggles is its header and the two numbers of the place, nothing else. */
    @GameTest(template = "empty")
    public static void climatePanelInGogglesShowsOnlyTemperatureAndHumidity(GameTestHelper helper) {
        List<String> origin = strings(Readout.climatePanel(FarmingEvolution.CLIMATE.at(helper.getLevel(), new BlockPos(0, 64, 0))));
        List<String> expected = List.of("Climate here", "temperature: 12.0 °C", "humidity: 50.0 %");
        if (!origin.equals(expected)) {
            throw new GameTestAssertException("Expected the climate panel " + expected + ", got " + origin);
        }
        helper.succeed();
    }

    private static List<Component> cellTooltip(GameTestHelper helper, Entry entry, Player player, Selection selection) {
        return SeedStoreView.tooltip(tooltip(helper, entry.stack(), player), entry, player, selection);
    }

    private static void expectNoCarriedColour(List<Component> lines) {
        for (Component line : lines) {
            List<String> gold = new ArrayList<>();
            line.visit((style, piece) -> {
                if (GOLD.equals(style.getColor()) && !piece.isEmpty()) {
                    gold.add(piece);
                }
                return Optional.empty();
            }, Style.EMPTY);
            if (!gold.isEmpty()) {
                throw new GameTestAssertException("The line " + line.getString() + " shows " + gold
                        + " in the colour of the carried allele");
            }
        }
    }

    private static Selection selection(String id) {
        return SeedStoreBlockEntity.selections().stream().filter(selection -> selection.id().equals(id)).findFirst()
                .orElseThrow(() -> new GameTestAssertException("No condition " + id));
    }

    private static Player goggled(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), PROFILE);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(FarmingEvolution.BREEDER_GOGGLES.get()));
        return player;
    }

    private static List<Component> tooltip(GameTestHelper helper, ItemStack stack, Player player) {
        return stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.NORMAL);
    }

    private static List<String> strings(List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }

    /** fiber_flax over a carried wild_flax, with this strength expressed over a carried one. */
    private static Individual heterozygote(int expressed, int carried) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, new Chromosome(new Allele.Named("fiber_flax", true), new Allele.Named("wild_flax", false)),
                "fiber_strength", new Chromosome(new Allele.Numeric(expressed), new Allele.Numeric(carried))));
    }

    private static ItemStack seed(Individual genome, int count) {
        ItemStack stack = new ItemStack(FarmingEvolution.SEEDS.get(), count);
        stack.set(FarmingEvolution.GENOME, genome);
        return stack;
    }

    private static void plant(GameTestHelper helper, Individual genome) {
        helper.setBlock(PLANT.below(), Blocks.FARMLAND);
        helper.setBlock(PLANT, Blocks.AIR);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(PLANT), Direction.DOWN, seed(genome, 1), Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + PLANT);
        }
    }

    private static PlantBlockEntity plantAt(GameTestHelper helper) {
        if (!(helper.getBlockEntity(PLANT) instanceof PlantBlockEntity plant)) {
            throw new GameTestAssertException("No plant at " + PLANT);
        }
        return plant;
    }
}
