package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Crossing, the second method of selection (issue #27; CONCEPT.md, "Методы селекции"): the
 * analyzer is made of Create's cogwheels and shafts without the belt, shows both alleles of a
 * plant and the odds of a pair, and carries pollen from one plant to another; a pollinated
 * plant's seeds are offspring of the pair. A player uses the analyzer the way the game does
 * for a right click, and the tests read what reached the player's chat.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrossingGameTests {
    private static final BlockPos MOTHER = new BlockPos(1, 2, 2);
    private static final BlockPos FATHER = new BlockPos(4, 2, 2);
    private static final String STRENGTH = "fiber_strength";
    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("0b6a2f4e-7d1c-4c55-8f0e-27c0a5b1e927"), "fe-breeder");
    private static final ResourceLocation ANALYZER_RECIPE = id("analyzer");
    private static final ResourceLocation BELT = ResourceLocation.fromNamespaceAndPath("create", "belt_connector");

    private CrossingGameTests() {
    }

    /**
     * Two cogwheels and two shafts of Create make one analyzer, by the pack's only recipe of
     * it. Nothing on the way to it takes the belt: not that recipe, not the recipes of the shaft,
     * the cogwheel and the andesite alloy they are made of.
     */
    @GameTest(template = "empty")
    public static void analyzerIsMadeOfCogwheelsAndShaftsWithoutBelt(GameTestHelper helper) {
        Item cogwheel = item(ResourceLocation.fromNamespaceAndPath("create", "cogwheel"));
        Item shaft = item(ResourceLocation.fromNamespaceAndPath("create", "shaft"));
        List<ItemStack> grid = List.of(new ItemStack(cogwheel), new ItemStack(shaft), new ItemStack(cogwheel),
                ItemStack.EMPTY, new ItemStack(shaft), ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 2, grid);
        ItemStack result = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        if (!result.is(FarmingEvolution.ANALYZER.get()) || result.getCount() != 1) {
            throw new GameTestAssertException("Expected 1 analyzer out of 2 cogwheels and 2 shafts, got " + result);
        }
        List<ResourceLocation> recipes = recipesOf(helper, FarmingEvolution.ANALYZER.get()).stream()
                .map(RecipeHolder::id).sorted().toList();
        if (!recipes.equals(List.of(ANALYZER_RECIPE))) {
            throw new GameTestAssertException("Expected the only recipe of the analyzer to be " + ANALYZER_RECIPE + ", got " + recipes);
        }
        ItemStack belt = new ItemStack(item(BELT));
        List<String> takingBelt = new ArrayList<>();
        for (Item made : List.of(FarmingEvolution.ANALYZER.get(), shaft, cogwheel,
                item(ResourceLocation.fromNamespaceAndPath("create", "andesite_alloy")))) {
            List<RecipeHolder<?>> ways = recipesOf(helper, made);
            if (ways.isEmpty()) {
                throw new GameTestAssertException("No recipe makes " + made);
            }
            for (RecipeHolder<?> recipe : ways) {
                for (Ingredient ingredient : recipe.value().getIngredients()) {
                    if (ingredient.test(belt)) {
                        takingBelt.add(recipe.id().toString());
                    }
                }
            }
        }
        if (!takingBelt.isEmpty()) {
            throw new GameTestAssertException("Recipes on the way to the analyzer take the belt: " + takingBelt);
        }
        helper.succeed();
    }

    /**
     * The analyzer on a plant shows both alleles of each chromosome, the expressed one first,
     * and what the plant gives: a 7|4 flax is strong_flax by its expressed 7. With a seed in the
     * other hand, used in the air, it shows the seed's alleles the same way.
     */
    @GameTest(template = "empty")
    public static void analyzerShowsBothAllelesAndProduct(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        plant(helper, MOTHER, flax(7, 4));
        List<String> lines = breeder.useOn(helper, MOTHER, false);
        String product = new ItemStack(item(id("strong_flax"))).getHoverName().getString();
        expectLines(lines, List.of("  " + STRENGTH + ": 7 | 4", "  species: fiber_flax | fiber_flax",
                "  gives " + product, "  pollinates itself"));

        breeder.setItemInHand(InteractionHand.OFF_HAND, seedOf(flax(3, 8)));
        lines = breeder.useInAir(helper, false);
        expectLines(lines, List.of("Seed, alleles (expressed | carried):", "  " + STRENGTH + ": 3 | 8"));
        helper.succeed();
    }

    /**
     * Odds worked out by hand. A 3|3 plant and pollen of a 9|9 one: each parent passes its
     * value with chance 0.8 and a value 1 lower or higher with 0.1 each, so the offspring is
     * 9 & 3 with 0.8 * 0.8 = 64%, 9 & 2, 9 & 4, 8 & 3 and 10 & 3 with 0.8 * 0.1 = 8% each, and
     * every pair of two moved values with 0.1 * 0.1 = 1%.
     */
    @GameTest(template = "empty")
    public static void analyzerShowsOddsOfPairComputedByHand(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        plant(helper, MOTHER, flax(3, 3));
        plant(helper, FATHER, flax(9, 9));
        expectLines(breeder.useOn(helper, FATHER, true), List.of("Took the pollen of species fiber_flax | fiber_flax, "
                + STRENGTH + " 9 | 9"));
        List<String> lines = breeder.useOn(helper, MOTHER, false);
        expectLines(lines, List.of("If pollinated with the analyzer's pollen:"));
        List<String> after = lines.subList(lines.indexOf("If pollinated with the analyzer's pollen:"), lines.size());
        String strength = after.stream().filter(line -> line.startsWith("  " + STRENGTH + ": ")).findFirst()
                .orElseThrow(() -> new GameTestAssertException("No line of the odds of the strength in " + lines));
        if (!strength.startsWith("  " + STRENGTH + ": 9 & 3 64.0%, ")) {
            throw new GameTestAssertException("Expected 9 & 3 64.0% first, got " + strength);
        }
        for (String odds : List.of("9 & 2 8.0%", "9 & 4 8.0%", "8 & 3 8.0%", "10 & 3 8.0%",
                "8 & 2 1.0%", "8 & 4 1.0%", "10 & 2 1.0%", "10 & 4 1.0%")) {
            if (!List.of(strength.substring(strength.indexOf(": ") + 2).split(", ")).contains(odds)) {
                throw new GameTestAssertException("Expected " + odds + " among the odds, got " + strength);
            }
        }
        if (outcomes(strength).size() != 9) {
            throw new GameTestAssertException("Expected 9 outcomes, got " + strength);
        }
        if (breeder.useOn(helper, MOTHER, false).contains("  pollinated by species fiber_flax | fiber_flax, " + STRENGTH + " 9 | 9")) {
            throw new GameTestAssertException("Looking at the odds pollinated the plant");
        }
        helper.succeed();
    }

    /**
     * The harvest rolls what the analyzer showed: a 3|3 plant pollinated with a 9|9 one gives
     * seeds with one allele near 3 and one near 9, each pair among the outcomes the analyzer
     * listed when it pollinated the plant. A plant that ignored its pollen would give no 8, 9
     * or 10.
     */
    @GameTest(template = "empty")
    public static void harvestOfPollinatedPlantRollsAmongShownOutcomes(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        for (int round = 0; round < 5; round++) {
            plant(helper, MOTHER, flax(3, 3));
            plant(helper, FATHER, flax(9, 9));
            breeder.useOn(helper, FATHER, true);
            List<String> lines = breeder.useOn(helper, MOTHER, true);
            expectLines(lines, List.of("Pollinated with species fiber_flax | fiber_flax, " + STRENGTH + " 9 | 9",
                    "Odds of the seeds of this plant:"));
            Set<String> shown = outcomes(lines.stream().filter(line -> line.startsWith("  " + STRENGTH + ": ")).findFirst()
                    .orElseThrow(() -> new GameTestAssertException("No odds of the strength in " + lines)));
            expectLines(breeder.useOn(helper, MOTHER, false),
                    List.of("  pollinated by species fiber_flax | fiber_flax, " + STRENGTH + " 9 | 9"));
            breeder.useInAir(helper, true);

            ripen(helper, MOTHER);
            List<Individual> seeds = genomesOfSeeds(breakBlock(helper, MOTHER));
            if (seeds.size() != 2) {
                throw new GameTestAssertException("Expected 2 seeds from the ripe plant, got " + seeds);
            }
            for (Individual seed : seeds) {
                List<Integer> alleles = strengths(seed);
                int low = Math.min(alleles.get(0), alleles.get(1));
                int high = Math.max(alleles.get(0), alleles.get(1));
                if (low < 2 || low > 4 || high < 8 || high > 10 || !shown.contains(high + " & " + low)) {
                    throw new GameTestAssertException("Seed " + seed + " is not among the shown outcomes " + shown);
                }
            }
            helper.setBlock(FATHER, Blocks.AIR);
        }
        helper.succeed();
    }

    /**
     * Crossing reaches strong_flax: two plants from the seeds of the wild fiber_flax (5) are
     * pollinated with each other's pollen through the analyzer every generation, and the best
     * seed of each plant's harvest is sown again. Every seed descends from its pair: one allele
     * within 1 of an allele of the plant, the other within 1 of an allele of the pollen's plant.
     */
    @GameTest(template = "empty")
    public static void crossingPollinatedPairsReachesStrongFlax(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        Individual mother = flax(5, 5);
        Individual father = flax(5, 5);
        for (int generation = 1; generation <= 400; generation++) {
            plant(helper, MOTHER, mother);
            plant(helper, FATHER, father);
            ripen(helper, MOTHER);
            ripen(helper, FATHER);
            pollinate(helper, breeder, FATHER, MOTHER);
            pollinate(helper, breeder, MOTHER, FATHER);
            List<Individual> fromMother = harvest(helper, MOTHER, mother, father);
            List<Individual> fromFather = harvest(helper, FATHER, father, mother);
            if (mother.trait(STRENGTH) >= 7 || father.trait(STRENGTH) >= 7) {
                if (generation == 1) {
                    throw new GameTestAssertException("The wild flax is strong_flax already: nothing was crossed");
                }
                helper.succeed();
                return;
            }
            mother = best(fromMother);
            father = best(fromFather);
        }
        throw new GameTestAssertException("400 generations of crossing did not reach strong_flax, the last pair: "
                + mother + " and " + father);
    }

    /**
     * A pair the engine cannot cross is refused with its reason, and the plant is not
     * pollinated. The pack's flax has no sexes, so pollen of a male flax (made by hand here)
     * cannot cross with it.
     */
    @GameTest(template = "empty")
    public static void pairThatCannotCrossIsRefusedAndNotPollinated(GameTestHelper helper) {
        Breeder breeder = new Breeder(helper);
        plant(helper, MOTHER, flax(5, 5));
        Individual male = new Individual(Sex.MALE, flax(9, 9).genome());
        breeder.getMainHandItem().set(FarmingEvolution.POLLEN, male);
        List<String> lines = breeder.useOn(helper, MOTHER, true);
        if (lines.size() != 1 || !lines.get(0).startsWith("These two cannot be crossed: ") || !lines.get(0).contains("no sexes")) {
            throw new GameTestAssertException("Expected the refusal with the engine's reason, got " + lines);
        }
        if (plantAt(helper, MOTHER).pollen().isPresent()) {
            throw new GameTestAssertException("The refused pollen pollinated the plant");
        }
        expectLines(breeder.useOn(helper, MOTHER, false), List.of("  pollinates itself"));
        helper.succeed();
    }

    /** The pollen of a plant and of an analyzer outlives saving and loading. */
    @GameTest(template = "empty")
    public static void pollenOfPlantAndAnalyzerSurvivesSaving(GameTestHelper helper) {
        Individual pollen = flax(9, 8);
        plant(helper, MOTHER, flax(3, 4));
        PlantBlockEntity plant = plantAt(helper, MOTHER);
        plant.setPollen(pollen);
        CompoundTag saved = plant.saveWithoutMetadata(helper.getLevel().registryAccess());
        PlantBlockEntity loaded = new PlantBlockEntity(plant.getBlockPos(), plant.getBlockState());
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        if (!loaded.pollen().equals(plant.pollen()) || !loaded.genome().equals(plant.genome())) {
            throw new GameTestAssertException("Saved " + plant.genome() + " pollinated by " + plant.pollen()
                    + ", loaded " + loaded.genome() + " pollinated by " + loaded.pollen());
        }

        ItemStack analyzer = new ItemStack(FarmingEvolution.ANALYZER.get());
        analyzer.set(FarmingEvolution.POLLEN, pollen);
        ItemStack reloaded = ItemStack.parse(helper.getLevel().registryAccess(),
                analyzer.save(helper.getLevel().registryAccess())).orElseThrow();
        if (!pollen.equals(reloaded.get(FarmingEvolution.POLLEN))) {
            throw new GameTestAssertException("The analyzer's pollen " + pollen + " came back as " + reloaded.get(FarmingEvolution.POLLEN));
        }
        helper.succeed();
    }

    /** A player whose chat the test reads, holding an analyzer. */
    private static final class Breeder extends FakePlayer {
        private final List<String> messages = new ArrayList<>();

        Breeder(GameTestHelper helper) {
            super(helper.getLevel(), PROFILE);
            setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FarmingEvolution.ANALYZER.get()));
        }

        @Override
        public void displayClientMessage(Component message, boolean overlay) {
            messages.add(message.getString());
        }

        /** A right click with the analyzer on the block, sneaking or not; returns the lines shown. */
        List<String> useOn(GameTestHelper helper, BlockPos pos, boolean sneaking) {
            messages.clear();
            setShiftKeyDown(sneaking);
            BlockPos absolute = helper.absolutePos(pos);
            gameMode.useItemOn(this, helper.getLevel(), getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
            setShiftKeyDown(false);
            return List.copyOf(messages);
        }

        /** A right click with the analyzer in the air; returns the lines shown. */
        List<String> useInAir(GameTestHelper helper, boolean sneaking) {
            messages.clear();
            setShiftKeyDown(sneaking);
            gameMode.useItem(this, helper.getLevel(), getMainHandItem(), InteractionHand.MAIN_HAND);
            setShiftKeyDown(false);
            return List.copyOf(messages);
        }
    }

    /** Takes the pollen of one plant with an empty analyzer, pollinates the other and drops the rest. */
    private static void pollinate(GameTestHelper helper, Breeder breeder, BlockPos from, BlockPos to) {
        breeder.useOn(helper, from, true);
        List<String> lines = breeder.useOn(helper, to, true);
        if (lines.isEmpty() || !lines.get(0).startsWith("Pollinated with ")) {
            throw new GameTestAssertException("The plant at " + to + " was not pollinated: " + lines);
        }
        expectLines(breeder.useInAir(helper, true), List.of("Dropped the pollen"));
    }

    /** Breaks the ripe plant; checks its product and that its seeds descend from the pair. */
    private static List<Individual> harvest(GameTestHelper helper, BlockPos pos, Individual plant, Individual pollen) {
        List<ItemStack> drops = breakBlock(helper, pos);
        List<Individual> seeds = genomesOfSeeds(drops);
        if (seeds.size() != 2 || seeds.stream().anyMatch(seed -> !descendsFrom(seed, plant, pollen))) {
            throw new GameTestAssertException("Expected 2 seeds descending from " + plant + " and " + pollen + ", got " + seeds);
        }
        String expected = plant.trait(STRENGTH) >= 7 ? "farmingevolution:strong_flax" : "farmingevolution:fiber_flax_fiber";
        List<String> products = drops.stream().filter(stack -> !stack.is(FarmingEvolution.SEEDS))
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()).toList();
        if (!products.equals(List.of(expected + " x1"))) {
            throw new GameTestAssertException("Expected " + expected + " from " + plant + ", got " + products);
        }
        return seeds;
    }

    /** One allele of the strength within 1 of an allele of each parent. */
    private static boolean descendsFrom(Individual child, Individual first, Individual second) {
        List<Integer> alleles = strengths(child);
        return child.species().equals(first.species())
                && (near(alleles.get(0), first) && near(alleles.get(1), second)
                        || near(alleles.get(1), first) && near(alleles.get(0), second));
    }

    private static boolean near(int allele, Individual parent) {
        return strengths(parent).stream().anyMatch(own -> Math.abs(own - allele) <= 1);
    }

    private static Individual best(List<Individual> seeds) {
        return seeds.stream().max(Comparator.comparingInt(seed -> strengths(seed).get(0) + strengths(seed).get(1))).orElseThrow();
    }

    /** The outcomes of a line of odds that have a chance: {@code 9 & 3}, ... */
    private static Set<String> outcomes(String line) {
        Set<String> outcomes = new HashSet<>();
        for (String odds : line.substring(line.indexOf(": ") + 2).split(", ")) {
            String pair = odds.substring(0, odds.lastIndexOf(' '));
            if (!odds.endsWith(" 0.0%")) {
                outcomes.add(pair);
            }
        }
        return outcomes;
    }

    private static void expectLines(List<String> lines, List<String> expected) {
        List<String> missing = expected.stream().filter(line -> !lines.contains(line)).toList();
        if (!missing.isEmpty()) {
            throw new GameTestAssertException("Expected the lines " + missing + " among " + lines);
        }
    }

    private static Individual flax(int active, int inactive) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("fiber_flax", true)),
                STRENGTH, new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive))));
    }

    private static List<Integer> strengths(Individual individual) {
        Chromosome strength = individual.genome().get(STRENGTH);
        return List.of(((Allele.Numeric) strength.active()).value(), ((Allele.Numeric) strength.inactive()).value());
    }

    private static List<RecipeHolder<?>> recipesOf(GameTestHelper helper, Item item) {
        return helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()).is(item))
                .toList();
    }

    private static PlantBlockEntity plantAt(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof PlantBlockEntity plant)) {
            throw new GameTestAssertException("No plant at " + pos);
        }
        return plant;
    }

    private static void ripen(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, helper.getBlockState(pos).setValue(CropBlock.AGE, CropBlock.MAX_AGE));
    }

    private static void plant(GameTestHelper helper, BlockPos pos, Individual genome) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos, Blocks.AIR);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(pos), Direction.DOWN, seedOf(genome), Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + pos);
        }
    }

    private static ItemStack seedOf(Individual genome) {
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        return seed;
    }

    private static List<ItemStack> breakBlock(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        if (!new FakePlayer(level, PROFILE).gameMode.destroyBlock(helper.absolutePos(pos)) || !helper.getBlockState(pos).isAir()) {
            throw new GameTestAssertException("The player could not break the block at " + pos);
        }
        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1));
        List<ItemStack> stacks = dropped.stream().map(entity -> entity.getItem().copy()).toList();
        dropped.forEach(ItemEntity::discard);
        return stacks;
    }

    private static List<Individual> genomesOfSeeds(List<ItemStack> stacks) {
        List<Individual> genomes = new ArrayList<>();
        for (ItemStack stack : stacks) {
            for (int i = 0; stack.is(FarmingEvolution.SEEDS) && i < stack.getCount(); i++) {
                Individual genome = stack.get(FarmingEvolution.GENOME);
                if (genome == null) {
                    throw new GameTestAssertException("A seed without a genome among " + stacks);
                }
                genomes.add(genome);
            }
        }
        return genomes;
    }

    private static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("Item " + id + " is not registered"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, path);
    }
}
