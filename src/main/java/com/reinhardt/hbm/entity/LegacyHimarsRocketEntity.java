package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmChunkTickets;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class LegacyHimarsRocketEntity extends Entity {
    private static final EntityDataAccessor<Integer> ROCKET_TYPE =
            SynchedEntityData.defineId(LegacyHimarsRocketEntity.class, EntityDataSerializers.INT);
    private static final double SPEED = 25.0D;
    private UUID targetUuid;
    private double targetX;
    private double targetY;
    private double targetZ;
    private final double[][] targetMotion = new double[20][3];
    private boolean steeringActive = true;
    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;
    private double syncPosX;
    private double syncPosY;
    private double syncPosZ;
    private double syncYaw;
    private double syncPitch;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private int turnProgress;

    public LegacyHimarsRocketEntity(EntityType<? extends LegacyHimarsRocketEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyHimarsRocketEntity(Level level, Vec3 position, Vec3 direction, int rocketType, Vec3 target) {
        this(HbmEntityTypes.LEGACY_HIMARS_ROCKET.get(), level);
        setPos(position.x, position.y, position.z);
        setRocketType(rocketType);
        setTarget(target.x, target.y, target.z);
        Vec3 motion = direction.normalize().scale(SPEED);
        setDeltaMovement(motion);
        updateRotationFromMotion(motion);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROCKET_TYPE, 0);
    }

    public LegacyProjectileUtil.HimarsType rocketType() {
        return LegacyProjectileUtil.HimarsType.byModelData(this.entityData.get(ROCKET_TYPE));
    }

    public void setRocketType(int rocketType) {
        this.entityData.set(ROCKET_TYPE, rocketType);
    }

    public void setTarget(Entity target) {
        if (target == null) {
            this.targetUuid = null;
            return;
        }
        this.targetUuid = target.getUUID();
        setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();

        if (level().isClientSide) {
            tickClientInterpolation();
            return;
        }
        updateForcedChunk();

        Vec3 motion = getDeltaMovement();
        updateRotationFromMotion(motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);
        HitResult hit = entityHit != null ? entityHit : blockHit;
        if (hit.getType() != HitResult.Type.MISS) {
            impact(hit);
            return;
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateCourseAfterMovement();
    }

    private void tickClientInterpolation() {
        if (this.turnProgress > 0) {
            double x = getX() + (this.syncPosX - getX()) / this.turnProgress;
            double y = getY() + (this.syncPosY - getY()) / this.turnProgress;
            double z = getZ() + (this.syncPosZ - getZ()) / this.turnProgress;
            double yawDelta = Mth.wrapDegrees(this.syncYaw - getYRot());
            setYRot((float) (getYRot() + yawDelta / this.turnProgress));
            setXRot((float) (getXRot() + (this.syncPitch - getXRot()) / this.turnProgress));
            this.turnProgress--;
            setPos(x, y, z);
        } else {
            setPos(getX(), getY(), getZ());
        }
        spawnKeroseneTrail(position().subtract(new Vec3(this.xo, this.yo, this.zo)));
    }

    private void updateCourseAfterMovement() {
        Entity target = resolveTargetEntity();
        Vec3 motion = getDeltaMovement();
        if (isInWater()) {
            motion = motion.scale(0.8D);
        }
        boolean steeringAtTickStart = this.steeringActive;
        if (!steeringAtTickStart) {
            motion = motion.add(0.0D, -0.01D, 0.0D);
        }
        Vec3 delta = new Vec3(this.targetX - getX(), this.targetY - getY(), this.targetZ - getZ());
        double momentum = motion.length();
        if (delta.length() <= momentum * 1.5D) {
            if (target == null || !target.isAlive()) {
                this.steeringActive = false;
            }
            if (delta.lengthSqr() > 1.0E-12D) {
                motion = delta.normalize().scale(momentum);
            }
        } else {
            if (target != null) {
                recalculatePredictiveTarget(target);
            }
            if (this.steeringActive) {
                motion = ballisticArcCourse(motion);
            }
        }
        setDeltaMovement(motion);
    }

    private Entity resolveTargetEntity() {
        if (this.targetUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(this.targetUuid);
    }

    private void recalculatePredictiveTarget(Entity target) {
        Vec3 speed = getDeltaMovement();
        Vec3 delta = new Vec3(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ());
        double eta = delta.length() - speed.length();
        double motionX = target.getDeltaMovement().x;
        double motionY = target.getDeltaMovement().y;
        double motionZ = target.getDeltaMovement().z;
        for (int i = 1; i < this.targetMotion.length; i++) {
            this.targetMotion[i - 1] = this.targetMotion[i];
            motionX += this.targetMotion[i][0];
            motionY += this.targetMotion[i][1];
            motionZ += this.targetMotion[i][2];
        }
        Vec3 targetDelta = target.getDeltaMovement();
        this.targetMotion[this.targetMotion.length - 1] = new double[] { targetDelta.x, targetDelta.y, targetDelta.z };
        if (eta <= 1.0D) {
            setTarget(target);
            return;
        }
        setTarget(
                target.getX() + (motionX / 20.0D) * eta,
                target.getY() + target.getBbHeight() * 0.5D + (motionY / 20.0D) * eta,
                target.getZ() + (motionZ / 20.0D) * eta
        );
    }

    private void spawnKeroseneTrail(Vec3 displacement) {
        Vec3 trail = displacement.scale(-1.0D);
        double velocity = trail.length();
        if (velocity <= 1.0D) {
            return;
        }
        Vec3 direction = trail.normalize();
        int offset = 6;
        for (int i = offset; i < velocity + offset; i++) {
            level().addParticle(
                    HbmParticleTypes.KEROSENE_ROCKET_FLAME.get(),
                    getX() + direction.x * i,
                    getY() + direction.y * i,
                    getZ() + direction.z * i,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private Vec3 ballisticArcCourse(Vec3 motion) {
        Vec3 direction = motion.normalize();
        double horizontalMomentum = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        Vec3 target = new Vec3(this.targetX - getX(), this.targetY - getY(), this.targetZ - getZ());
        double horizontalDelta = Math.sqrt(target.x * target.x + target.z * target.z);
        double stepsRequired = horizontalMomentum <= 1.0E-7D ? 1.0D : horizontalDelta / horizontalMomentum;
        Vec3 targetDirection = target.normalize();
        double rocketYaw = legacyYaw(direction);
        double rocketPitch = legacyPitch(direction);
        double targetYaw = legacyYaw(targetDirection);
        double targetPitch = legacyPitch(targetDirection);
        double turnSpeed = Math.min(15.0D, 45.0D / stepsRequired);
        if (stepsRequired <= 1.0D) {
            turnSpeed = 180.0D;
        }
        double deltaYaw = ((targetYaw - rocketYaw) + 180.0D) % 360.0D - 180.0D;
        double deltaPitch = ((targetPitch - rocketPitch) + 180.0D) % 360.0D - 180.0D;
        double turnYaw = Math.min(Math.abs(deltaYaw), turnSpeed) * Math.signum(deltaYaw);
        double turnPitch = Math.min(Math.abs(deltaPitch), turnSpeed) * Math.signum(deltaPitch);
        Vec3 velocity = new Vec3(SPEED, 0.0D, 0.0D);
        velocity = rotateZ(velocity, -Math.toRadians(rocketPitch + turnPitch));
        velocity = rotateY(velocity, Math.toRadians(rocketYaw + turnYaw + 90.0D));
        return velocity;
    }

    private static double legacyYaw(Vec3 vec) {
        boolean positiveZ = vec.z >= 0.0D;
        return Math.toDegrees(Math.atan(vec.x / vec.z)) + (positiveZ ? 180.0D : 0.0D);
    }

    private static double legacyPitch(Vec3 vec) {
        return Math.toDegrees(Math.atan(vec.y / Math.sqrt(vec.x * vec.x + vec.z * vec.z)));
    }

    private static Vec3 rotateZ(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vec.x * cos - vec.y * sin, vec.x * sin + vec.y * cos, vec.z);
    }

    private static Vec3 rotateY(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity closest = null;
        Vec3 closestHit = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable())) {
            Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (optionalHit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(optionalHit.get());
            if (distance < closestDistance) {
                closest = entity;
                closestHit = optionalHit.get();
                closestDistance = distance;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestHit);
    }

    private void impact(HitResult hit) {
        if (level().isClientSide) {
            discard();
            return;
        }
        Vec3 effectPos = Vec3.atCenterOf(BlockPos.containing(hit.getLocation()));
        Vec3 pos = hit.getLocation();
        Vec3 backstep = getDeltaMovement().normalize();
        if (Double.isFinite(backstep.x) && Double.isFinite(backstep.y) && Double.isFinite(backstep.z)) {
            pos = pos.subtract(backstep);
        }
        switch (rocketType()) {
            case STANDARD -> {
                LegacyProjectileUtil.standardExplosion(this, pos, 20.0F, 3.0F, false, true, false);
                LegacyProjectileUtil.composeExplosionEffect(level(), effectPos,
                        15, 5.0F, 1.0F, 45.0F, 10, 0, 50, 1.0F, 3.0F, -2.0F, 200.0F);
            }
            case STANDARD_HE -> {
                LegacyProjectileUtil.standardExplosion(this, pos, 20.0F, 3.0F, true, true, false);
                LegacyProjectileUtil.composeExplosionEffect(level(), effectPos,
                        15, 5.0F, 1.0F, 45.0F, 10, 16, 50, 1.0F, 3.0F, -2.0F, 200.0F);
            }
            case STANDARD_LAVA -> {
                LegacyProjectileUtil.volcanicExplosion(this, pos, 20.0F, 3.0F);
            }
            case SINGLE -> {
                LegacyProjectileUtil.standardExplosion(this, pos, 50.0F, 5.0F, true, true, false);
                LegacyProjectileUtil.composeExplosionEffect(level(), effectPos,
                        30, 6.5F, 2.0F, 65.0F, 25, 16, 50, 1.25F, 3.0F, -2.0F, 350.0F);
            }
            case STANDARD_MINI_NUKE -> LegacyProjectileUtil.promptNuke(this, pos, false);
            case STANDARD_WP -> LegacyProjectileUtil.phosphorus(
                    this, pos, hit.getLocation(), 30, 20, 20.0F, 30, 10, 15.0D, 15.0F);
            case STANDARD_TB -> {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_EXPLOSION_MEDIUM.get(),
                        SoundSource.HOSTILE, 20.0F, 0.9F + level().random.nextFloat() * 0.2F);
                LegacyProjectileUtil.standardExplosion(this, pos, 20.0F, 10.0F, true, true, false);
                LegacyProjectileUtil.spawnShrapnel(level(), pos, 30);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(HbmParticleTypes.RBMK_MUSH.get(), pos.x, pos.y, pos.z, 0, 20.0D, 0.0D, 0.0D, 1.0D);
                }
            }
            case SINGLE_TB -> {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_EXPLOSION_MEDIUM.get(),
                        SoundSource.HOSTILE, 20.0F, 0.9F + level().random.nextFloat() * 0.2F);
                LegacyProjectileUtil.standardExplosion(this, pos, 50.0F, 12.0F, true, true, false);
                LegacyProjectileUtil.spawnShrapnel(level(), pos, 30);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(HbmParticleTypes.RBMK_MUSH.get(), pos.x, pos.y, pos.z, 0, 35.0D, 0.0D, 0.0D, 1.0D);
                }
            }
        }
        discard();
    }

    private void updateRotationFromMotion(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yRot;
        this.syncPitch = xRot;
        this.turnProgress = steps;
        setDeltaMovement(this.velocityX, this.velocityY, this.velocityZ);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
        setDeltaMovement(x, y, z);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("rocket_type", this.entityData.get(ROCKET_TYPE));
        tag.putDouble("target_x", this.targetX);
        tag.putDouble("target_y", this.targetY);
        tag.putDouble("target_z", this.targetZ);
        tag.putBoolean("steering", this.steeringActive);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(ROCKET_TYPE, tag.getInt("rocket_type"));
        this.targetX = tag.getDouble("target_x");
        this.targetY = tag.getDouble("target_y");
        this.targetZ = tag.getDouble("target_z");
        this.steeringActive = tag.getBoolean("steering");
        this.targetUuid = null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunk();
        super.remove(reason);
    }

    private void updateForcedChunk() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int chunkX = Mth.floor(getX()) >> 4;
        int chunkZ = Mth.floor(getZ()) >> 4;
        if (chunkX == this.forcedChunkX && chunkZ == this.forcedChunkZ) {
            return;
        }
        releaseForcedChunk(serverLevel);
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(serverLevel, this, chunkX, chunkZ, true, true);
        this.forcedChunkX = chunkX;
        this.forcedChunkZ = chunkZ;
    }

    private void releaseForcedChunk() {
        if (level() instanceof ServerLevel serverLevel) {
            releaseForcedChunk(serverLevel);
        }
    }

    private void releaseForcedChunk(ServerLevel serverLevel) {
        if (this.forcedChunkX == Integer.MIN_VALUE) {
            return;
        }
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(
                serverLevel, this, this.forcedChunkX, this.forcedChunkZ, false, true);
        this.forcedChunkX = Integer.MIN_VALUE;
        this.forcedChunkZ = Integer.MIN_VALUE;
    }
}
