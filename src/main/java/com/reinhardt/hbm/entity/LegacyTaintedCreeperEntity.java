package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityCreeperTainted. */
public final class LegacyTaintedCreeperEntity extends LegacyCreeperEntity {
    public LegacyTaintedCreeperEntity(EntityType<? extends LegacyTaintedCreeperEntity> type, Level level) {
        super(type, level, Kind.TAINTED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(15.0D, 0.35D);
    }
}
