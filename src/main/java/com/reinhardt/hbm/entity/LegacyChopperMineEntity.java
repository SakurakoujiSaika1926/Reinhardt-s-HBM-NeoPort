package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** EntityChopperMine's 100-tick fuse, huge collision field, and non-griefing blast. */
public final class LegacyChopperMineEntity extends Entity {
    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(LegacyChopperMineEntity.class, EntityDataSerializers.INT);
    private UUID ownerUuid;

    public LegacyChopperMineEntity(EntityType<? extends LegacyChopperMineEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    public void setOwner(Entity entity) {
        ownerUuid = entity == null ? null : entity.getUUID();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUSE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
        EntityHitResult playerHit = findPlayerHit(start, traceEnd, motion);
        // The original mine only shortens its projectile trace against blocks;
        // it detonates on the next tick after actually entering a non-air block.
        if (playerHit != null || !level().getBlockState(blockPosition()).isAir() || entityData.get(FUSE) >= 100) {
            level().explode(owner(), getX(), getY(), getZ(), 5.0F, false, Level.ExplosionInteraction.NONE);
            discard();
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.MISC_NULL_MINE.get(),
                SoundSource.PLAYERS, 10.0F, 1.0F);
        if (motion.y > -0.85D) motion = motion.add(0.0D, -0.05D, 0.0D);
        motion = new Vec3(motion.x * 0.9D, motion.y, motion.z * 0.9D);
        setDeltaMovement(motion);
        // EntityChopperMine changes coordinates directly, so block collision
        // cannot prevent the delayed non-griefing detonation above.
        setPos(end);
        entityData.set(FUSE, entityData.get(FUSE) + 1);
    }

    private Entity owner() {
        return ownerUuid != null && level() instanceof ServerLevel server ? server.getEntity(ownerUuid) : null;
    }

    private EntityHitResult findPlayerHit(Vec3 start, Vec3 end, Vec3 motion) {
        AABB area = getBoundingBox().expandTowards(motion).inflate(1.0D);
        EntityHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, area,
                candidate -> candidate instanceof Player && candidate.canBeCollidedWith() && candidate != owner())) {
            var hit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = new EntityHitResult(entity, hit.get());
            }
        }
        return closest;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("fuse", entityData.get(FUSE));
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(FUSE, tag.getInt("fuse"));
        ownerUuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
