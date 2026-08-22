package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LegacyShrapnelEntity extends Entity {
    private static final EntityDataAccessor<Boolean> TRAIL =
            SynchedEntityData.defineId(LegacyShrapnelEntity.class, EntityDataSerializers.BOOLEAN);

    public LegacyShrapnelEntity(EntityType<? extends LegacyShrapnelEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyShrapnelEntity(Level level, double x, double y, double z, Vec3 motion, boolean trail) {
        this(HbmEntityTypes.LEGACY_SHRAPNEL.get(), level);
        setPos(x, y, z);
        setDeltaMovement(motion);
        this.entityData.set(TRAIL, trail);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIL, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);
        HitResult hit = entityHit != null ? entityHit : blockHit;

        if (!level().isClientSide && entityHit != null) {
            LegacyProjectileUtil.hurtNoIFrame(
                    entityHit.getEntity(),
                    damageSources().source(HbmDamageTypes.SHRAPNEL, this, null),
                    15.0F
            );
        }
        if (!level().isClientSide && hit.getType() != HitResult.Type.MISS && this.tickCount > 5) {
            ServerLevel serverLevel = (ServerLevel) level();
            serverLevel.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 5, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            discard();
            return;
        }

        move(MoverType.SELF, motion);
        setDeltaMovement(getDeltaMovement().scale(0.99D).add(0.0D, -0.03D, 0.0D));
        if (this.entityData.get(TRAIL) && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (!level().isClientSide && (this.tickCount > 200 || getY() < level().getMinBuildHeight() - 16)) {
            discard();
        }
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(0.3D);
        Entity closest = null;
        Vec3 closestHit = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable())) {
            var hit = entity.getBoundingBox().inflate(0.2D).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closest = entity;
                closestHit = hit.get();
                closestDistance = distance;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestHit);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("trail", this.entityData.get(TRAIL));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(TRAIL, tag.getBoolean("trail"));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }
}
