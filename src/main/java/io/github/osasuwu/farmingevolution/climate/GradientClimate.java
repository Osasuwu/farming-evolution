package io.github.osasuwu.farmingevolution.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/**
 * The climate stub of the vertical slice (CONCEPT.md, "Мир и климат"): a climate that depends
 * on the position alone, with real gradients in two directions, as in the climate of
 * TerraFirmaCraft. It does not look at biomes or the world seed: every world has the same map.
 *
 * <ul>
 * <li>Temperature changes from north to south: {@value #MEAN_TEMPERATURE} °C on the line z = 0,
 * colder to the north (towards negative z), warmer to the south, between
 * {@value #MEAN_TEMPERATURE} − {@value #TEMPERATURE_SPREAD} and {@value #MEAN_TEMPERATURE} +
 * {@value #TEMPERATURE_SPREAD} °C. The bands repeat every {@value #TEMPERATURE_PERIOD} blocks.
 * <li>Humidity changes from west to east: {@value #MEAN_HUMIDITY} % on the line x = 0, drier to
 * the west (towards negative x), wetter to the east, between {@value #MEAN_HUMIDITY} −
 * {@value #HUMIDITY_SPREAD} and {@value #MEAN_HUMIDITY} + {@value #HUMIDITY_SPREAD} %. The bands
 * repeat every {@value #HUMIDITY_PERIOD} blocks.
 * </ul>
 *
 * Both follow a sine, so the extremes lie a quarter period from the zero line: the coldest band
 * at z = −2000, the hottest at z = 2000, the driest at x = −1500, the wettest at x = 1500.
 */
public final class GradientClimate implements ClimateSource {
    static final double MEAN_TEMPERATURE = 12;
    static final double TEMPERATURE_SPREAD = 18;
    static final double TEMPERATURE_PERIOD = 8000;
    static final double MEAN_HUMIDITY = 50;
    static final double HUMIDITY_SPREAD = 40;
    static final double HUMIDITY_PERIOD = 6000;

    @Override
    public Climate at(LevelReader level, BlockPos pos) {
        return at(pos.getX(), pos.getZ());
    }

    /** The climate at the column x, z. */
    public Climate at(int x, int z) {
        double temperature = MEAN_TEMPERATURE + TEMPERATURE_SPREAD * Math.sin(2 * Math.PI * z / TEMPERATURE_PERIOD);
        double humidity = MEAN_HUMIDITY + HUMIDITY_SPREAD * Math.sin(2 * Math.PI * x / HUMIDITY_PERIOD);
        return new Climate(temperature, humidity);
    }
}
