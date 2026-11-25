package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.food.WildFoodLoad;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wild food is edible but unpleasant (issue #89): seeds and wild berries eaten in a row load
 * the eater, and a heavy load gives Hunger, then Nausea. A berry loads half as much as a seed.
 * The numbers are those of CONCEPT.md, "Еда в срезе": a seed adds 2 points, a berry 1; Hunger
 * for 30 s from 8 points, Nausea for 15 s from 12.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildFoodGameTests {
    private static final int HUNGER_TICKS = 600;
    private static final int NAUSEA_TICKS = 300;

    private WildFoodGameTests() {
    }

    /** One or two seeds do nothing, nor does a third; the fourth in a row gives Hunger, not Nausea. */
    @GameTest(template = "empty")
    public static void fourthSeedInARowGivesHunger(GameTestHelper helper) {
        Player player = hungryPlayer(helper);
        List<String> seen = eatAndWatch(helper, player, FarmingEvolution.SEEDS.get(), 4);
        expect(seen, List.of("none", "none", "none", "hunger " + HUNGER_TICKS), "seeds");
        helper.succeed();
    }

    /** The fifth seed in a row keeps Hunger alone; the sixth adds Nausea. */
    @GameTest(template = "empty")
    public static void sixthSeedInARowAddsNausea(GameTestHelper helper) {
        Player player = hungryPlayer(helper);
        List<String> seen = eatAndWatch(helper, player, FarmingEvolution.SEEDS.get(), 6);
        expect(seen.subList(4, 6), List.of("hunger " + HUNGER_TICKS,
                "hunger " + HUNGER_TICKS + ", nausea " + NAUSEA_TICKS), "fifth and sixth seed");
        helper.succeed();
    }

    /** Berries are twice as tolerable: seven in a row do nothing, the eighth gives Hunger. */
    @GameTest(template = "empty")
    public static void eighthBerryInARowGivesHunger(GameTestHelper helper) {
        Player player = hungryPlayer(helper);
        List<String> seen = eatAndWatch(helper, player, FarmingEvolution.WILD_BERRIES.get(), 8);
        List<String> expected = new ArrayList<>(List.of("none", "none", "none", "none", "none", "none", "none"));
        expected.add("hunger " + HUNGER_TICKS);
        expect(seen, expected, "berries");
        helper.succeed();
    }

    /**
     * The load falls by one point every 30 s: 6 points eaten 600 ticks ago are 5 now, so a seed
     * makes 7 (nothing) and the next 9 (Hunger). 599 ticks ago they are still 6, and one seed
     * makes 8 (Hunger).
     */
    @GameTest(template = "empty")
    public static void loadFallsByOnePointEveryThirtySeconds(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        Player rested = hungryPlayer(helper);
        rested.setData(FarmingEvolution.WILD_FOOD_LOAD, new WildFoodLoad(6, now - 600));
        expect(eatAndWatch(helper, rested, FarmingEvolution.SEEDS.get(), 2),
                List.of("none", "hunger " + HUNGER_TICKS), "seeds 600 ticks after 6 points");
        Player early = hungryPlayer(helper);
        early.setData(FarmingEvolution.WILD_FOOD_LOAD, new WildFoodLoad(6, now - 599));
        expect(eatAndWatch(helper, early, FarmingEvolution.SEEDS.get(), 1),
                List.of("hunger " + HUNGER_TICKS), "seed 599 ticks after 6 points");
        helper.succeed();
    }

    /**
     * The load is saved with the player: three seeds, then the player is saved and loaded as a
     * new one (leaving the world and coming back), and the fourth seed still gives Hunger.
     */
    @GameTest(template = "empty")
    public static void loadSurvivesSavingThePlayer(GameTestHelper helper) {
        Player before = hungryPlayer(helper);
        eatAndWatch(helper, before, FarmingEvolution.SEEDS.get(), 3);
        CompoundTag saved = before.saveWithoutId(new CompoundTag());
        Player after = hungryPlayer(helper);
        after.load(saved);
        expect(eatAndWatch(helper, after, FarmingEvolution.SEEDS.get(), 1),
                List.of("hunger " + HUNGER_TICKS), "fourth seed after saving and loading the player");
        helper.succeed();
    }

    /**
     * The load stops at 16 points however much is eaten, so it falls back to nothing within
     * 8 minutes: 20 seeds in a row, 40 points' worth, leave 16.
     */
    @GameTest(template = "empty")
    public static void loadStopsAtSixteenPoints(GameTestHelper helper) {
        Player player = hungryPlayer(helper);
        eatAndWatch(helper, player, FarmingEvolution.SEEDS.get(), 20);
        long now = helper.getLevel().getGameTime();
        WildFoodLoad load = player.getData(FarmingEvolution.WILD_FOOD_LOAD);
        if (load.points() != 16 || load.since() != now) {
            throw new GameTestAssertException("Expected 16 points since " + now + " after 20 seeds, got " + load);
        }
        helper.succeed();
    }

    private static Player hungryPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(2);
        return player;
    }

    /** Eats the food that many times in the same tick; after each, the effects the eater has. */
    private static List<String> eatAndWatch(GameTestHelper helper, Player player, Item food, int times) {
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            eat(helper, player, food);
            seen.add(effects(player));
        }
        return seen;
    }

    private static void eat(GameTestHelper helper, Player player, Item food) {
        ItemStack left = new ItemStack(food).finishUsingItem(helper.getLevel(), player);
        if (!left.isEmpty()) {
            throw new GameTestAssertException("Eating " + food + " left " + left);
        }
    }

    /** Hunger and Nausea with their remaining ticks, or "none". */
    private static String effects(Player player) {
        List<String> parts = new ArrayList<>();
        describe(player, MobEffects.HUNGER, "hunger", parts);
        describe(player, MobEffects.CONFUSION, "nausea", parts);
        if (player.getActiveEffects().size() != parts.size()) {
            parts.add("other " + player.getActiveEffects());
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }

    private static void describe(Player player, Holder<MobEffect> effect, String name, List<String> parts) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null) {
            parts.add(name + " " + instance.getDuration() + (instance.getAmplifier() == 0 ? "" : " x" + instance.getAmplifier()));
        }
    }

    private static void expect(List<String> seen, List<String> expected, String what) {
        if (!seen.equals(expected)) {
            throw new GameTestAssertException("Effects after each of the " + what + ": expected " + expected + ", got " + seen);
        }
    }
}
