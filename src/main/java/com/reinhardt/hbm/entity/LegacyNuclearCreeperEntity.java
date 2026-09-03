package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityCreeperNuclear. */
public final class LegacyNuclearCreeperEntity extends LegacyCreeperEntity {
    public LegacyNuclearCreeperEntity(EntityType<? extends LegacyNuclearCreeperEntity> type, Level level) {
        super(type, level, Kind.NUCLEAR);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(50.0D, 0.3D);
    }
}
