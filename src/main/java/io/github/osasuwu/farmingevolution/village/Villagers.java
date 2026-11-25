package io.github.osasuwu.farmingevolution.village;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Villagers and the player's crops (issue #88).
 *
 * <ul>
 * <li>A villager never griefs, as if the {@code mobGriefing} game rule were off for villagers
 * alone. A farmer at work breaks every ripe {@code CropBlock} it stands at, the plant of the mod
 * included, and drops its harvest on the ground; a villager that falls onto farmland tramples
 * it, and the plant on it breaks. Both ask the game rule through
 * {@link EntityMobGriefingEvent}, which is why the answer is given there: the pack has no
 * vanilla crop or seed a farmer could tend, so farming is all a farmer loses. The rule also
 * keeps villagers from picking up items, and the items they pick up are vanilla food and
 * seeds, which the pack does not have, and bone meal.</li>
 * <li>A novice farmer buys the product of a plant that fits no variety for an emerald and sells
 * bone meal for an emerald. Every one of its vanilla novice trades is food or a crop
 * ({@code VanillaRemoval}), so without these it had none. Seeds and wild berries are food, and
 * the pack trades no food, so they are not traded.</li>
 * </ul>
 */
public final class Villagers {
    /** Products of one species a farmer takes for one emerald. */
    static final int PRODUCTS_PER_EMERALD = 10;

    /** Bone meal a farmer gives for one emerald. */
    static final int BONE_MEAL_PER_EMERALD = 3;

    private Villagers() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Villagers::keepFromGriefing);
        NeoForge.EVENT_BUS.addListener(Villagers::addFarmerTrades);
    }

    private static void keepFromGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() instanceof Villager) {
            event.setCanGrief(false);
        }
    }

    private static void addFarmerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.FARMER) {
            return;
        }
        List<ItemListing> novice = event.getTrades().get(1);
        novice.add(Villagers::buyProduct);
        novice.add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 1),
                new ItemStack(Items.BONE_MEAL, BONE_MEAL_PER_EMERALD), 12, 1, 0.05F));
    }

    /**
     * The product of a plant of one species that fits no variety, for an emerald; the species
     * is picked for each farmer. The product of a variety is not bought: it is what breeding
     * works for.
     */
    private static MerchantOffer buyProduct(Entity trader, RandomSource random) {
        List<Item> products = FarmingEvolution.PRODUCTS.ofSpecies();
        Item product = products.get(random.nextInt(products.size()));
        return new MerchantOffer(new ItemCost(product, PRODUCTS_PER_EMERALD), new ItemStack(Items.EMERALD), 16, 2, 0.05F);
    }
}
