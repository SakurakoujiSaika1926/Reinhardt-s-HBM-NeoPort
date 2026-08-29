package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The BombMulti cluster pellet from XFactoryCatapult#cluster_submunition.
 * Motion is stored in the old BulletConfig coordinate space and multiplied by
 * its unconfigured default velocity of ten during each movement step.
 */
public final class ClusterSubmunitionEntity extends Entity {
    private static final double INITIAL_SPEED = 0.375D;
    private static final double LEGACY_CONFIG_VELOCITY = 10.0D;
    private static final double GRAVITY = 0.025D;
    private static final int LIFETIME = 1_200;

    public ClusterSubmunitionEntity(EntityType<? extends ClusterSubmunitionEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    private ClusterSubmunitionEntity(Level level, Vec3 position, float yaw, float pitch) {
        this(HbmEntityTypes.CLUSTER_SUBMUNITION.get(), level);
        setPos(position);
        setYRot(yaw * Mth.RAD_TO_DEG);
        setXRot(-pitch * Mth.RAD_TO_DEG);
        setDeltaMovement(new Vec3(
                -Mth.sin(yaw) * Mth.cos(pitch) * INITIAL_SPEED,
                Mth.sin(pitch) * INITIAL_SPEED,
                Mth.cos(yaw) * Mth.cos(pitch) * INITIAL_SPEED
        ));
    }

    public static void spawn(Level level, Vec3 position, int count) {
        for (int index = 0; index < count; index++) {
            float yaw = (float) (level.random.nextGaussian() * Math.PI * 2.0D);
            float pitch = (float) (Math.PI * 0.5D + level.random.nextGaussian() * Math.PI * 0.125D);
            level.addFreshEntity(new ClusterSubmunitionEntity(level, position, yaw, pitch));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (this.tickCount > LIFETIME) {
            discard();
            return;
        }

        Vec3 start = position();
        Vec3 storedMotion = getDeltaMovement();
        Vec3 displacement = storedMotion.scale(LEGACY_CONFIG_VELOCITY);
        Vec3 end = start.add(displacement);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 collisionEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, collisionEnd);
        HitResult hit = entityHit != null ? entityHit : blockHit;
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            LegacyProjectileUtil.fixedDamageExplosion(level(), this, hit.getLocation(), 7.5F, 50.0F, true);
            discard();
            return;
        }

        setPos(end);
        setDeltaMovement(storedMotion.x, storedMotion.y - GRAVITY, storedMotion.z);
        updateRotation(displacement);
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity closest = null;
        Vec3 closestPosition = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this, area,
                entity -> entity.isAlive() && entity.isPickable())) {
            var impact = candidate.getBoundingBox().inflate(0.3D).clip(start, end);
            if (impact.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(impact.get());
            if (distance < closestDistance) {
                closest = candidate;
                closestPosition = impact.get();
                closestDistance = distance;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestPosition);
    }

    private void updateRotation(Vec3 movement) {
        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (horizontal < 1.0E-7D && Math.abs(movement.y) < 1.0E-7D) {
            return;
        }
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(movement.x, movement.z))));
        setXRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(movement.y, horizontal)) - 90.0D));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65_536.0D;
    }
}
