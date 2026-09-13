package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The 1.7.10 registry exposed each Glyphid class as a separate entity.  The
 * port keeps the shared implementation in GlyphidEntity, but uses this
 * concrete type for the old per-variant registry entries and spawn eggs.
 */
public final class LegacyGlyphidVariantEntity extends GlyphidEntity {
    private final Variant fixedVariant;

    public LegacyGlyphidVariantEntity(EntityType<? extends LegacyGlyphidVariantEntity> type,
                                       Level level, Variant variant) {
        super(type, level);
        fixedVariant = variant;
        setVariant(variant);
    }

    @Override
    public void setVariant(Variant variant) {
        super.setVariant(fixedVariant == null ? variant : fixedVariant);
    }

    public Variant fixedVariant() {
        return fixedVariant;
    }

    @Override
    public boolean isAtDestination() {
        return fixedVariant != Variant.SCOUT
                ? super.isAtDestination()
                : getCurrentTask() == TASK_BUILD_HIVE && super.isAtDestination();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setVariant(fixedVariant);
        setHealth(getMaxHealth());
        return result;
    }
}
