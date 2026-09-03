package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityCreeperPhosgene. */
public final class LegacyPhosgeneCreeperEntity extends LegacyCreeperEntity {
    public LegacyPhosgeneCreeperEntity(EntityType<? extends LegacyPhosgeneCreeperEntity> type, Level level) {
        super(type, level, Kind.PHOSGENE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(20.0D, 0.25D);
    }
}
