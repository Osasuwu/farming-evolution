package io.github.osasuwu.farmingevolution.screenshot;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.WildFlaxBlock;
import io.github.osasuwu.farmingevolution.store.SeedStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The blocks of the vertical slice in front of the player, for the picture of
 * {@link ScreenshotRun}: a row of flax crops at every growth stage on farmland, wild flax on
 * the grass behind it, a seed store beside the row, and the analyzer in the player's hand.
 */
final class SliceScene {
    /** The crop row runs across the view, this many blocks in front of the player. */
    private static final int CROP_ROW = 5;
    private static final int WILD_ROW = 7;
    /** Crops from age 1 to 7, one per block, left to right. */
    private static final int HALF_WIDTH = 3;

    private SliceScene() {
    }

    /** Runs on the server thread. */
    static void build(MinecraftServer server, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        GameRules rules = level.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);

        BlockPos feet = player.blockPosition();
        // The top block of the column: grass on a flat world.
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, feet.getX(), feet.getZ()) - 1;
        // The player faces south (+Z), so +X is on the left of the picture.
        int x0 = feet.getX();
        int z0 = feet.getZ();

        Individual wildFlax = FarmingEvolution.BREEDING.wild(WildFlaxBlock.SPECIES, Sex.NONE);
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            int age = HALF_WIDTH - dx + 1;
            BlockPos soil = new BlockPos(x0 + dx, ground, z0 + CROP_ROW);
            level.setBlock(soil, farmland, 3);
            BlockPos crop = soil.above();
            level.setBlock(crop, FarmingEvolution.CROP.get().getStateForAge(age), 3);
            if (level.getBlockEntity(crop) instanceof PlantBlockEntity plant) {
                plant.setGenome(wildFlax);
            }
        }
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx += 2) {
            level.setBlock(new BlockPos(x0 + dx, ground + 1, z0 + WILD_ROW), FarmingEvolution.WILD_FLAX.get().defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(x0 - HALF_WIDTH - 1, ground + 1, z0 + CROP_ROW - 1), SeedStore.BLOCK.get().defaultBlockState(), 3);

        ItemStack seeds = new ItemStack(FarmingEvolution.SEEDS.get(), 16);
        seeds.set(FarmingEvolution.GENOME, wildFlax);
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(0, new ItemStack(FarmingEvolution.ANALYZER.get()));
        inventory.setItem(1, seeds);
        inventory.setItem(2, new ItemStack(SeedStore.ITEM.get()));
        inventory.selected = 0;

        // Standing in the middle of the block, looking south and down at the rows.
        player.connection.teleport(x0 + 0.5, ground + 1, z0 + 0.5, 0.0F, 25.0F);
    }
}
