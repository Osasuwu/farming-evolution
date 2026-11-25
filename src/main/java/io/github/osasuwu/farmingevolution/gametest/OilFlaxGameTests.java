package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.climate.Climate;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The climate a player can see (issue #67): fiber_flax, the flax a wild flax gives, pollinated
 * by itself gives oil_flax with a chance of 0.1 per gamete where it is warm and dry
 * (temperature not below 20 °C, humidity not above 35 %), and never elsewhere. oil_flax is a
 * species of its own with its own product, oil_flax_fiber.
 *
 * <p>The places are far from the test structure, each at an absolute position in a band of the
 * climate stub, as in {@link ClimateGameTests}. Everything starts from what a player has: the
 * seeds of a broken wild flax, the analyzer, a plant grown from a seed.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OilFlaxGameTests {
    /** About 30 °C, humidity about 10 %: the hottest and the driest band. */
    private static final BlockPos WARM_DRY = new BlockPos(-1500, -60, 2000);

    /** About 20.2 °C, humidity about 34.5 %: just inside both limits. */
    private static final BlockPos JUST_WARM_DRY = new BlockPos(-380, -60, 600);

    /** About 19.8 °C, humidity about 34.5 %: as dry, just too cool. */
    private static final BlockPos JUST_TOO_COOL = new BlockPos(-380, -60, 570);

    /** About 20.2 °C, humidity about 35.7 %: as warm, just too humid. */
    private static final BlockPos JUST_TOO_HUMID = new BlockPos(-350, -60, 600);

    /** About 30 °C, humidity about 90 %: as warm, but humid. */
    private static final BlockPos WARM_HUMID = new BlockPos(1500, -60, 2000);

    /** 12 °C, humidity about 10 %: as dry, but cool. */
    private static final BlockPos COOL_DRY = new BlockPos(-1500, -60, 0);

    /** About 11 °C, humidity about 48 %: near the origin of the world, where a new world puts the player, out of the GameTest grid. */
    private static final BlockPos NEAR_ORIGIN = new BlockPos(-50, -60, -50);

    /** Where a plant grows inside the test structure, on the farmland below it. */
    private static final BlockPos PLANT = new BlockPos(1, 2, 1);

    /** Ripe harvests at one place; each gives two seeds. */
    private static final int HARVESTS = 200;

    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("7c1d9e52-3f0a-4b8e-a6d4-91e05c3b2f67"), "fe-oil-breeder");

    /**
     * The odds of the species of the seeds of a fiber_flax pollinated by itself where oil_flax
     * may appear, worked out by hand: each gamete is oil_flax with chance 0.1, so a seed is
     * fiber_flax & fiber_flax with 0.9 * 0.9 = 81%, fiber_flax & oil_flax with 2 * 0.9 * 0.1 =
     * 18%, oil_flax & oil_flax with 0.1 * 0.1 = 1%.
     */
    private static final String OIL_FLAX_ODDS =
            "  species: fiber_flax & fiber_flax 81.0%, fiber_flax & oil_flax 18.0%, oil_flax & oil_flax 1.0%";

    /** The same odds where no hybridization happens. */
    private static final String NO_HYBRID_ODDS = "  species: fiber_flax & fiber_flax 100.0%";

    private OilFlaxGameTests() {
    }

    /**
     * Seeds of a broken wild flax, grown into ripe plants and harvested: in the warm dry place
     * about a fifth of the seeds carry oil_flax, in the cool, the humid and the mild places none
     * does. A ripe plant whose expressed species is oil_flax gives oil_flax_fiber.
     */
    @GameTest(template = "empty")
    public static void fiberFlaxGivesOilFlaxOnlyWhereWarmAndDry(GameTestHelper helper) {
        List<Individual> warm = seeds(helper, WARM_DRY);
        long withOil = warm.stream().filter(OilFlaxGameTests::carriesOilFlax).count();
        // Each seed carries oil_flax with odds 1 - 0.9^2 = 0.19: about 76 of 400 seeds, and 41 to
        // 111 lies 4.5 standard deviations to either side.
        if (withOil < 41 || withOil > 111 || warm.stream().anyMatch(seed -> !speciesAlleles(seed).stream()
                .allMatch(s -> s.equals("fiber_flax") || s.equals("oil_flax")))) {
            throw new GameTestAssertException("Expected 41 to 111 of " + warm.size() + " seeds of fiber_flax harvested at "
                    + WARM_DRY.toShortString() + " (" + climate(helper, WARM_DRY) + ") to carry oil_flax and the rest to be"
                    + " fiber_flax, got " + withOil + " carrying oil_flax");
        }
        for (BlockPos place : List.of(NEAR_ORIGIN, WARM_HUMID, COOL_DRY)) {
            List<Individual> seeds = seeds(helper, place);
            if (seeds.stream().anyMatch(seed -> !speciesAlleles(seed).equals(List.of("fiber_flax", "fiber_flax")))) {
                throw new GameTestAssertException("Expected only fiber_flax seeds of fiber_flax harvested at "
                        + place.toShortString() + " (" + climate(helper, place) + "), got "
                        + seeds.stream().filter(OilFlaxGameTests::carriesOilFlax).count() + " carrying oil_flax");
            }
        }

        Individual oil = warm.stream().filter(seed -> seed.species().equals("oil_flax")).findFirst()
                .orElseThrow(() -> new GameTestAssertException("No seed of the warm dry harvest expresses oil_flax"));
        plant(helper, oil);
        helper.setBlock(PLANT, helper.getBlockState(PLANT).setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        List<String> products = breakBlock(helper).stream().filter(stack -> !stack.is(FarmingEvolution.SEEDS))
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()).toList();
        if (!products.equals(List.of("farmingevolution:oil_flax_fiber x1"))) {
            throw new GameTestAssertException("Expected oil_flax_fiber from a ripe plant of " + oil + ", got " + products);
        }
        helper.succeed();
    }

    /**
     * The analyzer on a plant grown from a seed of a wild flax shows oil_flax among the odds of
     * its seeds exactly where the climate allows it: just inside both limits and well inside
     * them, but not just past either limit, nor in the humid, the cool or the mild place.
     */
    @GameTest(template = "empty")
    public static void analyzerShowsOilFlaxOnlyWhereWarmAndDry(GameTestHelper helper) {
        Map<BlockPos, String> expected = new LinkedHashMap<>();
        expected.put(WARM_DRY, OIL_FLAX_ODDS);
        expected.put(JUST_WARM_DRY, OIL_FLAX_ODDS);
        expected.put(JUST_TOO_COOL, NO_HYBRID_ODDS);
        expected.put(JUST_TOO_HUMID, NO_HYBRID_ODDS);
        expected.put(WARM_HUMID, NO_HYBRID_ODDS);
        expected.put(COOL_DRY, NO_HYBRID_ODDS);
        expected.put(NEAR_ORIGIN, NO_HYBRID_ODDS);
        Breeder breeder = new Breeder(helper);
        List<String> wrong = new ArrayList<>();
        expected.forEach((place, odds) -> {
            List<String> lines = analyzeAt(helper, breeder, place);
            String header = "Odds of the seeds of this plant:";
            if (!lines.contains(header)) {
                throw new GameTestAssertException("No odds of the seeds among " + lines + " at " + place.toShortString());
            }
            String species = lines.subList(lines.indexOf(header), lines.size()).stream()
                    .filter(line -> line.startsWith("  species: ")).findFirst().orElse("no line of the species");
            boolean oilShown = lines.stream().anyMatch(line -> line.contains("oil_flax"));
            if (!species.equals(odds) || oilShown != odds.equals(OIL_FLAX_ODDS)) {
                wrong.add(place.toShortString() + " (" + climate(helper, place) + "): expected \"" + odds + "\", got " + lines);
            }
        });
        if (!wrong.isEmpty()) {
            throw new GameTestAssertException("The analyzer showed the wrong odds of the species: " + wrong);
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
    }

    /**
     * Plants a seed of a broken wild flax on farmland at the absolute place, stands the player
     * next to it, uses the analyzer on it and clears the place again; returns the lines shown.
     */
    private static List<String> analyzeAt(GameTestHelper helper, Breeder breeder, BlockPos place) {
        ServerLevel level = helper.getLevel();
        BlockPos soil = place.below();
        BlockState before = level.getBlockState(soil);
        level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(place, Blocks.AIR.defaultBlockState());
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(level, place, Direction.DOWN,
                wildFlaxSeed(helper), Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("A seed of a wild flax could not be planted at " + place.toShortString());
        }
        breeder.moveTo(place.getX() + 2.5, place.getY(), place.getZ() + 0.5);
        breeder.messages.clear();
        breeder.gameMode.useItemOn(breeder, level, breeder.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(place), Direction.UP, place, false));
        List<String> lines = List.copyOf(breeder.messages);
        level.setBlockAndUpdate(place, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(soil, before);
        return lines;
    }

    /**
     * The seeds of {@link #HARVESTS} ripe plants grown from a seed of a broken wild flax and
     * harvested at the place, by the same call every way of breaking a block makes.
     */
    private static List<Individual> seeds(GameTestHelper helper, BlockPos place) {
        BlockState ripe = FarmingEvolution.CROP.get().defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        PlantBlockEntity plant = new PlantBlockEntity(place, ripe);
        plant.setGenome(wildFlaxSeed(helper).get(FarmingEvolution.GENOME));
        List<Individual> seeds = new ArrayList<>();
        for (int i = 0; i < HARVESTS; i++) {
            for (ItemStack stack : Block.getDrops(ripe, helper.getLevel(), place, plant)) {
                for (int n = 0; stack.is(FarmingEvolution.SEEDS) && n < stack.getCount(); n++) {
                    Individual seed = stack.get(FarmingEvolution.GENOME);
                    if (seed == null) {
                        throw new GameTestAssertException("A seed harvested at " + place.toShortString() + " has no genome");
                    }
                    seeds.add(seed);
                }
            }
        }
        if (seeds.size() != 2 * HARVESTS) {
            throw new GameTestAssertException("Expected " + 2 * HARVESTS + " seeds from " + HARVESTS
                    + " ripe plants at " + place.toShortString() + ", got " + seeds.size());
        }
        return seeds;
    }

    /** One seed of a wild flax broken the way the world's wild flax is: the player's first seed. */
    private static ItemStack wildFlaxSeed(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(PLANT);
        List<ItemStack> drops = Block.getDrops(FarmingEvolution.WILD_FLAX.get().defaultBlockState(), helper.getLevel(), pos, null);
        ItemStack seed = drops.stream().filter(stack -> stack.is(FarmingEvolution.SEEDS) && stack.has(FarmingEvolution.GENOME))
                .findFirst().orElseThrow(() -> new GameTestAssertException("No seed with a genome from a wild flax: " + drops));
        Individual genome = seed.get(FarmingEvolution.GENOME);
        if (!speciesAlleles(genome).equals(List.of("fiber_flax", "fiber_flax"))) {
            throw new GameTestAssertException("Expected a wild flax to give seeds of fiber_flax, got " + genome);
        }
        return seed.copyWithCount(1);
    }

    private static void plant(GameTestHelper helper, Individual genome) {
        helper.setBlock(PLANT.below(), Blocks.FARMLAND);
        helper.setBlock(PLANT, Blocks.AIR);
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(PLANT), Direction.DOWN, seed, Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + PLANT);
        }
    }

    /** Breaks the plant the way a player in survival does; returns the drops. */
    private static List<ItemStack> breakBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(PLANT);
        if (!new FakePlayer(level, PROFILE).gameMode.destroyBlock(absolute) || !helper.getBlockState(PLANT).isAir()) {
            throw new GameTestAssertException("The player could not break the plant at " + PLANT);
        }
        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1));
        List<ItemStack> stacks = dropped.stream().map(entity -> entity.getItem().copy()).toList();
        dropped.forEach(ItemEntity::discard);
        return stacks;
    }

    private static boolean carriesOilFlax(Individual seed) {
        return speciesAlleles(seed).contains("oil_flax");
    }

    /** Both alleles of the species, the expressed one first. */
    private static List<String> speciesAlleles(Individual individual) {
        Chromosome species = individual.genome().get(Breeding.SPECIES);
        return List.of(((Allele.Named) species.active()).id(), ((Allele.Named) species.inactive()).id());
    }

    private static Climate climate(GameTestHelper helper, BlockPos place) {
        return FarmingEvolution.CLIMATE.at(helper.getLevel(), place);
    }
}
