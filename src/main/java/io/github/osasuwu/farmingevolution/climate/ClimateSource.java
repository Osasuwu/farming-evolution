package io.github.osasuwu.farmingevolution.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/**
 * Where the mod reads the climate of a place (CONCEPT.md, "Мир и климат"). Everything that
 * needs the climate asks this interface, never a particular model, so another model, such as a
 * climate of its own world generation, can stand behind it without changes to the callers. In
 * the vertical slice it is {@link GradientClimate}.
 */
public interface ClimateSource {
    /** The climate at the position of the level. */
    Climate at(LevelReader level, BlockPos pos);
}
