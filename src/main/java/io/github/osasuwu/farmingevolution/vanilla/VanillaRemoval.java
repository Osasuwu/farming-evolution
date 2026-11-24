package io.github.osasuwu.farmingevolution.vanilla;

import com.mojang.serialization.MapCodec;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The pack has no hostile mobs and no vanilla food (CONCEPT.md, "Игра"). This class is the
 * mod's part of removing them; recipes are removed by the pack's KubeJS script
 * (kubejs/server_scripts/remove_vanilla_food.js). Every way in, with what closes it and what
 * confirms that, is listed in docs/vanilla-removal-paths.md.
 *
 * <ul>
 * <li>A hostile mob is an entity whose type is in the category {@link MobCategory#MONSTER}.
 * No biome lists such types for natural spawning ({@link RemoveHostileSpawns}), natural
 * spawning picks none of them anywhere, structures included, no mob turns into one, and a
 * level accepts no such entity, whatever creates it.</li>
 * <li>A removed item ({@link #isRemoved}) comes out of no loot table
 * ({@link RemoveVanillaItemsLoot}), is in no trade of a villager or the wandering trader, and
 * does not enter a level as a dropped item.</li>
 * </ul>
 */
public final class VanillaRemoval {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, FarmingEvolution.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, FarmingEvolution.MOD_ID);

    static {
        BIOME_MODIFIERS.register("remove_hostile_spawns", () -> RemoveHostileSpawns.CODEC);
        LOOT_MODIFIERS.register("remove_vanilla_items", () -> RemoveVanillaItemsLoot.CODEC);
    }

    private VanillaRemoval() {
    }

    public static void register(IEventBus modBus) {
        BIOME_MODIFIERS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(VanillaRemoval::keepOutOfLevel);
        NeoForge.EVENT_BUS.addListener(VanillaRemoval::keepFromTurningHostile);
        NeoForge.EVENT_BUS.addListener(VanillaRemoval::filterPotentialSpawns);
        NeoForge.EVENT_BUS.addListener(VanillaRemoval::filterVillagerTrades);
        NeoForge.EVENT_BUS.addListener(VanillaRemoval::filterWandererTrades);
    }

    /**
     * Whether the pack removes the item: an item of Minecraft itself that a player can eat, or
     * one of {@link Inedible#ITEMS}. Items of mods, this one included, are never removed.
     */
    public static boolean isRemoved(ItemStack stack) {
        Item item = stack.getItem();
        if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft")) {
            return false;
        }
        return item.components().has(DataComponents.FOOD) || Inedible.ITEMS.contains(item);
    }

    private static boolean isHostile(EntityType<?> type) {
        return type.getCategory() == MobCategory.MONSTER;
    }

    /**
     * Lightning turns a villager into a witch and a pig into a zombified piglin. The level would
     * refuse the new mob, and the old one would be gone anyway, so the conversion does not start.
     */
    private static void keepFromTurningHostile(LivingConversionEvent.Pre event) {
        if (isHostile(event.getOutcome())) {
            event.setCanceled(true);
        }
    }

    private static void keepOutOfLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (isHostile(entity.getType())) {
            // A mob kept out may already sit on a horse or carry a rider (the skeleton trap seats
            // its skeletons first); left seated, it would hold the other one forever.
            entity.stopRiding();
            entity.ejectPassengers();
            event.setCanceled(true);
        } else if (entity instanceof ItemEntity item && isRemoved(item.getItem())) {
            event.setCanceled(true);
        }
    }

    /**
     * What natural spawning may pick at a place: the list of the biome, or the spawn overrides
     * of a structure there (a witch hut, an ocean monument, a pillager outpost, a nether
     * fortress), which the biome modifier does not reach.
     */
    private static void filterPotentialSpawns(LevelEvent.PotentialSpawns event) {
        List.copyOf(event.getSpawnerDataList()).stream()
                .filter(data -> isHostile(data.type))
                .forEach(event::removeSpawnerData);
    }

    private static void filterVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(VanillaRemoval::filter);
    }

    private static void filterWandererTrades(WandererTradesEvent event) {
        filter(event.getGenericTrades());
        filter(event.getRareTrades());
    }

    /**
     * What a listing offers is known only once it makes the offer, so every listing is wrapped:
     * an offer that takes or gives a removed item is not made, and the trader gets one offer
     * fewer.
     */
    private static void filter(List<ItemListing> listings) {
        listings.replaceAll(listing -> (trader, random) -> {
            MerchantOffer offer = listing.getOffer(trader, random);
            if (offer == null
                    || isRemoved(offer.getBaseCostA()) || isRemoved(offer.getCostB()) || isRemoved(offer.getResult())) {
                return null;
            }
            return offer;
        });
    }

    /** Removed items a player cannot eat from the hand: the cake, and vanilla crops and their seeds. */
    private static final class Inedible {
        static final Set<Item> ITEMS = Set.of(
                Items.CAKE,
                Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS,
                Items.COCOA_BEANS, Items.NETHER_WART, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD);
    }
}
