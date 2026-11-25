package io.github.osasuwu.farmingevolution;

import io.github.osasuwu.farmingevolution.plant.WildFlaxBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The creative tab of the mod (issue #80). JEI lists the items of the creative tabs, so this
 * tab is also what puts the mod's items into JEI's list and search. It holds every item
 * registered under the mod's namespace, read from the item registry when the contents are
 * built: an item the mod registers later is in it with no list to edit.
 */
public final class CreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FarmingEvolution.MOD_ID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + FarmingEvolution.MOD_ID))
                .icon(() -> new ItemStack(FarmingEvolution.SEEDS.get()))
                // In the registry's order, the order the mod registers its items in.
                .displayItems((parameters, output) -> BuiltInRegistries.ITEM.stream()
                        .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FarmingEvolution.MOD_ID))
                        .map(CreativeTab::stack)
                        .forEach(output::accept))
                .build());
    }

    private CreativeTab() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    /**
     * The stack of an item in the tab. Seeds are shown as the world gives them, with the
     * genome of the wild type: a seed with no genome grows a plant with no species, which
     * gives no product (CONCEPT.md, "Хранение генома").
     */
    private static ItemStack stack(Item item) {
        return item == FarmingEvolution.SEEDS.get() ? WildFlaxBlock.wildSeeds(1) : new ItemStack(item);
    }
}
