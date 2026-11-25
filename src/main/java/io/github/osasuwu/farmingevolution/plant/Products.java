package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.PackData;
import io.github.osasuwu.farmingevolution.breeding.Variety;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The product items of pack data: one for every variety and one for every species, the
 * product of a plant that fits no variety of its species. They are plain items with no data
 * on them, so machines and autocrafting tell them apart by type. The set is closed once the
 * game has started.
 */
public final class Products {
    /** In the slice a variety is a variety of the fibre, so every product is a fibre. */
    private static final String NO_VARIETY_SUFFIX = "_fiber";

    private final Breeding breeding;
    private final Map<String, DeferredItem<Item>> ofVariety = new LinkedHashMap<>();
    private final Map<String, DeferredItem<Item>> ofSpecies = new LinkedHashMap<>();

    /**
     * Registers the items. An id that is not a valid item id, or two entries that give the
     * same one, stop the game here, like any other fault of pack data.
     */
    public Products(PackData data, Breeding breeding, DeferredRegister.Items items) {
        this.breeding = breeding;
        data.varieties().keySet().forEach(id -> ofVariety.put(id, items.registerSimpleItem(varietyItem(id))));
        data.species().keySet().forEach(id -> ofSpecies.put(id, items.registerSimpleItem(speciesItem(id))));
    }

    /** The path of the item id of a variety's product: the id of the variety. */
    public static String varietyItem(String varietyId) {
        return varietyId;
    }

    /** The path of the item id of the product of a plant of the species that fits no variety. */
    public static String speciesItem(String speciesId) {
        return speciesId + NO_VARIETY_SUFFIX;
    }

    /** The item ids of every product, of a variety and of a species. */
    public List<ResourceLocation> ids() {
        List<ResourceLocation> all = new ArrayList<>();
        ofVariety.values().forEach(item -> all.add(item.getId()));
        ofSpecies.values().forEach(item -> all.add(item.getId()));
        return List.copyOf(all);
    }

    /** The products of plants that fit no variety, one for each species; only once items are registered. */
    public List<Item> ofSpecies() {
        return ofSpecies.values().stream().map(DeferredItem::get).toList();
    }

    /**
     * What one harvest of the plant gives: the product of the narrowest variety it fits, or
     * the product of its species when it fits none. A genome pack data does not describe is
     * first brought to it the way one read from a save is ({@link Breeding#restore}): a
     * genome set by a command or by other code has not been through that reading.
     */
    public Item of(Individual plant) {
        Individual described = breeding.restore(plant, replaced -> { });
        return breeding.variety(described).map(Variety::id).map(ofVariety::get)
                .orElseGet(() -> ofSpecies.get(described.species())).get();
    }
}
