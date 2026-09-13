package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

/** The 1.7.10 EntityAcidBomb used by the bombardier. */
public final class GlyphidAcidBombEntity extends Entity {
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(GlyphidAcidBombEntity.class, EntityDataSerializers.FLOAT);
    private Entity owner;

    public GlyphidAcidBombEntity(EntityType<? extends GlyphidAcidBombEntity> type, Level level) {
        super(type, level);
    }

    public GlyphidAcidBombEntity(Level level, Entity owner, Vec3 position, Vec3 direction,
                                 float velocity, float inaccuracy, float damage) {
        this(HbmEntityTypes.GLYPHID_ACID_BOMB.get(), level);
        this.owner = owner;
        setPos(position);
        entityData.set(DAMAGE, damage);
        Vec3 motion = direction.normalize().add(
                random.nextGaussian() * 0.0075D * inaccuracy,
                random.nextGaussian() * 0.0075D * inaccuracy,
                random.nextGaussian() * 0.0075D * inaccuracy
        ).scale(velocity);
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 1.5F);
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);
        if (entityHit != null && !(entityHit.getEntity() instanceof GlyphidEntity)) {
            if (!level().isClientSide) {
                entityHit.getEntity().hurt(damageSources().source(HbmDamageTypes.ACID, this, owner),
                        entityData.get(DAMAGE));
            }
            discard();
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            discard();
            return;
        }

        move(MoverType.SELF, motion);
        updateRotation(motion);
        setDeltaMovement(motion.x, motion.y - 0.04D, motion.z);
        if (!level().isClientSide && (tickCount > 1200 || getY() < level().getMinBuildHeight() - 16)) {
            discard();
        }
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        return level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable() && entity != owner)
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.3D).clip(start, end)
                        .map(point -> new EntityHitResult(entity, point)))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())))
                .orElse(null);
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        yRotO = getYRot();
        xRotO = getXRot();
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) (Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("damage", entityData.get(DAMAGE));
        if (owner != null) {
            tag.putUUID("owner", owner.getUUID());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DAMAGE, tag.getFloat("damage"));
        if (tag.hasUUID("owner") && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            owner = serverLevel.getEntity(tag.getUUID("owner"));
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }
}
