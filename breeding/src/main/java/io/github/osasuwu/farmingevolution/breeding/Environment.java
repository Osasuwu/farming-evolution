package io.github.osasuwu.farmingevolution.breeding;

import java.util.Map;

/**
 * Where a cross happens, as far as mutation conditions care: named climate readings
 * (temperature, rainfall, ...) and the soil id, or null when there is no soil.
 */
public record Environment(Map<String, Double> climate, String soil) {
    public static final Environment NONE = new Environment(Map.of(), null);

    public Environment {
        climate = Map.copyOf(climate);
    }
}
