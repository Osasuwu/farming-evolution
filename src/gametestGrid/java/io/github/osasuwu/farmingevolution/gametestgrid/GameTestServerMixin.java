package io.github.osasuwu.farmingevolution.gametestgrid;

import io.github.osasuwu.farmingevolution.gametest.GameTestGridGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Puts the grid of all GameTests at {@link GameTestGridGameTests#GRID_ORIGIN} instead of the
 * random point the game picks (issue #82). The game picks it in GameTestServer.startTests and
 * keeps it in the only BlockPos local of that method: the grid, the line "tests are now running
 * at position" in the log and nothing else read it. GameTestServer is the server of the
 * GameTest run alone, and this mixin is in a dev-only jar only that run gets (build.gradle).
 */
@Mixin(GameTestServer.class)
abstract class GameTestServerMixin {
    @ModifyVariable(method = "startTests", at = @At("STORE"))
    private BlockPos farmingevolution$pinGrid(BlockPos random) {
        return GameTestGridGameTests.GRID_ORIGIN;
    }
}
