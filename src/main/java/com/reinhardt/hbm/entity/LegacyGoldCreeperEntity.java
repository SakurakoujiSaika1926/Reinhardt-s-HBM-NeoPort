package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityCreeperGold. */
public final class LegacyGoldCreeperEntity extends LegacyCreeperEntity {
    public LegacyGoldCreeperEntity(EntityType<? extends LegacyGoldCreeperEntity> type, Level level) {
        super(type, level, Kind.GOLD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(20.0D, 0.25D);
    }

    /** Old getCanSpawnHere gate: Overworld and Y <= 40 only. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        return level().dimension() == Level.OVERWORLD && getY() <= 40.0D
                && super.checkSpawnRules(level, reason);
    }
}
