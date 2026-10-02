package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ten-tick, scale-aware ParticleAmatFlash carrier. */
public final class LegacyAntimatterFlashEntity extends Entity {
    public static final int MAX_AGE = 10;
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(LegacyAntimatterFlashEntity.class, EntityDataSerializers.FLOAT);

    public LegacyAntimatterFlashEntity(EntityType<? extends LegacyAntimatterFlashEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(Level level, Vec3 position, float scale) {
        if (level.isClientSide) {
            return;
        }
        LegacyAntimatterFlashEntity flash = new LegacyAntimatterFlashEntity(
                HbmEntityTypes.ANTIMATTER_FLASH.get(), level);
        flash.setPos(position);
        flash.entityData.set(SCALE, scale);
        level.addFreshEntity(flash);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0F);
    }

    public float scale() {
        return entityData.get(SCALE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= MAX_AGE) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("scale", scale());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SCALE, tag.getFloat("scale"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = Math.max(64.0D, scale() * 16.0D);
        return distance < range * range;
    }
}
