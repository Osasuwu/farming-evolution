package io.github.osasuwu.farmingevolution.gametest;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Proves the GameTest stage of the check pipeline runs inside a real server world.
 * Real tests of the mod replace it as mechanics appear.
 */
@GameTestHolder(FarmingEvolution.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SkeletonGameTests {
    private SkeletonGameTests() {
    }

    @GameTest(template = "empty")
    public static void placedBlockIsReadBack(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.FARMLAND);
        helper.assertBlockPresent(Blocks.FARMLAND, pos);
        helper.succeed();
    }
}
