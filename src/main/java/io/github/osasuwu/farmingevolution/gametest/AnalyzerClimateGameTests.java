package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The analyzer names the climate of the plant's place and says in words whether a
 * hybridization of the plant's pair that depends on the climate happens there (issue #90).
 * The places are absolute positions in bands of the climate stub, as in {@link OilFlaxGameTests}.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnalyzerClimateGameTests {
    /** 30 °C, humidity 10 %: warm and dry, fiber_flax gives oil_flax here. */
    private static final BlockPos WARM_DRY = new BlockPos(-1500, -60, 2000);

    /** About 11 °C, humidity about 48 %: neither hybridization of the pack happens here. */
    private static final BlockPos NEAR_ORIGIN = new BlockPos(-50, -60, -50);

    /** 12 °C, humidity 90 %: cool and humid, wild_flax gives fiber_flax here. */
    private static final BlockPos COOL_HUMID = new BlockPos(1500, -60, 0);

    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("5e2a7d14-93c6-4f0b-8d1a-6c4b2f8e7a35"), "fe-climate-breeder");

    private AnalyzerClimateGameTests() {
    }

    @GameTest(template = "empty")
    public static void analyzerSaysWhereTheHybridizationOfThePairHappens(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        List<String> warm = strings(analyzeAt(helper, breeder, WARM_DRY, wild("fiber_flax")));
        expect(warm, List.of("Climate here: 30.0 °C, humidity 10.0 %",
                "  fiber_flax × fiber_flax can give oil_flax here (needs temperature ≥ 20 °C, humidity ≤ 35 %)"));

        List<String> mild = strings(analyzeAt(helper, breeder, NEAR_ORIGIN, wild("fiber_flax")));
        expect(mild, List.of("  no hybrid here: fiber_flax × fiber_flax gives a new species only where temperature ≥ 20 °C,"
                + " humidity ≤ 35 %"));
        if (mild.stream().anyMatch(line -> line.startsWith("Climate here: 30"))) {
            throw new GameTestAssertException("The climate of the warm place is shown in the mild one: " + mild);
        }

        List<String> humid = strings(analyzeAt(helper, breeder, COOL_HUMID, wild("wild_flax")));
        expect(humid, List.of("Climate here: 12.0 °C, humidity 90.0 %",
                "  wild_flax × wild_flax can give fiber_flax here (needs temperature 4–16 °C, humidity ≥ 65 %)"));
        List<String> wildMild = strings(analyzeAt(helper, breeder, NEAR_ORIGIN, wild("wild_flax")));
        expect(wildMild, List.of("  no hybrid here: wild_flax × wild_flax gives a new species only where"
                + " temperature 4–16 °C, humidity ≥ 65 %"));
        helper.succeed();
    }

    /** The analyzer's lines tell the parts apart by colour, as the goggles do; a refusal is red. */
    @GameTest(template = "empty")
    public static void analyzerColoursHeadersNamesAllelesAndProduct(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        List<Component> lines = analyzeAt(helper, breeder, NEAR_ORIGIN, wild("fiber_flax"));
        List<String> text = strings(lines);
        Component header = lines.get(text.indexOf("Plant, alleles (expressed | carried):"));
        Component species = lines.get(text.indexOf("  species: fiber_flax | fiber_flax"));
        Component product = lines.stream().filter(line -> line.getString().startsWith("  gives ")).findFirst()
                .orElseThrow(() -> new GameTestAssertException("No product among " + text));
        String productName = product.getString().substring("  gives ".length());
        TextColor headerColor = header.getStyle().getColor();
        TextColor name = BreederGogglesGameTests.colorOf(species, "species");
        TextColor allele = BreederGogglesGameTests.colorOf(species, "fiber_flax");
        TextColor item = BreederGogglesGameTests.colorOf(product, productName);
        List<TextColor> colors = new ArrayList<>();
        colors.add(headerColor);
        colors.add(name);
        colors.add(allele);
        colors.add(item);
        if (colors.contains(null) || colors.stream().distinct().count() != colors.size()) {
            throw new GameTestAssertException("Expected the header, the name, the allele and the product in four colours, got "
                    + headerColor + ", " + name + ", " + allele + ", " + item);
        }
        helper.succeed();
    }

    /**
     * The analyzer explains the two alleles with the legend of the goggles (issue #105): the line
     * stands once, right under the alleles of the plant, and its words wear the colours of the
     * expressed and the carried value.
     */
    @GameTest(template = "empty")
    public static void analyzerShowsTheAlleleLegendOfTheGoggles(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        Map<String, Chromosome> genome = new LinkedHashMap<>(wild("fiber_flax").genome());
        genome.put("fiber_strength", new Chromosome(new Allele.Numeric(7), new Allele.Numeric(4)));
        List<Component> lines = analyzeAt(helper, breeder, NEAR_ORIGIN, new Individual(Sex.NONE, genome));
        List<String> text = strings(lines);
        String legend = "  " + BreederGogglesGameTests.LEGEND;
        int at = text.indexOf(legend);
        if (at < 0 || at != text.lastIndexOf(legend)) {
            throw new GameTestAssertException("Expected the legend of the alleles once among " + text);
        }
        int header = text.indexOf("Plant, alleles (expressed | carried):");
        int strength = text.indexOf("  fiber_strength: 7 | 4");
        if (header < 0 || strength < 0 || !text.get(at + 1).startsWith("  gives ")
                || text.subList(header + 1, at).size() != genome.size()) {
            throw new GameTestAssertException("Expected the legend right after the alleles of the plant, before its product,"
                    + " got " + text);
        }
        BreederGogglesGameTests.expectLegendColours(lines.get(at), lines.get(strength), "7", "4");
        helper.succeed();
    }

    private static void expect(List<String> lines, List<String> expected) {
        List<String> missing = expected.stream().filter(line -> !lines.contains(line)).toList();
        if (!missing.isEmpty()) {
            throw new GameTestAssertException("Expected the lines " + missing + " among " + lines);
        }
    }

    private static List<String> strings(List<Component> lines) {
        return BreederGogglesGameTests.strings(lines);
    }

    /** The wild type of the species, as the engine makes it. */
    private static Individual wild(String species) {
        return FarmingEvolution.BREEDING.wild(species, Sex.NONE);
    }

    /** A player whose chat the test reads, holding an analyzer. */
    private static final class Breeder extends FakePlayer {
        private final List<Component> messages = new ArrayList<>();

        Breeder(GameTestHelper helper) {
            super(helper.getLevel(), PROFILE);
            setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FarmingEvolution.ANALYZER.get()));
        }

        @Override
        public void displayClientMessage(Component message, boolean overlay) {
            messages.add(message);
        }
    }

    /** Plants the genome at the absolute place, uses the analyzer on it and clears the place; returns the lines. */
    private static List<Component> analyzeAt(GameTestHelper helper, Breeder breeder, BlockPos place, Individual genome) {
        ServerLevel level = helper.getLevel();
        BlockPos soil = place.below();
        BlockState before = level.getBlockState(soil);
        level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(place, Blocks.AIR.defaultBlockState());
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(level, place, Direction.DOWN, seed, Direction.UP))
                .consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + place.toShortString());
        }
        breeder.moveTo(place.getX() + 2.5, place.getY(), place.getZ() + 0.5);
        breeder.messages.clear();
        breeder.gameMode.useItemOn(breeder, level, breeder.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(place), Direction.UP, place, false));
        List<Component> lines = List.copyOf(breeder.messages);
        level.setBlockAndUpdate(place, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(soil, before);
        return lines;
    }
}
