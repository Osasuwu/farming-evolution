package io.github.osasuwu.farmingevolution.food;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;

/**
 * Wild food (issue #89): the edible seed and the wild berries feed, but eaten in a row they
 * load the eater, and a heavy load gives vanilla Hunger, then Nausea. There is no nutrition
 * system of the mod (CONCEPT.md): only this counter on the player, {@link WildFoodLoad}.
 *
 * <p>Every number of wild food is here; CONCEPT.md, "Еда в срезе", names them and gives the
 * reckoning that a player living on wild food alone does not starve.
 */
public final class WildFood {
    /** Food points a seed restores: half a hunger icon. */
    public static final int SEED_NUTRITION = 1;

    /** Food points a wild berry restores. */
    public static final int BERRY_NUTRITION = 2;

    /** Saturation modifier of both: saturation gained is nutrition × 0.1 × 2. */
    public static final float SATURATION_MODIFIER = 0.1F;

    /** Load points a seed adds. */
    public static final int SEED_LOAD = 2;

    /** Load points a wild berry adds: half a seed's, so berries are twice as tolerable. */
    public static final int BERRY_LOAD = 1;

    /** The load falls by one point every this many ticks of game time (30 s). */
    public static final int DECAY_TICKS = 600;

    /** The load never goes above this: at most 8 minutes to fall back to nothing. */
    public static final int MAX_LOAD = 16;

    /** From this load on, eating gives Hunger: the 4th seed in a row, the 8th berry. */
    public static final int HUNGER_LOAD = 8;

    /** How long that Hunger lasts (30 s); amplifier 0, eating again only renews it. */
    public static final int HUNGER_TICKS = 600;

    /** From this load on, eating also gives Nausea: the 6th seed in a row, the 12th berry. */
    public static final int NAUSEA_LOAD = 12;

    /** How long that Nausea lasts (15 s). */
    public static final int NAUSEA_TICKS = 300;

    public static final FoodProperties SEED_FOOD = new FoodProperties.Builder()
            .nutrition(SEED_NUTRITION).saturationModifier(SATURATION_MODIFIER).build();

    public static final FoodProperties BERRY_FOOD = new FoodProperties.Builder()
            .nutrition(BERRY_NUTRITION).saturationModifier(SATURATION_MODIFIER).build();

    private WildFood() {
    }

    /**
     * Called once the eater has eaten wild food worth that many load points: adds them to the
     * eater's load and gives the effects the new load reaches. Players only, on the server.
     */
    public static void eaten(LivingEntity eater, int load) {
        if (eater.level().isClientSide() || !(eater instanceof Player)) {
            return;
        }
        WildFoodLoad after = eater.getData(FarmingEvolution.WILD_FOOD_LOAD).eat(eater.level().getGameTime(), load);
        eater.setData(FarmingEvolution.WILD_FOOD_LOAD, after);
        if (after.points() >= HUNGER_LOAD) {
            eater.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_TICKS));
        }
        if (after.points() >= NAUSEA_LOAD) {
            eater.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS));
        }
    }
}
