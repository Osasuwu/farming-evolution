package io.github.osasuwu.farmingevolution;

import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.plant.GenomeCodec;
import io.github.osasuwu.farmingevolution.plant.PlantBlock;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.Products;
import io.github.osasuwu.farmingevolution.plant.RemovedPlants;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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
    public static final Products PRODUCTS = new Products(PACK, ITEMS);

    /**
     * Plant fibre of the prototype: a plain item the check pipeline sends through a Create
     * machine. No plant gives it any more, a harvest gives one of {@link #PRODUCTS}; the item
     * stays until recipes take the products through a raw-material requirement (issue #47).
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

    /** The seed: the item of the crop block, so placing it plants the crop. */
    public static final DeferredItem<ItemNameBlockItem> SEEDS =
            ITEMS.register("seeds", () -> new ItemNameBlockItem(CROP.get(), new Item.Properties()));

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
    }

    private static PackData readPack() {
        String path = "/" + MOD_ID + "/pack.json";
        try (InputStream stream = FarmingEvolution.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("The mod has no pack data at " + path);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return PackData.parse(reader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Pack data at " + path + " could not be read", e);
        }
    }
}
