package io.github.osasuwu.farmingevolution.food;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Wild berries: wild food worth {@link WildFood#BERRY_LOAD} load points. */
public final class WildBerriesItem extends Item {
    public WildBerriesItem(Properties properties) {
        super(properties.food(WildFood.BERRY_FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        ItemStack left = super.finishUsingItem(stack, level, eater);
        WildFood.eaten(eater, WildFood.BERRY_LOAD);
        return left;
    }
}
