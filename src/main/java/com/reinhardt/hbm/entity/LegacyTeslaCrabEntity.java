package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityTeslaCrab. */
public final class LegacyTeslaCrabEntity extends LegacyCyberCrabEntity {
    public LegacyTeslaCrabEntity(EntityType<? extends LegacyTeslaCrabEntity> type, Level level) {
        super(type, level, Kind.TESLA);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(10.0D, 0.5D);
    }

    @Override
    public void tick() {
        updateTeslaTargets(1.0D, 3.0D);
        super.tick();
    }
}
