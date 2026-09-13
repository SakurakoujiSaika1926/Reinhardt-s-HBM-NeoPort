package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Exact EntityMappings.addSpawn coverage for the three legacy creepers.
 *
 * <p>HBM 1.7.10 iterated {@code BiomeGenBase.getBiomeGenArray()} and skipped
 * only {@code BiomeGenMushroomIsland}.  The entities themselves then rejected
 * non-Overworld natural spawns.  This modifier deliberately retains that
 * split instead of substituting a biome tag with narrower coverage.</p>
 */
public final class LegacyAllBiomeCreeperSpawnsModifier implements BiomeModifier {
    public static final MapCodec<LegacyAllBiomeCreeperSpawnsModifier> CODEC =
            MapCodec.unit(LegacyAllBiomeCreeperSpawnsModifier::new);

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || biome.is(Biomes.MUSHROOM_FIELDS)) {
            return;
        }

        builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(HbmEntityTypes.PHOSGENE_CREEPER.get(), 5, 1, 1));
        builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(HbmEntityTypes.VOLATILE_CREEPER.get(), 10, 1, 1));
        builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(HbmEntityTypes.GOLD_CREEPER.get(), 1, 1, 1));
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
