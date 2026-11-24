package io.github.osasuwu.farmingevolution.vanilla;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Empties the list of hostile mobs of every biome, so natural spawning has nothing to try.
 * Applied by data/farmingevolution/neoforge/biome_modifier/remove_hostile_spawns.json.
 */
public final class RemoveHostileSpawns implements BiomeModifier {
    public static final RemoveHostileSpawns INSTANCE = new RemoveHostileSpawns();
    public static final MapCodec<RemoveHostileSpawns> CODEC = MapCodec.unit(INSTANCE);

    private RemoveHostileSpawns() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.REMOVE) {
            builder.getMobSpawnSettings().getSpawner(MobCategory.MONSTER).clear();
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
