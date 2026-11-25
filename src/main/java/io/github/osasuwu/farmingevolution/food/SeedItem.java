package io.github.osasuwu.farmingevolution.food;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The seed: places its crop, and is wild food worth {@link WildFood#SEED_LOAD} load points. */
public final class SeedItem extends ItemNameBlockItem {
    public SeedItem(Block crop, Properties properties) {
        super(crop, properties.food(WildFood.SEED_FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        ItemStack left = super.finishUsingItem(stack, level, eater);
        WildFood.eaten(eater, WildFood.SEED_LOAD);
        return left;
    }
}
