package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.climate.Climate;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The climate stub and the hybridization that depends on it (issue #28). The mod reads the
 * climate of a place through {@link FarmingEvolution#CLIMATE}; the pack data of the mod has one
 * climate condition: a ripe wild_flax gives seeds of fiber_flax only where it is cool and humid
 * (temperature 4 to 16 °C, humidity not below 65 %).
 *
 * <p>The places are far from the test structure, each in its own climate band of the stub, so
 * the harvest is asked for directly at their positions, by the same call every way of breaking
 * a block makes: the loot table and the plant block run as they do in play.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClimateGameTests {
    /** 12 °C, humidity about 85 %: cool and humid. */
    private static final BlockPos COOL_HUMID = new BlockPos(1000, 64, 0);

    /** 12 °C, humidity about 15 %: as cool, but dry. */
    private static final BlockPos COOL_DRY = new BlockPos(-1000, 64, 0);

    /** About −6 °C, humidity about 85 %: as humid, but cold. */
    private static final BlockPos COLD_HUMID = new BlockPos(1000, 64, -2000);

    /** Ripe harvests at one place; each gives two seeds. */
    private static final int HARVESTS = 60;

    private ClimateGameTests() {
    }

    @GameTest(template = "empty")
    public static void climateDiffersBetweenPlaces(GameTestHelper helper) {
        Climate humid = FarmingEvolution.CLIMATE.at(helper.getLevel(), COOL_HUMID);
        Climate dry = FarmingEvolution.CLIMATE.at(helper.getLevel(), COOL_DRY);
        Climate cold = FarmingEvolution.CLIMATE.at(helper.getLevel(), COLD_HUMID);
        if (!(humid.humidity() >= dry.humidity() + 50)) {
            helper.fail("Expected the east (" + COOL_HUMID + ") far more humid than the west (" + COOL_DRY
                    + "), got " + humid + " and " + dry);
        }
        if (!(humid.temperature() >= cold.temperature() + 10)) {
            helper.fail("Expected the line z = 0 (" + COOL_HUMID + ") far warmer than the north (" + COLD_HUMID
                    + "), got " + humid + " and " + cold);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wildFlaxGivesFiberFlaxOnlyInCoolHumidClimate(GameTestHelper helper) {
        List<String> humid = seedSpecies(helper, COOL_HUMID);
        long fiber = humid.stream().filter("fiber_flax"::equals).count();
        // Each seed is fiber_flax with odds 1 - 0.75^2 = 0.4375 (a chance of 0.25 per gamete):
        // about 52 of 120 seeds, and 28 to 77 lies 4.5 standard deviations to either side.
        if (fiber < 28 || fiber > 77 || humid.stream().anyMatch(s -> !s.equals("fiber_flax") && !s.equals("wild_flax"))) {
            helper.fail("Expected 28 to 77 of " + humid.size() + " seeds of wild_flax harvested at " + COOL_HUMID
                    + " to be fiber_flax and the rest wild_flax, got " + counts(humid));
        }
        for (BlockPos place : List.of(COOL_DRY, COLD_HUMID)) {
            List<String> species = seedSpecies(helper, place);
            if (species.stream().anyMatch(s -> !s.equals("wild_flax"))) {
                helper.fail("Expected only wild_flax seeds of wild_flax harvested at " + place + ", got " + counts(species));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terraFirmaCraftIsNotInThePack(GameTestHelper helper) {
        if (ModList.get().isLoaded("tfc")) {
            helper.fail("TerraFirmaCraft is loaded: the pack must not depend on it (CONCEPT.md, \"Мир и климат\")");
        }
        helper.succeed();
    }

    /** How many seeds of each species, short enough for the report of a failed test. */
    private static Map<String, Long> counts(List<String> species) {
        return species.stream().collect(Collectors.groupingBy(s -> s, TreeMap::new, Collectors.counting()));
    }

    /** The species of the seeds of {@link #HARVESTS} ripe wild_flax plants harvested at the place. */
    private static List<String> seedSpecies(GameTestHelper helper, BlockPos place) {
        BlockState ripe = FarmingEvolution.CROP.get().defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        PlantBlockEntity plant = new PlantBlockEntity(place, ripe);
        plant.setGenome(FarmingEvolution.BREEDING.wild("wild_flax", Sex.NONE));
        List<String> species = new ArrayList<>();
        for (int i = 0; i < HARVESTS; i++) {
            for (ItemStack stack : Block.getDrops(ripe, helper.getLevel(), place, plant)) {
                if (!stack.is(FarmingEvolution.SEEDS)) {
                    continue;
                }
                Individual seed = stack.get(FarmingEvolution.GENOME);
                if (seed == null) {
                    throw new GameTestAssertException("A seed of a wild_flax harvested at " + place + " has no genome");
                }
                for (int n = 0; n < stack.getCount(); n++) {
                    species.add(seed.species());
                }
            }
        }
        if (species.size() != 2 * HARVESTS) {
            throw new GameTestAssertException("Expected " + 2 * HARVESTS + " seeds from " + HARVESTS
                    + " ripe wild_flax at " + place + ", got " + species.size());
        }
        return species;
    }
}
