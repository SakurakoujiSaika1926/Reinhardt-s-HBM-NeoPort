package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.explosion.LegacyMukeExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NPC bullet configuration port for worm bolts, UFO lasers and UFO rockets.
 * The values are the corresponding 1.7.10 GunNPCFactory values, rather than
 * reusing a player-ammo profile.
 */
public final class LegacyBossProjectileEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(LegacyBossProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(LegacyBossProjectileEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUuid;
    private UUID targetUuid;

    public LegacyBossProjectileEntity(EntityType<? extends LegacyBossProjectileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyBossProjectileEntity(Level level, Entity owner, Vec3 origin, Vec3 direction, Type type, Entity target) {
        this(HbmEntityTypes.LEGACY_BOSS_PROJECTILE.get(), level);
        setPos(origin.x, origin.y, origin.z);
        this.ownerUuid = owner.getUUID();
        this.targetUuid = target == null ? null : target.getUUID();
        entityData.set(TYPE, type.ordinal());
        entityData.set(DAMAGE, type.damage(level.random));
        Vec3 motion = direction.normalize().scale(type.speed());
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, Type.WORM_BOLT.ordinal());
        builder.define(DAMAGE, 15.0F);
    }

    public Type projectileType() {
        int id = entityData.get(TYPE);
        return id >= 0 && id < Type.values().length ? Type.values()[id] : Type.WORM_BOLT;
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        Vec3 motion = getDeltaMovement();
        if (projectileType() == Type.UFO_ROCKET) {
            Entity target = target();
            if (target != null && target.isAlive()) {
                motion = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                        .subtract(position()).normalize().scale(Math.max(2.0D, motion.length()));
                setDeltaMovement(motion);
            }
        }
        updateRotation(motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);
        if (entityHit != null) {
            impact(entityHit.getLocation(), entityHit.getEntity());
            return;
        }
        if (blockHit instanceof BlockHitResult block && blockHit.getType() != HitResult.Type.MISS) {
            impact(block.getLocation(), null);
            return;
        }
        setPos(end);
        if (!level().isClientSide && (tickCount > projectileType().maxAge() || getY() < level().getMinBuildHeight() - 16)) {
            discard();
        }
    }

    private void impact(Vec3 hit, Entity directHit) {
        if (level().isClientSide) {
            discard();
            return;
        }
        if (projectileType() == Type.UFO_ROCKET) {
            level().playSound(null, hit.x, hit.y, hit.z, HbmSoundEvents.ENTITY_UFO_BLAST.get(),
                    SoundSource.HOSTILE, 5.0F, 0.9F + level().random.nextFloat() * 0.2F);
            level().playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.HOSTILE, 5.0F, 0.5F);
            LegacyMukeExplosion.detonateUfoRocket((ServerLevel) level(), this, hit);
            if (level() instanceof ServerLevel serverLevel) {
                for (int index = 0; index < 3; index++) {
                    serverLevel.sendParticles(com.reinhardt.hbm.registry.HbmParticleTypes.LEGACY_PLASMA_BLAST.get(),
                            hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        } else if (directHit != null) {
            directHit.hurt(damageSources().indirectMagic(this, owner()), entityData.get(DAMAGE));
        }
        discard();
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        List<EntityHitResult> hits = level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable()
                        && entity != owner())
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.3D).clip(start, end)
                        .map(point -> new EntityHitResult(entity, point)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())))
                .toList();
        return hits.isEmpty() ? null : hits.getFirst();
    }

    private Entity owner() {
        return ownerUuid == null || !(level() instanceof ServerLevel serverLevel) ? null : serverLevel.getEntity(ownerUuid);
    }

    private Entity target() {
        return targetUuid == null || !(level() instanceof ServerLevel serverLevel) ? null : serverLevel.getEntity(targetUuid);
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("type", entityData.get(TYPE));
        tag.putFloat("damage", entityData.get(DAMAGE));
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
        if (targetUuid != null) tag.putUUID("target", targetUuid);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("type"));
        entityData.set(DAMAGE, tag.getFloat("damage"));
        ownerUuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        targetUuid = tag.hasUUID("target") ? tag.getUUID("target") : null;
    }

    public enum Type {
        WORM_BOLT(0.5D, 60, 15.0F, 25.0F),
        WORM_LASER(1.0D, 100, 35.0F, 60.0F),
        UFO_ROCKET(2.0D, 100, 20.0F, 20.0F),
        CHOPPER_BULLET(3.0D, 30, 3.0F, 7.0F);

        private final double speed;
        private final int maxAge;
        private final float minDamage;
        private final float maxDamage;

        Type(double speed, int maxAge, float minDamage, float maxDamage) {
            this.speed = speed;
            this.maxAge = maxAge;
            this.minDamage = minDamage;
            this.maxDamage = maxDamage;
        }

        private double speed() { return speed; }
        private int maxAge() { return maxAge; }
        private float damage(net.minecraft.util.RandomSource random) {
            return minDamage + random.nextFloat() * (maxDamage - minDamage + 1.0F);
        }
    }
}
