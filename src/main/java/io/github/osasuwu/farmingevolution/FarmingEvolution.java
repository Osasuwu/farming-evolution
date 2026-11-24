package io.github.osasuwu.farmingevolution;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FarmingEvolution.MOD_ID)
public final class FarmingEvolution {
    public static final String MOD_ID = "farmingevolution";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    /**
     * Plant fibre: the product of the spinning-crop line. For now a plain item with no
     * behaviour of its own, which is what the check pipeline sends through a Create machine.
     */
    public static final DeferredItem<Item> FIBER = ITEMS.registerSimpleItem("fiber");

    public FarmingEvolution(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
