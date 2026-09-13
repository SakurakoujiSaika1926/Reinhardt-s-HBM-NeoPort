package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.explosion.LegacyMukeExplosion;
import com.reinhardt.hbm.item.TurretAmmoItem;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JeremyShellEntity extends Entity {
    private static final EntityDataAccessor<Integer> AMMO_TYPE =
            SynchedEntityData.defineId(JeremyShellEntity.class, EntityDataSerializers.INT);
    private static final double SPEED = 10.0D;
    private float damageRemaining;
    private int ricochets;

    public JeremyShellEntity(EntityType<? extends JeremyShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public JeremyShellEntity(Level level, double x, double y, double z, Vec3 direction, TurretAmmoItem.AmmoType ammoType) {
        this(HbmEntityTypes.JEREMY_SHELL.get(), level);
        setPos(x, y, z);
        setAmmoType(ammoType);
        this.damageRemaining = ammoType.damage();
        Vec3 motion = direction.normalize().scale(SPEED);
        setDeltaMovement(motion);
        updateRotationFromMotion(motion);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AMMO_TYPE, TurretAmmoItem.AmmoType.STOCK.ordinal());
    }

    public TurretAmmoItem.AmmoType ammoType() {
        return TurretAmmoItem.AmmoType.byOrdinal(this.entityData.get(AMMO_TYPE));
    }

    private void setAmmoType(TurretAmmoItem.AmmoType type) {
        this.entityData.set(AMMO_TYPE, type.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        Vec3 motion = getDeltaMovement();
        updateRotationFromMotion(motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        List<EntityHitResult> entityHits = findEntityHits(start, entityEnd);
        TurretAmmoItem.AmmoType type = ammoType();
        if (type.penetrates()) {
            for (EntityHitResult entityHit : entityHits) {
                if (impactPenetratorEntity(type, entityHit)) {
                    return;
                }
            }
            if (blockHit instanceof BlockHitResult blockResult && blockHit.getType() != HitResult.Type.MISS) {
                if (ricochetOrStop(blockResult)) {
                    return;
                }
                motion = getDeltaMovement();
            }
        } else {
            HitResult hit = entityHits.isEmpty() ? blockHit : entityHits.getFirst();
            if (hit.getType() != HitResult.Type.MISS && impactExplosive(type, hit)) {
                return;
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (!level().isClientSide && (tickCount > 30 || getY() < level().getMinBuildHeight() - 16)) {
            discard();
        }
    }

    private void updateRotationFromMotion(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) -Math.toDegrees(Math.atan2(motion.y, horizontal)));
    }

    private List<EntityHitResult> findEntityHits(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        List<EntityHitResult> hits = new ArrayList<>();
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable())) {
            AABB box = entity.getBoundingBox().inflate(0.3D);
            var optionalHit = box.clip(start, end);
            if (optionalHit.isEmpty()) {
                continue;
            }
            hits.add(new EntityHitResult(entity, optionalHit.get()));
        }
        if (!ammoType().penetrates()) {
            hits.sort(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())));
        }
        return hits;
    }

    private boolean impactPenetratorEntity(TurretAmmoItem.AmmoType type, EntityHitResult hit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        Entity target = hit.getEntity();
        if (target instanceof LivingEntity living) {
            LegacyProjectileUtil.SednaImpact impact = LegacyProjectileUtil.turretShellImpact(
                    this,
                    living,
                    hit.getLocation(),
                    this.damageRemaining,
                    true,
                    type.damageFalloffByPenetration()
            );
            this.damageRemaining = impact.remainingDamage();
            if (!impact.continueFlight()) {
                discard();
                return true;
            }
        } else {
            LegacyProjectileUtil.hurtNoIFrame(
                    target,
                    damageSources().source(HbmDamageTypes.SEDNA_PHYSICAL, this, null),
                    this.damageRemaining
            );
        }
        return false;
    }

    private boolean impactExplosive(TurretAmmoItem.AmmoType type, HitResult hit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        Vec3 pos = hit.getLocation();
        if (type == TurretAmmoItem.AmmoType.W9 && level() instanceof ServerLevel serverLevel) {
            LegacyMukeExplosion.detonateW9(serverLevel, this, pos, type.damage());
        } else if (type == TurretAmmoItem.AmmoType.STOCK) {
            LegacyProjectileUtil.fixedDamageExplosion(this, pos, 10.0F, type.damage(), false);
        } else if (type == TurretAmmoItem.AmmoType.EXPLOSIVE) {
            LegacyProjectileUtil.fixedDamageExplosion(this, pos, 10.0F, type.damage(), true);
        }
        discard();
        return true;
    }

    private boolean ricochetOrStop(BlockHitResult hit) {
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (state.getSoundType(level(), hit.getBlockPos(), this) == SoundType.GLASS) {
            level().destroyBlock(hit.getBlockPos(), false, this);
            setPos(hit.getLocation());
            return false;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        double grazingAngle = Math.toDegrees(Math.asin(Mth.clamp(Math.abs(motion.normalize().dot(normal)), 0.0D, 1.0D)));
        if (grazingAngle > 5.0D || ++this.ricochets > 2) {
            setPos(hit.getLocation());
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

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ammo_type", this.entityData.get(AMMO_TYPE));
        tag.putFloat("damage_remaining", this.damageRemaining);
        tag.putInt("ricochets", this.ricochets);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(AMMO_TYPE, tag.getInt("ammo_type"));
        this.damageRemaining = tag.contains("damage_remaining") ? tag.getFloat("damage_remaining") : ammoType().damage();
        this.ricochets = tag.getInt("ricochets");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 102400.0D;
    }
}
