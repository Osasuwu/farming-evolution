package io.github.osasuwu.farmingevolution.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SuspiciousStewRecipe;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The pack has no hostile mobs and no vanilla food (CONCEPT.md, "Игра"): the ways they reach
 * a world are listed in docs/vanilla-removal-paths.md, and the tests here are what that list
 * names as confirmation.
 *
 * <p>What counts as removed is written out here by item id, independently of the mod's rule
 * ({@code VanillaRemoval.isRemoved}): {@link #VANILLA_FOOD} is every item of Minecraft 1.21.1
 * a player can eat, {@link #VANILLA_CROPS} the seeds and harvest of vanilla crops that are
 * not food themselves.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VanillaRemovalGameTests {
    private static final Set<String> VANILLA_FOOD = Set.of(
            "apple", "baked_potato", "beef", "beetroot", "beetroot_soup", "bread", "cake", "carrot",
            "chicken", "chorus_fruit", "cod", "cooked_beef", "cooked_chicken", "cooked_cod",
            "cooked_mutton", "cooked_porkchop", "cooked_rabbit", "cooked_salmon", "cookie",
            "dried_kelp", "enchanted_golden_apple", "glow_berries", "golden_apple", "golden_carrot",
            "honey_bottle", "melon_slice", "mushroom_stew", "mutton", "ominous_bottle",
            "poisonous_potato", "porkchop", "potato", "pufferfish", "pumpkin_pie", "rabbit",
            "rabbit_stew", "rotten_flesh", "salmon", "spider_eye", "suspicious_stew",
            "sweet_berries", "tropical_fish");

    private static final Set<String> VANILLA_CROPS = Set.of(
            "wheat", "wheat_seeds", "beetroot_seeds", "melon_seeds", "pumpkin_seeds", "cocoa_beans",
            "nether_wart", "torchflower_seeds", "pitcher_pod");

    /** The keys under which recipes of Minecraft and Create write what they give. */
    private static final Set<String> RESULT_KEYS = Set.of("result", "results");

    private VanillaRemovalGameTests() {
    }

    /**
     * Every way a mob gets into a world ends in the level accepting the entity: natural
     * spawning, spawners, patrols, phantoms, structures, commands. So a hostile mob handed to
     * the level directly is not there afterwards, whatever asked for it; an animal is.
     */
    @GameTest(template = "empty")
    public static void hostileMobDoesNotEnterWorld(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        List<EntityType<?>> hostile = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                EntityType.SPIDER, EntityType.ENDERMAN, EntityType.WITCH, EntityType.SLIME, EntityType.DROWNED,
                EntityType.PHANTOM, EntityType.PILLAGER);
        for (EntityType<?> type : hostile) {
            helper.spawn(type, pos);
            helper.assertEntityNotPresent(type);
        }
        helper.spawn(EntityType.PIG, pos);
        helper.assertEntityPresent(EntityType.PIG);
        helper.succeed();
    }

    /**
     * The skeleton trap seats a skeleton on each horse before it adds them to the world. The
     * skeleton is kept out, and the horse comes without a rider, free for the player to mount.
     */
    @GameTest(template = "empty")
    public static void horseComesWithoutHostileRider(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        Entity horse = EntityType.SKELETON_HORSE.create(level);
        Entity skeleton = EntityType.SKELETON.create(level);
        if (horse == null || skeleton == null) {
            throw new GameTestAssertException("no horse or skeleton");
        }
        horse.moveTo(at);
        skeleton.moveTo(at);
        skeleton.startRiding(horse);
        level.addFreshEntityWithPassengers(horse);
        helper.assertEntityPresent(EntityType.SKELETON_HORSE);
        helper.assertEntityNotPresent(EntityType.SKELETON);
        if (!horse.getPassengers().isEmpty()) {
            throw new GameTestAssertException("horse keeps a rider: " + horse.getPassengers());
        }
        horse.discard();
        helper.succeed();
    }

    /**
     * Lightning turns a villager into a witch and a pig into a zombified piglin: the game adds
     * the new mob and takes the old one away. The world accepts no hostile mob, so the
     * conversion must not start, or the animal would just vanish.
     */
    @GameTest(template = "empty")
    public static void lightningTurnsNoAnimalIntoHostileMob(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 2, 1);
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.VILLAGER, EntityType.PIG)) {
            Mob mob = helper.spawn(type, pos);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                throw new GameTestAssertException("no lightning bolt");
            }
            bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(pos)));
            mob.thunderHit(level, bolt);
            if (mob.isRemoved()) {
                throw new GameTestAssertException(BuiltInRegistries.ENTITY_TYPE.getKey(type)
                        + " struck by lightning left the world");
            }
            mob.discard();
        }
        helper.succeed();
    }

    /**
     * A night of natural spawning. The game test server keeps mob spawning off, so the test
     * runs the game's own spawner, the one each chunk near a player runs every tick, at
     * midnight, with a player 40 blocks away (spawning needs one between 24 and 128 blocks),
     * as many times as a chunk tries in a whole night: one attempt per tick, 12000 ticks. On
     * open ground at midnight Minecraft spawns zombies, skeletons, spiders and creepers there.
     */
    @GameTest(template = "empty")
    public static void nightOfNaturalSpawningBringsNoHostileMob(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spawnAt = helper.absolutePos(new BlockPos(1, 0, 1));
        if (!level.getBlockState(spawnAt).isAir() || !level.getBlockState(spawnAt.below()).isSolid()) {
            throw new GameTestAssertException("Expected open ground at " + spawnAt);
        }
        // A player in the level, not logged in: spawning only looks for the nearest player of
        // the level, and a login would make other mods send data to a connection that is not there.
        ServerPlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "spawn-test-player"));
        player.moveTo(spawnAt.getX() + 40.5, spawnAt.getY(), spawnAt.getZ() + 0.5);
        level.addNewPlayer(player);
        long dayTime = level.getDayTime();
        try {
            if (level.getNearestPlayer(spawnAt.getX(), spawnAt.getY(), spawnAt.getZ(), -1, false) != player) {
                throw new GameTestAssertException("The spawner would not see the test player");
            }
            level.setDayTime(18000);
            level.updateSkyBrightness();
            for (int tick = 0; tick < 12000; tick++) {
                NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, spawnAt);
            }
        } finally {
            level.setDayTime(dayTime);
            level.updateSkyBrightness();
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        List<String> hostile = level.getEntities((Entity) null, new AABB(spawnAt).inflate(16),
                        entity -> entity.getType().getCategory() == MobCategory.MONSTER).stream()
                .map(entity -> EntityType.getKey(entity.getType()).toString()).toList();
        if (!hostile.isEmpty()) {
            throw new GameTestAssertException("A night of spawning brought " + hostile);
        }
        helper.succeed();
    }

    /**
     * Inside some structures natural spawning reads the structure's own list instead of the
     * biome's: the witch of a swamp hut, the guardian of an ocean monument, the pillager of an
     * outpost, the mobs of a nether fortress. What it gets there holds no hostile mob.
     */
    @GameTest(template = "empty")
    public static void structureSpawnOverridesBringNoHostileMob(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<String> spawning = new TreeSet<>();
        int checked = 0;
        for (ResourceKey<Structure> key : List.of(BuiltinStructures.SWAMP_HUT, BuiltinStructures.OCEAN_MONUMENT,
                BuiltinStructures.PILLAGER_OUTPOST, BuiltinStructures.FORTRESS)) {
            StructureSpawnOverride override = structures.getOrThrow(key).spawnOverrides().get(MobCategory.MONSTER);
            if (override == null || override.spawns().isEmpty()) {
                throw new GameTestAssertException("Structure " + key.location() + " has no hostile spawn override to filter");
            }
            checked++;
            EventHooks.getPotentialSpawns(level, MobCategory.MONSTER, helper.absolutePos(BlockPos.ZERO), override.spawns())
                    .unwrap().forEach(data -> spawning.add(key.location() + " -> " + EntityType.getKey(data.type)));
        }
        if (checked != 4 || !spawning.isEmpty()) {
            throw new GameTestAssertException("Structures still spawn " + spawning);
        }
        helper.succeed();
    }

    /**
     * Natural spawning reads the list of a biome: with nothing in the hostile category the
     * game does not even try. Animals stay in the lists.
     */
    @GameTest(template = "empty")
    public static void noBiomeListsHostileMobsForNaturalSpawning(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        if (biomes.getOrThrow(Biomes.PLAINS).getMobSettings().getMobs(MobCategory.CREATURE).isEmpty()) {
            throw new GameTestAssertException("Plains list no animals for natural spawning");
        }
        Set<String> listing = new TreeSet<>();
        biomes.entrySet().forEach(entry -> {
            if (!entry.getValue().getMobSettings().getMobs(MobCategory.MONSTER).isEmpty()) {
                listing.add(entry.getKey().location().toString());
            }
        });
        if (!listing.isEmpty()) {
            throw new GameTestAssertException("Biomes that still spawn hostile mobs: " + listing);
        }
        helper.succeed();
    }

    /**
     * No recipe of any type, Create's included, gives vanilla food or a vanilla crop. The
     * suspicious stew is checked by its recipe class: a special recipe names no result.
     */
    @GameTest(template = "empty")
    public static void noRecipeGivesVanillaFoodOrCrop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        if (level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("stick")).isEmpty()) {
            throw new GameTestAssertException("The recipe of sticks is gone: recipes are not loaded");
        }
        Set<String> giving = new TreeSet<>();
        for (RecipeHolder<?> recipe : level.getRecipeManager().getRecipes()) {
            ItemStack result = recipe.value().getResultItem(level.registryAccess());
            JsonElement json = Recipe.CODEC.encodeStart(
                    level.registryAccess().createSerializationContext(JsonOps.INSTANCE), recipe.value()).getOrThrow();
            if (removed(result) || namesRemovedResult(json, false) || recipe.value() instanceof SuspiciousStewRecipe) {
                giving.add(recipe.id().toString());
            }
        }
        if (!giving.isEmpty()) {
            throw new GameTestAssertException("Recipes that give vanilla food or crops: " + giving);
        }
        helper.succeed();
    }

    /**
     * Grass broken by hand gives nothing: its only drop was wheat seeds. Cut with shears it
     * still gives itself, so the drops of the block are filtered, not switched off.
     */
    @GameTest(template = "empty")
    public static void grassDropsNoVanillaCrop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState grass = Blocks.SHORT_GRASS.defaultBlockState();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(Blocks.SHORT_GRASS.getLootTable());
        List<String> byHand = roll(table, blockParams(level, grass, ItemStack.EMPTY), 400);
        if (!byHand.isEmpty()) {
            throw new GameTestAssertException("Grass broken by hand dropped " + new TreeSet<>(byHand));
        }
        List<String> sheared = roll(table, blockParams(level, grass, new ItemStack(Items.SHEARS)), 1);
        if (!sheared.equals(List.of("minecraft:short_grass"))) {
            throw new GameTestAssertException("Expected sheared grass to drop itself, got " + sheared);
        }
        helper.succeed();
    }

    /**
     * Loot of the other kinds has no vanilla food either: a village chest keeps the rest of
     * its loot, a cow keeps its leather, fishing for fish gives nothing.
     */
    @GameTest(template = "empty")
    public static void chestAnimalAndFishingLootHasNoVanillaFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 origin = helper.absoluteVec(new Vec3(1.5, 2, 1.5));

        List<String> chest = roll(lootTable(level, BuiltInLootTables.VILLAGE_PLAINS_HOUSE),
                new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                        .create(LootContextParamSets.CHEST), 100);
        assertNoneRemoved("A village house chest", chest);
        if (!chest.contains("minecraft:emerald")) {
            throw new GameTestAssertException("100 village house chests held no emerald: " + new TreeSet<>(chest));
        }

        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        List<String> cowLoot = roll(lootTable(level, cow.getLootTable()),
                new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, cow)
                        .withParameter(LootContextParams.ORIGIN, origin)
                        .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                        .create(LootContextParamSets.ENTITY), 100);
        cow.discard();
        assertNoneRemoved("A cow", cowLoot);
        if (!cowLoot.contains("minecraft:leather")) {
            throw new GameTestAssertException("100 cows dropped no leather: " + new TreeSet<>(cowLoot));
        }

        List<String> fish = roll(lootTable(level, BuiltInLootTables.FISHING_FISH),
                new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                        .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                        .create(LootContextParamSets.FISHING), 100);
        if (!fish.isEmpty()) {
            throw new GameTestAssertException("Fishing gave " + new TreeSet<>(fish));
        }
        helper.succeed();
    }

    /**
     * No trade of a villager or of the wandering trader takes or gives vanilla food or crops.
     * The cartographer is left out: its listings search the world for structures, and it
     * trades maps, banners and paper.
     */
    @GameTest(template = "empty")
    public static void villagersTradeNoVanillaFoodOrCrop(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        List<VillagerTrades.ItemListing> listings = new ArrayList<>();
        VillagerTrades.TRADES.forEach((profession, levels) -> {
            if (profession != VillagerProfession.CARTOGRAPHER) {
                levels.values().forEach(level -> listings.addAll(List.of(level)));
            }
        });
        VillagerTrades.WANDERING_TRADER_TRADES.values().forEach(level -> listings.addAll(List.of(level)));

        Set<String> traded = new TreeSet<>();
        int offers = 0;
        for (VillagerTrades.ItemListing listing : listings) {
            for (int attempt = 0; attempt < 5; attempt++) {
                MerchantOffer offer = listing.getOffer(villager, villager.getRandom());
                if (offer == null) {
                    continue;
                }
                offers++;
                Stream.of(offer.getBaseCostA(), offer.getCostB(), offer.getResult())
                        .filter(VanillaRemovalGameTests::removed)
                        .forEach(stack -> traded.add(id(stack.getItem())));
            }
        }
        villager.discard();
        if (!traded.isEmpty()) {
            throw new GameTestAssertException("Villagers still trade " + traded);
        }
        if (offers < 100) {
            throw new GameTestAssertException("Expected the other trades to stay, got " + offers + " offers");
        }
        helper.succeed();
    }

    /**
     * A sweet berry bush hands its berries to the world without a loot table. Picking the
     * ripe bush resets it, and no berries lie around afterwards.
     */
    @GameTest(template = "empty")
    public static void sweetBerryBushGivesNoBerries(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(pos, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        helper.useBlock(pos);
        helper.assertBlockProperty(pos, SweetBerryBushBlock.AGE, 1);
        helper.assertItemEntityNotPresent(Items.SWEET_BERRIES);
        helper.spawnItem(Items.STICK, pos.above());
        helper.assertItemEntityPresent(Items.STICK);
        helper.succeed();
    }

    /**
     * The mod's rule of what is removed, asked about every registered item, names exactly the
     * items written out at the top of this class: all vanilla food and crops and nothing of
     * another mod.
     */
    @GameTest(template = "empty")
    public static void removedItemsAreExactlyVanillaFoodAndCrops(GameTestHelper helper) {
        Set<String> expected = Stream.concat(VANILLA_FOOD.stream(), VANILLA_CROPS.stream())
                .map(path -> "minecraft:" + path).collect(Collectors.toCollection(TreeSet::new));
        Set<String> actual = BuiltInRegistries.ITEM.stream()
                .filter(item -> io.github.osasuwu.farmingevolution.vanilla.VanillaRemoval.isRemoved(new ItemStack(item)))
                .map(VanillaRemovalGameTests::id).collect(Collectors.toCollection(TreeSet::new));
        if (!actual.equals(expected)) {
            Set<String> missing = new TreeSet<>(expected);
            missing.removeAll(actual);
            Set<String> extra = new TreeSet<>(actual);
            extra.removeAll(expected);
            throw new GameTestAssertException("Removed items differ: not removed " + missing + ", removed in excess " + extra);
        }
        helper.succeed();
    }

    private static boolean removed(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("minecraft")
                && (VANILLA_FOOD.contains(id.getPath()) || VANILLA_CROPS.contains(id.getPath()));
    }

    /**
     * Whether the recipe, as its own codec writes it, names a removed item anywhere under a
     * key of results. This sees what getResultItem does not: the second and later results of
     * a Create processing recipe.
     */
    private static boolean namesRemovedResult(JsonElement json, boolean underResult) {
        if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                if (namesRemovedResult(entry.getValue(), underResult || RESULT_KEYS.contains(entry.getKey()))) {
                    return true;
                }
            }
            return false;
        }
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                if (namesRemovedResult(element, underResult)) {
                    return true;
                }
            }
            return false;
        }
        if (!underResult || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(json.getAsString());
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
                && removed(new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    private static void assertNoneRemoved(String source, List<String> loot) {
        Set<String> found = loot.stream()
                .filter(id -> removed(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!found.isEmpty()) {
            throw new GameTestAssertException(source + " gave " + found);
        }
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static LootTable lootTable(ServerLevel level, ResourceKey<LootTable> key) {
        return level.getServer().reloadableRegistries().getLootTable(key);
    }

    private static LootParams blockParams(ServerLevel level, BlockState state, ItemStack tool) {
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .create(LootContextParamSets.BLOCK);
    }

    /**
     * The ids of everything the table gives over the given number of rolls. One random source
     * for all the rolls: sources seeded with consecutive numbers open with nearly the same
     * value, so a seed per roll would roll the same chance every time.
     */
    private static List<String> roll(LootTable table, LootParams params, int rolls) {
        RandomSource random = RandomSource.create(29);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            table.getRandomItems(params, random).forEach(stack -> items.add(id(stack.getItem())));
        }
        return items;
    }
}
