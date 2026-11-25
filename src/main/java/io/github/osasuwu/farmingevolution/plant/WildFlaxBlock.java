package io.github.osasuwu.farmingevolution.plant;

import com.mojang.serialization.MapCodec;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Sex;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Wild flax, the wild source of the flax line (CONCEPT.md, "Вертикальный срез"). World
 * generation scatters it over the overworld. It does not spread: a player who lost every
 * plant and seed finds it again in new chunks (#104, #122). Breaking it gives seeds of the
 * wild type of {@link #SPECIES}.
 */
public final class WildFlaxBlock extends BushBlock {
    public static final MapCodec<WildFlaxBlock> CODEC = simpleCodec(WildFlaxBlock::new);

    /** The species of the seeds: the flax of pack data that has a variety to select towards. */
    public static final String SPECIES = "fiber_flax";

    /** Seeds a broken plant gives. */
    private static final int SEEDS = 2;

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
        return List.of(wildSeeds(SEEDS));
    }

    /**
     * Seeds of the wild type of {@link #SPECIES}: what a broken plant gives, and what the
     * creative tab of the mod holds.
     */
    public static ItemStack wildSeeds(int count) {
        Individual wild = FarmingEvolution.BREEDING.wild(SPECIES, Sex.NONE);
        ItemStack seeds = new ItemStack(FarmingEvolution.SEEDS.get(), count);
        seeds.set(FarmingEvolution.GENOME, wild);
        return seeds;
    }
}
