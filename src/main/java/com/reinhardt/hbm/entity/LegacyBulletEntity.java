package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.block.LegacyBarrelBlock;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public class LegacyBulletEntity extends Entity {
    private static final EntityDataAccessor<Integer> AMMO_TYPE =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BASE_DAMAGE =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BALEFIRE =
            SynchedEntityData.defineId(LegacyBulletEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID targetUuid;
    private int ricochets;

    public LegacyBulletEntity(EntityType<? extends LegacyBulletEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyBulletEntity(Level level, double x, double y, double z, Vec3 direction, StandardAmmoItem.StandardAmmoType ammo, float baseDamage) {
        this(HbmEntityTypes.LEGACY_BULLET.get(), level);
        setPos(x, y, z);
        setAmmoType(ammo);
        setBaseDamage(baseDamage * ammo.damageMultiplier());
        Vec3 normalized = direction.normalize();
        double speed = switch (ammo.family()) {
            case ROCKET_ML -> 0.0D;
            case FLAME -> 1.0D;
            default -> 10.0D;
        };
        Vec3 motion = normalized.scale(speed);
        setDeltaMovement(motion);
        updateRotationFromMotion(normalized);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AMMO_TYPE, StandardAmmoItem.StandardAmmoType.NONE.ordinal());
        builder.define(BASE_DAMAGE, 0.0F);
        builder.define(BALEFIRE, false);
    }

    public StandardAmmoItem.StandardAmmoType ammoType() {
        StandardAmmoItem.StandardAmmoType[] values = StandardAmmoItem.StandardAmmoType.values();
        int ordinal = this.entityData.get(AMMO_TYPE);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : StandardAmmoItem.StandardAmmoType.NONE;
    }

    public boolean balefire() {
        return this.entityData.get(BALEFIRE);
    }

    public void setBalefire(boolean balefire) {
        this.entityData.set(BALEFIRE, balefire);
    }

    public void setLockonTarget(Entity target) {
        this.targetUuid = target == null ? null : target.getUUID();
    }

    private void setAmmoType(StandardAmmoItem.StandardAmmoType ammo) {
        this.entityData.set(AMMO_TYPE, ammo.ordinal());
    }

    private void setBaseDamage(float damage) {
        this.entityData.set(BASE_DAMAGE, damage);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();

        StandardAmmoItem.StandardAmmoType ammo = ammoType();
        Vec3 motion = getDeltaMovement();
        updateRotationFromMotion(motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        List<EntityHitResult> entityHits = findEntityHits(start, entityEnd);

        if (!entityHits.isEmpty()) {
            if (ammo.penetrates()) {
                for (EntityHitResult entityHit : entityHits) {
                    if (impactEntity(ammo, entityHit)) {
                        return;
                    }
                }
                if (blockHit instanceof BlockHitResult blockResult && blockHit.getType() != HitResult.Type.MISS) {
                    if (impactBlock(ammo, blockResult)) {
                        return;
                    }
                    motion = getDeltaMovement();
                }
            } else if (impactEntity(ammo, entityHits.getFirst())) {
                return;
            }
        } else if (blockHit instanceof BlockHitResult blockResult && blockHit.getType() != HitResult.Type.MISS) {
            if (impactBlock(ammo, blockResult)) {
                return;
            }
            motion = getDeltaMovement();
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
            setDeltaMovement(nextRocketMotion(motion));
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME && level().isClientSide) {
            level().addParticle(
                    balefire() ? HbmParticleTypes.FLAMETHROWER_BALEFIRE.get() : HbmParticleTypes.FLAMETHROWER_FIRE.get(),
                    getX(), getY() - 0.125D, getZ(), 0.0D, 0.0D, 0.0D
            );
        }
        int maxLife = ammo.family() == StandardAmmoItem.AmmoFamily.FLAME ? 100 : ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML ? 300 : 30;
        if (!level().isClientSide && (tickCount > maxLife || getY() < level().getMinBuildHeight() - 16)) {
            discard();
        }
    }

    private Vec3 nextRocketMotion(Vec3 previousMotion) {
        Vec3 direction = previousMotion.lengthSqr() > 1.0E-12D
                ? previousMotion.normalize()
                : directionFromRotation();
        Entity target = lockonTarget();
        if (target != null && target.isAlive()) {
            Vec3 delta = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(position());
            double turn = Math.min(0.005D * this.tickCount, 1.0D);
            direction = direction.lerp(delta, turn).normalize();
        }
        double speed = previousMotion.length();
        if (speed < 7.0D) {
            speed += 0.4D;
        }
        return direction.scale(speed);
    }

    private Vec3 directionFromRotation() {
        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(getXRot());
        double horizontal = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * horizontal, -Math.sin(pitch), Math.cos(yaw) * horizontal).normalize();
    }

    private Entity lockonTarget() {
        if (this.targetUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(this.targetUuid);
    }

    private boolean impactEntity(StandardAmmoItem.StandardAmmoType ammo, EntityHitResult entityHit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        Entity hit = entityHit.getEntity();
        if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
            if (this.tickCount < 3) {
                return false;
            }
            LegacyProjectileUtil.rocketImpact(ammo, this, entityHit.getLocation(), hit);
            discard();
            return true;
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME) {
            if (hit instanceof LivingEntity living) {
                HbmLivingHazards hazards = HbmLivingHazards.get(living);
                if (balefire()) {
                    hazards.extendBalefire(200);
                } else {
                    hazards.extendFire(100);
                }
            }
            LegacyProjectileUtil.hurtSednaFire(hit, this, this.entityData.get(BASE_DAMAGE));
            discard();
            return true;
        }
        LegacyProjectileUtil.SednaImpact impact = LegacyProjectileUtil.bulletImpact(
                ammo,
                this,
                hit,
                entityHit.getLocation(),
                this.entityData.get(BASE_DAMAGE)
        );
        setBaseDamage(impact.remainingDamage());
        if (impact.continueFlight()) {
            return false;
        }
        discard();
        return true;
    }

    private boolean impactBlock(StandardAmmoItem.StandardAmmoType ammo, BlockHitResult hit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
            LegacyProjectileUtil.rocketImpact(ammo, this, hit.getLocation(), null);
        } else if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME) {
            if (balefire()) {
                LegacyProjectileUtil.spawnLingeringFire(
                        this,
                        hit.getLocation(),
                        LegacyLingeringFireEntity.FireType.BALEFIRE,
                        3.0F,
                        1.0F,
                        300,
                        true
                );
                discard();
                return true;
            }
            BlockPos blockPos = hit.getBlockPos();
            Direction side = hit.getDirection();
            BlockPos firePos = blockPos.relative(side);
            BlockState state = level().getBlockState(blockPos);
            if (state.isFlammable(level(), blockPos, side.getOpposite()) && level().isEmptyBlock(firePos)) {
                level().setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                setPos(hit.getLocation());
                return false;
            }
            LegacyProjectileUtil.spawnLingeringFire(
                    this,
                    hit.getLocation(),
                    LegacyLingeringFireEntity.FireType.DIESEL,
                    2.0F,
                    1.0F,
                    100,
                    true
            );
        } else if (isSednaBullet(ammo)) {
            return ricochetOrStop(ammo, hit);
        }
        BlockState hitState = level().getBlockState(hit.getBlockPos());
        if (hitState.getBlock() instanceof LegacyBarrelBlock barrel && barrel.detonatesWhenShot()) {
            barrel.detonateOnShot((net.minecraft.server.level.ServerLevel) level(), hit.getBlockPos(), this);
            discard();
            return true;
        }
        discard();
        return true;
    }

    private boolean ricochetOrStop(StandardAmmoItem.StandardAmmoType ammo, BlockHitResult hit) {
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (state.getSoundType(level(), hit.getBlockPos(), this) == SoundType.GLASS) {
            level().destroyBlock(hit.getBlockPos(), false, this);
            setPos(hit.getLocation());
            return false;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        double grazingAngle = Math.toDegrees(Math.asin(Mth.clamp(Math.abs(motion.normalize().dot(normal)), 0.0D, 1.0D)));
        if (grazingAngle > ammo.ricochetAngle() || ++this.ricochets > ammo.maxRicochetCount()) {
            discard();
            return true;
        }

        Vec3 reflected = switch (hit.getDirection().getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        };
        setDeltaMovement(reflected);
        setPos(hit.getLocation());
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_RICOCHET.get(), SoundSource.PLAYERS, 0.25F, 1.0F);
        return false;
    }

    private static boolean isSednaBullet(StandardAmmoItem.StandardAmmoType ammo) {
        return ammo.family() == StandardAmmoItem.AmmoFamily.BMG50
                || ammo.family() == StandardAmmoItem.AmmoFamily.R556
                || ammo.family() == StandardAmmoItem.AmmoFamily.P9;
    }

    private List<EntityHitResult> findEntityHits(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        List<EntityHitResult> hits = new java.util.ArrayList<>();
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable())) {
            Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (optionalHit.isEmpty()) {
                continue;
            }
            hits.add(new EntityHitResult(entity, optionalHit.get()));
        }
        if (!ammoType().penetrates()) {
            hits.sort(java.util.Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())));
        }
        return hits;
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
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ammo_type", this.entityData.get(AMMO_TYPE));
        tag.putFloat("base_damage", this.entityData.get(BASE_DAMAGE));
        tag.putBoolean("balefire", this.entityData.get(BALEFIRE));
        tag.putInt("ricochets", this.ricochets);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(AMMO_TYPE, tag.getInt("ammo_type"));
        this.entityData.set(BASE_DAMAGE, tag.getFloat("base_damage"));
        this.entityData.set(BALEFIRE, tag.getBoolean("balefire"));
        this.ricochets = tag.getInt("ricochets");
        this.targetUuid = null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }
}
