package io.github.osasuwu.farmingevolution.store;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The seed store (issue #30): a block that keeps seeds, each genome in its own slot, and lets
 * out only the seeds that pass the condition the player has set. Hoppers, Create funnels and
 * other mods reach it through the item capability of NeoForge, so the mod does not compile
 * against Create. See {@link SeedStoreBlock} for the player's side and
 * {@link SeedStoreBlockEntity} for the storage.
 */
public final class SeedStore {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FarmingEvolution.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FarmingEvolution.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FarmingEvolution.MOD_ID);

    public static final DeferredBlock<SeedStoreBlock> BLOCK = BLOCKS.register("seed_store", () -> new SeedStoreBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK, new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeedStoreBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("seed_store",
                    () -> BlockEntityType.Builder.of(SeedStoreBlockEntity::new, BLOCK.get()).build(null));

    private SeedStore() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(SeedStore::registerCapabilities);
    }

    /** The same storage on every side: what goes in and what may come out does not depend on the side. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLOCK_ENTITY.get(), (store, side) -> store.storage());
    }
}
