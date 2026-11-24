package io.github.osasuwu.farmingevolution.plant;

import com.mojang.serialization.MapCodec;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Wild flax, the wild source of the flax line (CONCEPT.md, "Вертикальный срез"). World
 * generation scatters it over the overworld, and it slowly spreads to the grass next to it,
 * so a player who lost every plant and seed finds it again. Breaking it gives seeds of the
 * wild type of {@link #SPECIES}.
 */
public final class WildFlaxBlock extends BushBlock {
    public static final MapCodec<WildFlaxBlock> CODEC = simpleCodec(WildFlaxBlock::new);

    /** The species of the seeds: the flax of pack data that has a variety to select towards. */
    public static final String SPECIES = "fiber_flax";

    /** Seeds a broken plant gives. */
    private static final int SEEDS = 2;

    /** One random tick in this many spreads the plant. */
    private static final int SPREAD_CHANCE = 25;

    /** No spreading where this many wild flax plants already grow within 4 blocks. */
    private static final int CROWD = 5;

    public WildFlaxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WildFlaxBlock> codec() {
        return CODEC;
    }

    /** Every way of breaking gives the same: the drops are set here, not by a loot table. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Individual wild = FarmingEvolution.BREEDING.wild(SPECIES, Sex.NONE);
        ItemStack seeds = new ItemStack(FarmingEvolution.SEEDS.get(), SEEDS);
        seeds.set(FarmingEvolution.GENOME, wild);
        return List.of(seeds);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * Spreads to one random free spot on grass next to it, unless the spot is crowded. Only
     * grass: tilled farmland and other dirt of the player's field stay free of it.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPREAD_CHANCE) != 0) {
            return;
        }
        int nearby = 0;
        for (BlockPos other : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(other).is(this) && ++nearby >= CROWD) {
                return;
            }
        }
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
        if (level.isEmptyBlock(target) && level.getBlockState(target.below()).is(Blocks.GRASS_BLOCK)
                && state.canSurvive(level, target)) {
            level.setBlock(target, state, 2);
        }
    }
}
