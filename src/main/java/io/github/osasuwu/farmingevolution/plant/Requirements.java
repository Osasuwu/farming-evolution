package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Requirement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The raw-material requirements of pack data as item tags: the tag of a requirement holds the
 * products of the varieties that pass it, and nothing else. The product of a plant of no
 * variety is in none of them. The tags reach the game through {@link GeneratedPack}.
 */
public final class Requirements {
    /** The folder of the requirement tags among the mod's item tags. */
    private static final String TAG_FOLDER = "requirement/";

    /** The item ids of each tag, by tag id. */
    private final Map<ResourceLocation, List<ResourceLocation>> tags = new LinkedHashMap<>();

    /**
     * Builds the tags. A requirement id that is not a valid tag path stops the game here,
     * like any other fault of pack data.
     */
    public Requirements(PackData data) {
        for (Requirement requirement : data.requirements().values()) {
            tags.put(tagId(requirement.id()), data.passing(requirement).stream()
                    .map(variety -> ResourceLocation.fromNamespaceAndPath(
                            FarmingEvolution.MOD_ID, Products.varietyItem(variety.id())))
                    .toList());
        }
    }

    /** The id of the item tag of a requirement: {@code farmingevolution:requirement/<requirement id>}. */
    public static ResourceLocation tagId(String requirementId) {
        return ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, TAG_FOLDER + requirementId);
    }

    /** The item ids of each requirement tag, by tag id. */
    public Map<ResourceLocation, List<ResourceLocation>> tags() {
        return tags;
    }
}
