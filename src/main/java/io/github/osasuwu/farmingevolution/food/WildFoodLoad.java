package io.github.osasuwu.farmingevolution.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How much wild food a player has eaten lately: {@code points} as of game time {@code since}.
 * Points fall by one every {@link WildFood#DECAY_TICKS} after {@code since}. Saved with the
 * player, so leaving the world and coming back keeps it, and the game time it counts in stands
 * still while nobody plays; it is not copied on death, so a respawned player starts at 0.
 */
public record WildFoodLoad(int points, long since) {
    public static final WildFoodLoad NONE = new WildFoodLoad(0, 0);

    public static final Codec<WildFoodLoad> CODEC = RecordCodecBuilder.create(load -> load.group(
            Codec.INT.fieldOf("points").forGetter(WildFoodLoad::points),
            Codec.LONG.fieldOf("since").forGetter(WildFoodLoad::since)).apply(load, WildFoodLoad::new));

    /** The load as of game time {@code now}, the whole decay steps since {@link #since} taken off. */
    public WildFoodLoad at(long now) {
        if (now < since) {
            // The game time went back (the player came from another world's save): count from now.
            return new WildFoodLoad(points, now);
        }
        long steps = (now - since) / WildFood.DECAY_TICKS;
        if (steps == 0) {
            return this;
        }
        return new WildFoodLoad((int) Math.max(0, points - steps), since + steps * WildFood.DECAY_TICKS);
    }

    /** The load after eating food worth {@code load} points at game time {@code now}. */
    public WildFoodLoad eat(long now, int load) {
        WildFoodLoad current = at(now);
        if (current.points == 0) {
            return new WildFoodLoad(Math.min(load, WildFood.MAX_LOAD), now);
        }
        return new WildFoodLoad(Math.min(current.points + load, WildFood.MAX_LOAD), current.since);
    }
}
