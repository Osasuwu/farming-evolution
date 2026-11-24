package io.github.osasuwu.farmingevolution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.PackDataException;
import io.github.osasuwu.farmingevolution.climate.ClimateSource;
import io.github.osasuwu.farmingevolution.climate.GradientClimate;
import io.github.osasuwu.farmingevolution.plant.AnalyzerItem;
import io.github.osasuwu.farmingevolution.plant.GenomeCodec;
import io.github.osasuwu.farmingevolution.plant.PlantBlock;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.Products;
import io.github.osasuwu.farmingevolution.plant.RemovedPlants;
import io.github.osasuwu.farmingevolution.plant.Requirements;
import io.github.osasuwu.farmingevolution.plant.WildBerryBushBlock;
import io.github.osasuwu.farmingevolution.plant.WildFlaxBlock;
import io.github.osasuwu.farmingevolution.recipe.RecipeGraphDump;
import io.github.osasuwu.farmingevolution.vanilla.VanillaRemoval;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(FarmingEvolution.MOD_ID)
public final class FarmingEvolution {
    public static final String MOD_ID = "farmingevolution";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    /**
     * Pack data: species, alleles and varieties. Read once, when the mod is loaded, because
     * the set of items follows from it; a fault in it stops the game here.
     */
    public static final PackData PACK = readPack();

    /** The inheritance engine over {@link #PACK}. */
    public static final Breeding BREEDING = new Breeding(PACK);

    /** The product items of the varieties and species of {@link #PACK}. */
    public static final Products PRODUCTS = new Products(PACK, BREEDING, ITEMS);

    /** The item tags of the raw-material requirements of {@link #PACK}. */
    public static final Requirements REQUIREMENTS = new Requirements(PACK);

    /** Where the mod reads the climate of a place: the climate stub of the vertical slice. */
    public static final ClimateSource CLIMATE = new GradientClimate();

    /**
     * Plant fibre of the prototype: a plain item the check pipeline sends through a Create
     * machine (CreateMachineGameTests). No plant gives it and no requirement takes it; recipes
     * take the products through a raw-material requirement ({@link #REQUIREMENTS}).
     */
    public static final DeferredItem<Item> FIBER = ITEMS.registerSimpleItem("fiber");

    /**
     * The genome of a seed, on the item stack. A growing plant keeps the same value in its
     * block entity ({@link PlantBlockEntity}) and exposes it under this component.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Individual>> GENOME =
            DATA_COMPONENTS.registerComponentType("genome", genome -> genome
                    .persistent(GenomeCodec.saved(BREEDING))
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(GenomeCodec.INDIVIDUAL)));

    /**
     * The pollen an {@link AnalyzerItem analyzer} carries: the genome of the plant it was taken
     * from. A pollinated plant keeps the same value in its block entity.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Individual>> POLLEN =
            DATA_COMPONENTS.registerComponentType("pollen", pollen -> pollen
                    .persistent(GenomeCodec.saved(BREEDING))
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(GenomeCodec.INDIVIDUAL)));

    /** The equipment of crossing: shows alleles and odds, carries pollen. */
    public static final DeferredItem<AnalyzerItem> ANALYZER = ITEMS.register("analyzer",
            () -> new AnalyzerItem(new Item.Properties().stacksTo(1)));

    /**
     * The growing plant of the storage prototype: one crop block for every species, since
     * the species is a chromosome of the genome.
     */
    public static final DeferredBlock<PlantBlock> CROP = BLOCKS.register("crop", () -> new PlantBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY)));

    /**
     * The seed: the item of the crop block, so placing it plants the crop. It is also food,
     * the slice's food of the same line (CONCEPT.md, "Вертикальный срез").
     */
    public static final DeferredItem<ItemNameBlockItem> SEEDS = ITEMS.register("seeds", () -> new ItemNameBlockItem(
            CROP.get(), new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build())));

    /** The wild source of the flax line, found in the world; see {@link WildFlaxBlock}. */
    public static final DeferredBlock<WildFlaxBlock> WILD_FLAX = BLOCKS.register("wild_flax", () -> new WildFlaxBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)));

    /** A wild bush that gives {@link #WILD_BERRIES}; see {@link WildBerryBushBlock}. */
    public static final DeferredBlock<WildBerryBushBlock> WILD_BERRY_BUSH = BLOCKS.register("wild_berry_bush",
            () -> new WildBerryBushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<Item> WILD_BERRIES = ITEMS.registerSimpleItem("wild_berries",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlantBlockEntity>> PLANT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("plant", () -> BlockEntityType.Builder.of(PlantBlockEntity::new, CROP.get()).build(null));

    /** Per level and not saved: what {@link PlantBlock} hands from a removed plant to its replacement. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RemovedPlants>> REMOVED_PLANTS =
            ATTACHMENTS.register("removed_plants", () -> AttachmentType.builder(RemovedPlants::new).build());

    public FarmingEvolution(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(REQUIREMENTS::addPack);
        NeoForge.EVENT_BUS.addListener(RecipeGraphDump::onServerStarted);
        VanillaRemoval.register(modBus);
    }

    /**
     * The system property naming a file of pack data added to the mod's own. Only the GameTest
     * run sets it (build.gradle, run gameTestServer): the species and varieties some GameTests
     * need but a player must not see live there (src/gametest/pack.json), outside the jar.
     */
    private static final String GAME_TEST_PACK_PROPERTY = MOD_ID + ".gameTestPackData";

    private static PackData readPack() {
        String path = "/" + MOD_ID + "/pack.json";
        JsonObject root;
        try (InputStream stream = FarmingEvolution.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("The mod has no pack data at " + path);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = object(reader, path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Pack data at " + path + " could not be read", e);
        }
        String extra = System.getProperty(GAME_TEST_PACK_PROPERTY);
        if (extra != null) {
            path += " with " + extra;
            addEntries(root, extra);
        }
        PackData pack;
        try {
            pack = PackData.parse(new StringReader(root.toString()));
        } catch (PackDataException e) {
            throw new PackDataException("pack data at " + path + ": " + e.getMessage());
        }
        if (pack.species().isEmpty()) {
            throw new IllegalStateException("Pack data at " + path + " has no species");
        }
        return pack;
    }

    /** Appends every list of the pack data file to the list of the same name in the root. */
    private static void addEntries(JsonObject root, String file) {
        try (Reader reader = Files.newBufferedReader(Path.of(file), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, JsonElement> list : object(reader, file).entrySet()) {
                if (!list.getValue().isJsonArray()) {
                    throw new PackDataException("pack data at " + file + ": " + list.getKey() + " is not a list");
                }
                if (!root.has(list.getKey())) {
                    root.add(list.getKey(), new JsonArray());
                }
                if (!root.get(list.getKey()).isJsonArray()) {
                    throw new PackDataException("pack data: " + list.getKey() + " is not a list");
                }
                root.getAsJsonArray(list.getKey()).addAll(list.getValue().getAsJsonArray());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Pack data at " + file + " could not be read", e);
        }
    }

    /** The JSON object a pack data file holds; anything else stops the game with the file named. */
    private static JsonObject object(Reader reader, String file) {
        try {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new PackDataException("malformed pack data at " + file + ": not a JSON object");
            }
            return element.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new PackDataException("malformed pack data at " + file + ": " + e);
        }
    }
}
