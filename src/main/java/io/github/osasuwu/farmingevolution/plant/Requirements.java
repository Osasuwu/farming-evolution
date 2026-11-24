package io.github.osasuwu.farmingevolution.plant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Requirement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * The raw-material requirements of pack data as item tags: the tag of a requirement holds the
 * products of the varieties that pass it, and nothing else. The product of a plant of no
 * variety is in none of them. The tags are ordinary ones for recipes, machines and KubeJS:
 * they come from a data pack the mod builds in memory from pack data when it is loaded, which
 * is always on.
 */
public final class Requirements {
    /** The folder of the requirement tags among the mod's item tags. */
    private static final String TAG_FOLDER = "requirement/";

    private static final String PACK_ID = FarmingEvolution.MOD_ID + ":requirements";

    /** The tag files of the data pack, by their location in it. */
    private final Map<ResourceLocation, byte[]> files = new LinkedHashMap<>();

    /**
     * Builds the tags. A requirement id that is not a valid tag path stops the game here,
     * like any other fault of pack data.
     */
    public Requirements(PackData data) {
        for (Requirement requirement : data.requirements().values()) {
            ResourceLocation id = tagId(requirement.id());
            JsonArray values = new JsonArray();
            data.passing(requirement).forEach(variety -> values.add(ResourceLocation.fromNamespaceAndPath(
                    FarmingEvolution.MOD_ID, Products.varietyItem(variety.id())).toString()));
            JsonObject tag = new JsonObject();
            tag.add("values", values);
            files.put(id.withPath(path -> Registries.tagsDirPath(Registries.ITEM) + "/" + path + ".json"),
                    tag.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The id of the item tag of a requirement: {@code farmingevolution:requirement/<requirement id>}. */
    public static ResourceLocation tagId(String requirementId) {
        return ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, TAG_FOLDER + requirementId);
    }

    /** Adds the data pack with the tags to the packs of the server. */
    public void addPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        PackLocationInfo location = new PackLocationInfo(
                PACK_ID, Component.literal("Farming Evolution raw-material requirements"), PackSource.BUILT_IN,
                Optional.empty());
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return new TagPack(info, files);
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new TagPack(info, files);
            }
        };
        Pack pack = new Pack(location, resources,
                new Pack.Metadata(Component.literal("Item tags of the raw-material requirements of pack data"),
                        PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), true),
                new PackSelectionConfig(true, Pack.Position.TOP, false));
        event.addRepositorySource(packs -> packs.accept(pack));
    }

    /** A data pack that holds the given files and nothing else. */
    private record TagPack(PackLocationInfo location, Map<ResourceLocation, byte[]> files) implements PackResources {
        @Override
        @Nullable
        public IoSupplier<InputStream> getRootResource(String... elements) {
            return null;
        }

        @Override
        @Nullable
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation file) {
            byte[] content = type == PackType.SERVER_DATA ? files.get(file) : null;
            return content == null ? null : () -> new ByteArrayInputStream(content);
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type != PackType.SERVER_DATA) {
                return;
            }
            files.forEach((file, content) -> {
                if (file.getNamespace().equals(namespace) && file.getPath().startsWith(path + "/")) {
                    output.accept(file, () -> new ByteArrayInputStream(content));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return type == PackType.SERVER_DATA && !files.isEmpty() ? Set.of(FarmingEvolution.MOD_ID) : Set.of();
        }

        @Override
        @Nullable
        public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
            // The pack is described where it is created; it has no pack.mcmeta to read.
            return null;
        }

        @Override
        public void close() {
        }
    }
}
