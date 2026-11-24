package io.github.osasuwu.farmingevolution.climate;

import java.util.Map;

/**
 * The climate of one place.
 *
 * @param temperature the mean air temperature, in degrees Celsius
 * @param humidity    the relative humidity of the air, in percent, 0 to 100
 */
public record Climate(double temperature, double humidity) {
    /** The name of the temperature reading in the climate conditions of pack data. */
    public static final String TEMPERATURE = "temperature";

    /** The name of the humidity reading in the climate conditions of pack data. */
    public static final String HUMIDITY = "humidity";

    /** The readings under the names the climate conditions of pack data use. */
    public Map<String, Double> readings() {
        return Map.of(TEMPERATURE, temperature, HUMIDITY, humidity);
    }
}
