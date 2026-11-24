package io.github.osasuwu.farmingevolution.plant;

import com.mojang.serialization.MapCodec;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A wild berry bush, the food of the slice besides the seed (CONCEPT.md, "Вертикальный
 * срез"). A ripe bush gives berries to an empty hand and ripens again. It only grows where
 * world generation put it: there is no item to plant it with.
 */
public final class WildBerryBushBlock extends BushBlock {
    public static final MapCodec<WildBerryBushBlock> CODEC = simpleCodec(WildBerryBushBlock::new);

    /** 1 when the bush has berries. */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;

    /** Berries one picking gives. */
    private static final int BERRIES = 2;

    /** One random tick in this many ripens an empty bush. */
    private static final int RIPEN_CHANCE = 8;

    public WildBerryBushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 1));
    }

    @Override
    protected MapCodec<WildBerryBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) == 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(RIPEN_CHANCE) == 0) {
            level.setBlock(pos, state.setValue(AGE, 1), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) == 0) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(FarmingEvolution.WILD_BERRIES.get(), BERRIES));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
