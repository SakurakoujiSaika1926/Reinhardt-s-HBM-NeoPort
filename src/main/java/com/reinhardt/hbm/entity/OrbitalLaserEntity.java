package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Five-tick full-bright beam spawned by the precision orbital laser. */
public final class OrbitalLaserEntity extends Entity {
    public static final int MAX_AGE = 5;

    public OrbitalLaserEntity(EntityType<? extends OrbitalLaserEntity> type, Level level) {
        super(type, level);
    }

    public OrbitalLaserEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.ORBITAL_LASER.get(), level);
        setPos(x, y, z);
    }

    /** The old satellite invokes the terrain/entity explosion before spawning the visual entity. */
    public void explode() {
        if (level() instanceof ServerLevel serverLevel) {
            LegacyProjectileUtil.precisionOrbitalLaserExplosion(serverLevel, position());
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= MAX_AGE) discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
