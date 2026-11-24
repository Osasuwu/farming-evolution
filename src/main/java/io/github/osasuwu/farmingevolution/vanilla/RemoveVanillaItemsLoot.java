package io.github.osasuwu.farmingevolution.vanilla;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Takes the removed items ({@link VanillaRemoval#isRemoved}) out of what any loot table
 * gives: block drops, mob drops, chests, fishing, gifts. Applied by
 * data/farmingevolution/loot_modifiers/remove_vanilla_items.json.
 */
public final class RemoveVanillaItemsLoot extends LootModifier {
    public static final MapCodec<RemoveVanillaItemsLoot> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, RemoveVanillaItemsLoot::new));

    private RemoveVanillaItemsLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(VanillaRemoval::isRemoved);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
