package io.github.osasuwu.farmingevolution.plant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/**
 * The game data that follows from pack data: the item tags of the raw-material requirements
 * ({@link Requirements}) and the composter values of the mod's plant items
 * ({@link #compostables}). They are ordinary data for recipes, machines and KubeJS: they come
 * from a data pack the mod builds in memory when it is loaded, which is always on. A product
 * added to pack data gets its compost value here with no list to edit.
 */
public final class GeneratedPack {
    /**
     * The id of the data pack. It is the id the pack had when it held only the requirement tags;
     * a world keeps the ids of its data packs, so the id stays.
     */
    private static final String PACK_ID = FarmingEvolution.MOD_ID + ":requirements";

    /** The files of the data pack, by their location in it. */
    private final Map<ResourceLocation, byte[]> files = new LinkedHashMap<>();

    /**
     * Builds the files: each item tag id with the ids of the items it holds, and the chance of
     * each compostable item to raise the composter's level.
     */
    public GeneratedPack(Map<ResourceLocation, List<ResourceLocation>> itemTags,
            Map<ResourceLocation, Float> compostables) {
        itemTags.forEach((id, items) -> {
            JsonArray values = new JsonArray();
            items.forEach(item -> values.add(item.toString()));
            JsonObject tag = new JsonObject();
            tag.add("values", values);
            add(id.withPath(path -> Registries.tagsDirPath(Registries.ITEM) + "/" + path + ".json"), tag);
        });
        JsonObject values = new JsonObject();
        compostables.forEach((item, chance) -> {
            JsonObject value = new JsonObject();
            value.addProperty("chance", chance);
            values.add(item.toString(), value);
        });
        JsonObject dataMap = new JsonObject();
        dataMap.add("values", values);
        // data/neoforge/data_maps/item/compostables.json, the file NeoForge reads the data map from.
        add(NeoForgeDataMaps.COMPOSTABLES.id().withPath(path -> "data_maps/item/" + path + ".json"), dataMap);
    }

    private void add(ResourceLocation file, JsonObject content) {
        if (files.put(file, content.toString().getBytes(StandardCharsets.UTF_8)) != null) {
            throw new IllegalStateException("Pack data gives two files " + file);
        }
    }

    /** Adds the data pack to the packs of the server. */
    public void addPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        PackLocationInfo location = new PackLocationInfo(
                PACK_ID, Component.literal("Farming Evolution data of pack data"), PackSource.BUILT_IN,
                Optional.empty());
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return new FilePack(info, files);
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new FilePack(info, files);
            }
        };
        Pack pack = new Pack(location, resources,
                new Pack.Metadata(Component.literal("Requirement tags and compost values of pack data"),
                        PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), true),
                new PackSelectionConfig(true, Pack.Position.TOP, false));
        event.addRepositorySource(packs -> packs.accept(pack));
    }

    /** A data pack that holds the given files and nothing else. */
    private record FilePack(PackLocationInfo location, Map<ResourceLocation, byte[]> files) implements PackResources {
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
            return type == PackType.SERVER_DATA
                    ? files.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toUnmodifiableSet())
                    : Set.of();
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
