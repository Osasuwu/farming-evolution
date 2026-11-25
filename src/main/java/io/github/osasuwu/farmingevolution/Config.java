package io.github.osasuwu.farmingevolution;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The settings of the mod, in {@code config/farmingevolution-common.toml} of the game
 * directory. The game writes the file with the defaults on the first start; a changed value
 * takes effect on the next start at the latest.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * The pace of growth of the plants of the mod (issue #87): mean minutes of play from seed to
     * ripe plant on watered farmland at the default random tick speed. On dry farmland a plant
     * takes {@link io.github.osasuwu.farmingevolution.plant.GrowthPace#DRY_SLOWDOWN} times
     * longer.
     */
    public static final ModConfigSpec.DoubleValue GENERATION_MINUTES = BUILDER
            .comment("Mean minutes of play from seed to ripe plant of the mod on watered farmland,",
                    "at the default random tick speed (3). On dry farmland a plant takes 1.5 times longer.",
                    "Vanilla crops are not affected.")
            .defineInRange("plantGenerationMinutes", 9.0, 0.5, 600.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
