package io.github.osasuwu.farmingevolution.gametest;

import com.mojang.authlib.GameProfile;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Allele;
import io.github.osasuwu.farmingevolution.breeding.Breeding;
import io.github.osasuwu.farmingevolution.breeding.Chromosome;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.BreederGogglesItem;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.Readout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The breeder's goggles (issue #90): worn on the head, they show the player the plant of the
 * mod they look at (species, product, ripeness, never the odds of its offspring), the climate
 * of their place and, in the tooltip of a seed, its genome. The HUD itself is drawn by the
 * client and is checked on screenshots; what it draws is {@link Readout}, checked here.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BreederGogglesGameTests {
    private static final BlockPos PLANT = new BlockPos(1, 2, 1);

    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("0b6f3c1e-8a52-4d7e-9c41-2e7a5d90f318"), "fe-goggles");

    private BreederGogglesGameTests() {
    }

    /** Using the goggles puts them on the head; only there do they count as worn. */
    @GameTest(template = "empty")
    public static void gogglesGoOnTheHeadAndCountOnlyThere(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), PROFILE);
        if (BreederGogglesItem.worn(player)) {
            throw new GameTestAssertException("A player without goggles wears them");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, goggles());
        if (BreederGogglesItem.worn(player)) {
            throw new GameTestAssertException("Goggles held in the hand count as worn");
        }
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(FarmingEvolution.BREEDER_GOGGLES.get())
                || !player.getMainHandItem().isEmpty()) {
            throw new GameTestAssertException("Using the goggles did not put them on the head: head "
                    + player.getItemBySlot(EquipmentSlot.HEAD) + ", hand " + player.getMainHandItem());
        }
        if (!BreederGogglesItem.worn(player)) {
            throw new GameTestAssertException("Goggles on the head do not count as worn");
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.CARVED_PUMPKIN));
        if (BreederGogglesItem.worn(player)) {
            throw new GameTestAssertException("A pumpkin on the head counts as goggles");
        }
        helper.succeed();
    }

    /**
     * The goggles are made of what the first hour gives: two glass and the fibre of any flax,
     * wild or bred. A string is no fibre of flax.
     */
    @GameTest(template = "empty")
    public static void gogglesAreMadeOfGlassAndAnyFlaxFibre(GameTestHelper helper) {
        for (String fibre : List.of("wild_flax_fiber", "fiber_flax_fiber", "oil_flax_fiber", "strong_flax")) {
            ItemStack made = craft(helper, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, fibre))));
            if (!made.is(FarmingEvolution.BREEDER_GOGGLES.get()) || made.getCount() != 1) {
                throw new GameTestAssertException("Expected one pair of goggles from glass, " + fibre + ", glass, got " + made);
            }
        }
        ItemStack fromString = craft(helper, new ItemStack(Items.STRING));
        if (!fromString.isEmpty()) {
            throw new GameTestAssertException("Glass, string, glass made " + fromString);
        }
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, ItemStack middle) {
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.GLASS), middle, new ItemStack(Items.GLASS)));
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /**
     * The tooltip of a seed shows its genome, species and traits with both alleles, only to a
     * player who wears the goggles; otherwise it is the name alone, as before.
     */
    @GameTest(template = "empty")
    public static void seedTooltipShowsGenomeOnlyWithGoggles(GameTestHelper helper) {
        ItemStack seed = seedOf(flax(7, 4));
        FakePlayer player = new FakePlayer(helper.getLevel(), PROFILE);
        String name = seed.getHoverName().getString();

        List<String> bare = tooltip(helper, seed, player);
        if (!bare.equals(List.of(name))) {
            throw new GameTestAssertException("Expected only the name in the tooltip without goggles, got " + bare);
        }
        if (!tooltip(helper, seed, null).equals(List.of(name))) {
            throw new GameTestAssertException("Expected only the name in the tooltip with no player, got " + tooltip(helper, seed, null));
        }

        player.setItemSlot(EquipmentSlot.HEAD, goggles());
        List<String> worn = tooltip(helper, seed, player);
        if (!worn.equals(List.of(name, "species: fiber_flax | fiber_flax", "fiber_strength: 7 | 4"))
                && !worn.equals(List.of(name, "fiber_strength: 7 | 4", "species: fiber_flax | fiber_flax"))) {
            throw new GameTestAssertException("Expected the name, then the species and the strength with both alleles,"
                    + " in the tooltip with goggles, got " + worn);
        }
        List<String> plain = tooltip(helper, new ItemStack(FarmingEvolution.SEEDS.get()), player);
        if (plain.size() != 1) {
            throw new GameTestAssertException("A seed without a genome shows more than its name: " + plain);
        }
        helper.succeed();
    }

    /**
     * What the goggles show of a plant: the species with both alleles, the product and the
     * ripeness, in colours that tell the name, the alleles and the product apart; no odds.
     */
    @GameTest(template = "empty")
    public static void plantReadoutShowsSpeciesProductAndRipenessWithoutOdds(GameTestHelper helper) {
        plant(helper, flax(7, 4));
        PlantBlockEntity plant = plantAt(helper);
        String product = new ItemStack(FarmingEvolution.PRODUCTS.of(flax(7, 4))).getHoverName().getString();

        List<Component> growing = Readout.plant(plant);
        List<String> lines = strings(growing);
        if (!lines.equals(List.of("species: fiber_flax | fiber_flax", "gives " + product, "growing: stage 1 of 8"))) {
            throw new GameTestAssertException("Expected the species, the product and the stage of a young plant, got " + lines);
        }
        TextColor label = colorOf(growing.get(0), "species");
        TextColor allele = colorOf(growing.get(0), "fiber_flax");
        TextColor item = colorOf(growing.get(1), product);
        if (label == null || allele == null || item == null || label.equals(allele) || label.equals(item) || allele.equals(item)) {
            throw new GameTestAssertException("Expected the name, the allele and the product in three colours, got "
                    + label + ", " + allele + ", " + item);
        }

        helper.setBlock(PLANT, helper.getBlockState(PLANT).setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        List<String> ripe = strings(Readout.plant(plantAt(helper)));
        if (!ripe.equals(List.of("species: fiber_flax | fiber_flax", "gives " + product, "ripe"))) {
            throw new GameTestAssertException("Expected a ripe plant shown as ripe, got " + ripe);
        }
        if (ripe.stream().anyMatch(line -> line.contains("%") || line.contains("&"))) {
            throw new GameTestAssertException("The goggles show odds: " + ripe);
        }

        plantAt(helper).setGenome(null);
        List<String> none = strings(Readout.plant(plantAt(helper)));
        if (!none.equals(List.of("no genome"))) {
            throw new GameTestAssertException("Expected a plant without a genome shown as such, got " + none);
        }
        helper.succeed();
    }

    /** The climate the goggles show is that of the place: 30 °C, 10 % in the hot dry band, 12 °C, 50 % at the origin. */
    @GameTest(template = "empty")
    public static void climateReadoutNamesTemperatureAndHumidityOfThePlace(GameTestHelper helper) {
        List<String> hotDry = strings(Readout.climate(FarmingEvolution.CLIMATE.at(helper.getLevel(), new BlockPos(-1500, 64, 2000))));
        List<String> origin = strings(Readout.climate(FarmingEvolution.CLIMATE.at(helper.getLevel(), new BlockPos(0, 64, 0))));
        if (!hotDry.equals(List.of("temperature: 30.0 °C", "humidity: 10.0 %"))
                || !origin.equals(List.of("temperature: 12.0 °C", "humidity: 50.0 %"))) {
            throw new GameTestAssertException("Expected 30.0 °C, 10.0 % and 12.0 °C, 50.0 %, got " + hotDry + " and " + origin);
        }
        helper.succeed();
    }

    /**
     * The genome of a plant reaches the client: the update the server sends of the plant's
     * block entity carries it, and a block entity that reads the update has the same genome.
     */
    @GameTest(template = "empty")
    public static void updateOfPlantCarriesItsGenomeToTheClient(GameTestHelper helper) {
        plant(helper, flax(7, 4));
        PlantBlockEntity plant = plantAt(helper);
        if (!(plant.getUpdatePacket() instanceof ClientboundBlockEntityDataPacket)) {
            throw new GameTestAssertException("The plant sends no update of its block entity: " + plant.getUpdatePacket());
        }
        CompoundTag update = plant.getUpdateTag(helper.getLevel().registryAccess());
        PlantBlockEntity client = new PlantBlockEntity(plant.getBlockPos(), plant.getBlockState());
        client.handleUpdateTag(update, helper.getLevel().registryAccess());
        if (!client.genome().equals(Optional.of(flax(7, 4)))) {
            throw new GameTestAssertException("The update " + update + " gave the genome " + client.genome()
                    + ", expected " + flax(7, 4));
        }
        helper.succeed();
    }

    static List<String> strings(List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }

    /** The colour of the first piece of the line whose text is exactly this; null when there is none or it has none. */
    static TextColor colorOf(Component line, String text) {
        List<TextColor> found = new ArrayList<>();
        line.visit((style, piece) -> {
            if (piece.equals(text)) {
                found.add(style.getColor());
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static List<String> tooltip(GameTestHelper helper, ItemStack stack, FakePlayer player) {
        return strings(stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.NORMAL));
    }

    private static ItemStack goggles() {
        return new ItemStack(FarmingEvolution.BREEDER_GOGGLES.get());
    }

    private static Individual flax(int active, int inactive) {
        return new Individual(Sex.NONE, Map.of(
                Breeding.SPECIES, Chromosome.pure(new Allele.Named("fiber_flax", true)),
                "fiber_strength", new Chromosome(new Allele.Numeric(active), new Allele.Numeric(inactive))));
    }

    private static ItemStack seedOf(Individual genome) {
        ItemStack seed = new ItemStack(FarmingEvolution.SEEDS.get());
        seed.set(FarmingEvolution.GENOME, genome);
        return seed;
    }

    private static void plant(GameTestHelper helper, Individual genome) {
        helper.setBlock(PLANT.below(), Blocks.FARMLAND);
        helper.setBlock(PLANT, Blocks.AIR);
        if (!FarmingEvolution.SEEDS.get().place(new DirectionalPlaceContext(
                helper.getLevel(), helper.absolutePos(PLANT), Direction.DOWN, seedOf(genome), Direction.UP)).consumesAction()) {
            throw new GameTestAssertException("The seed could not be planted at " + PLANT);
        }
    }

    private static PlantBlockEntity plantAt(GameTestHelper helper) {
        if (!(helper.getBlockEntity(PLANT) instanceof PlantBlockEntity plant)) {
            throw new GameTestAssertException("No plant at " + PLANT);
        }
        return Objects.requireNonNull(plant);
    }
}
