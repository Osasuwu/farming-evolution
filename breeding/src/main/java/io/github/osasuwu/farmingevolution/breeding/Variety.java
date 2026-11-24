package io.github.osasuwu.farmingevolution.breeding;

import java.util.Map;

/**
 * A variety: a species plus thresholds on one or more of its traits. A threshold is the
 * smallest trait value the variety accepts.
 */
public record Variety(String id, String species, Map<String, Integer> thresholds) {
}
