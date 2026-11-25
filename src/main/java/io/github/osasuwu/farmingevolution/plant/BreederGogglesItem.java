package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The breeder's goggles, the equipment of the first look at a plant (CONCEPT.md, "Методы
 * селекции"), after the engineer's goggles of Create. Worn on the head, they show the player
 * what the eye can tell: the plant looked at (the species and the expressed value of each
 * trait, product, ripeness), the two numbers of the climate of the player's place, and the
 * expressed genome of a seed in its tooltip. The carried allele is not theirs (issue #121). The odds of the
 * offspring are not theirs: those are the analyzer's.
 *
 * <p>The HUD is drawn by the client (client.GogglesHud); what it draws is {@link Readout}.
 */
public final class BreederGogglesItem extends Item implements Equipable {
    public BreederGogglesItem(Properties properties) {
        super(properties);
    }

    /** Whether the entity wears the goggles: on its head, not in a hand. */
    public static boolean worn(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(FarmingEvolution.BREEDER_GOGGLES.get());
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    /** Use puts the goggles on, as with a helmet. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    /**
     * The genome of a seed under its name, for a player who wears the goggles. Without them, or
     * with no player (a tooltip made outside play), the tooltip stays the name alone.
     */
    public static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || !worn(player) || !stack.is(FarmingEvolution.SEEDS.get())) {
            return;
        }
        Individual genome = stack.get(FarmingEvolution.GENOME);
        if (genome == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), Readout.seed(genome));
    }
}
