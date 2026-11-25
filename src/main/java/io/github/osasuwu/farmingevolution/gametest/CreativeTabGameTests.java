package io.github.osasuwu.farmingevolution.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The creative tab of the mod (issue #80): JEI lists the items of the creative tabs, so an item
 * that is in no tab is not in JEI. The tab holds every item the mod registers, taken from the
 * item registry, so the products of the GameTest-only pack data (hemp) are in it here too.
 * Building the tab's contents is common code; the client builds them the same way.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTests {
    private static final ResourceLocation TAB = ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "main");

    private CreativeTabGameTests() {
    }

    /**
     * The tab exists, is named in the language file and shows an item of the mod as its icon,
     * and holds exactly the items of the mod's namespace, each once: the ones of the mod's own
     * pack data and those the GameTest pack data adds without any list naming them.
     */
    @GameTest(template = "empty")
    public static void tabHoldsEveryItemOfTheModAndHasNameAndIcon(GameTestHelper helper) {
        CreativeModeTab tab = tab();
        Collection<ItemStack> shown = build(helper, tab);

        if (!(tab.getDisplayName().getContents() instanceof TranslatableContents title)) {
            throw new GameTestAssertException("Expected a translated tab title, got " + tab.getDisplayName());
        }
        String name = english().has(title.getKey()) ? english().get(title.getKey()).getAsString() : null;
        if (!"Farming Evolution".equals(name)) {
            throw new GameTestAssertException("Expected the title key " + title.getKey()
                    + " to read \"Farming Evolution\" in en_us.json, got " + name);
        }
        ItemStack icon = tab.getIconItem();
        if (icon.isEmpty() || !BuiltInRegistries.ITEM.getKey(icon.getItem()).getNamespace().equals(FarmingEvolution.MOD_ID)) {
            throw new GameTestAssertException("Expected an item of the mod as the tab icon, got " + icon);
        }

        List<ResourceLocation> listed = shown.stream().map(s -> BuiltInRegistries.ITEM.getKey(s.getItem())).toList();
        Set<ResourceLocation> registered = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(FarmingEvolution.MOD_ID))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // The closed set the issue names, and products of the GameTest-only pack data: no list in
        // the mod names those, so they are in the tab only if it reads the registry.
        for (String required : List.of("analyzer", "seed_store", "seeds", "strong_flax", "wild_berries",
                "strong_hemp", "hemp_fiber")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, required);
            if (!registered.contains(id)) {
                throw new GameTestAssertException(id + " is not registered");
            }
        }
        if (listed.size() != new LinkedHashSet<>(listed).size() || !new LinkedHashSet<>(listed).equals(registered)) {
            List<ResourceLocation> missing = new ArrayList<>(registered);
            missing.removeAll(listed);
            List<ResourceLocation> extra = new ArrayList<>(listed);
            extra.removeAll(registered);
            throw new GameTestAssertException("Expected the tab to list every item of the mod once; missing "
                    + missing + ", not of the mod " + extra + ", listed " + listed);
        }
        helper.succeed();
    }

    /** The seeds in the tab are those the world gives: the drop of a broken wild flax, genome and all. */
    @GameTest(template = "empty")
    public static void seedsInTabAreSeedsOfBrokenWildFlax(GameTestHelper helper) {
        Collection<ItemStack> shown = build(helper, tab());
        List<ItemStack> seeds = shown.stream().filter(s -> s.is(FarmingEvolution.SEEDS.get())).toList();

        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        List<ItemStack> drops = Block.getDrops(FarmingEvolution.WILD_FLAX.get().defaultBlockState(), level, pos, null);
        if (drops.size() != 1 || !drops.getFirst().has(FarmingEvolution.GENOME.get())) {
            throw new GameTestAssertException("Expected one stack of seeds with a genome from a wild flax, got " + drops);
        }
        if (seeds.size() != 1 || !ItemStack.isSameItemSameComponents(seeds.getFirst(), drops.getFirst())) {
            throw new GameTestAssertException("Expected the tab's seeds to be the wild flax drop " + drops.getFirst()
                    + " with " + drops.getFirst().get(FarmingEvolution.GENOME.get()) + ", got " + seeds
                    + (seeds.isEmpty() ? "" : " with " + seeds.getFirst().get(FarmingEvolution.GENOME.get())));
        }
        helper.succeed();
    }

    private static CreativeModeTab tab() {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(TAB);
        if (tab == null) {
            throw new GameTestAssertException("No creative tab " + TAB);
        }
        return tab;
    }

    /** The contents of the tab as a player with commands sees them; nothing of the mod depends on that. */
    private static Collection<ItemStack> build(GameTestHelper helper, CreativeModeTab tab) {
        ServerLevel level = helper.getLevel();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        return tab.getDisplayItems();
    }

    /** The English language file of the mod, as the jar carries it. */
    private static JsonObject english() {
        String path = "/assets/" + FarmingEvolution.MOD_ID + "/lang/en_us.json";
        try (InputStream stream = CreativeTabGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new GameTestAssertException("No language file at " + path);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
