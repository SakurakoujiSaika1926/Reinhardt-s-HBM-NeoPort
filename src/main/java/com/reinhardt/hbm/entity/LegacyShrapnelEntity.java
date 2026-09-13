package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LegacyShrapnelEntity extends Entity {
    private static final EntityDataAccessor<Byte> MODE =
            SynchedEntityData.defineId(LegacyShrapnelEntity.class, EntityDataSerializers.BYTE);

    public LegacyShrapnelEntity(EntityType<? extends LegacyShrapnelEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyShrapnelEntity(Level level, double x, double y, double z, Vec3 motion, boolean trail) {
        this(HbmEntityTypes.LEGACY_SHRAPNEL.get(), level);
        setPos(x, y, z);
        setDeltaMovement(motion);
        this.entityData.set(MODE, (byte) (trail ? 1 : 0));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);
        BlockHitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
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
            byte mode = this.entityData.get(MODE);
            if ((mode == 2 || mode == 4) && blockHit.getType() != HitResult.Type.MISS) {
                BlockPos hitPos = blockHit.getBlockPos();
                if (getDeltaMovement().y < -0.2D) {
                    BlockState lava = (mode == 4 ? HbmBlocks.RAD_LAVA_BLOCK : HbmBlocks.VOLCANIC_LAVA_BLOCK)
                            .get().defaultBlockState();
                    BlockPos above = hitPos.above();
                    if (serverLevel.getBlockState(above).canBeReplaced()) {
                        serverLevel.setBlock(above, lava, Block.UPDATE_ALL);
                    }
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 0; y <= 2; y++) {
                            for (int z = -1; z <= 1; z++) {
                                BlockPos gasPos = hitPos.offset(x, y, z);
                                if (serverLevel.isEmptyBlock(gasPos)) {
                                    serverLevel.setBlock(gasPos, HbmBlocks.GAS_MONOXIDE.get().defaultBlockState(), Block.UPDATE_ALL);
                                }
                            }
                        }
                    }
                } else if (getDeltaMovement().y > 0.0D) {
                    LegacyProjectileUtil.volcanicTerrainExplosion(serverLevel,
                            Vec3.atCenterOf(hitPos), 7.0F,
                            (mode == 4 ? HbmBlocks.RAD_LAVA_BLOCK : HbmBlocks.VOLCANIC_LAVA_BLOCK)
                                    .get().defaultBlockState());
                }
            } else if (mode == 3 && blockHit.getType() != HitResult.Type.MISS) {
                BlockPos hitPos = blockHit.getBlockPos();
                BlockPos above = hitPos.above();
                if (serverLevel.getBlockState(above).canBeReplaced()) {
                    serverLevel.setBlock(above, HbmBlocks.MUD_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            } else if (entityHit == null && mode == 0) {
                serverLevel.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 5,
                        0.0D, 0.0D, 0.0D, 0.0D);
            }
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            discard();
            return;
        }

        move(MoverType.SELF, motion);
        setDeltaMovement(getDeltaMovement().scale(0.99D).add(0.0D, -0.03D, 0.0D));
        if (this.entityData.get(MODE) == 1 && level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
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
        tag.putByte("mode", this.entityData.get(MODE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(MODE, tag.contains("mode") ? tag.getByte("mode") : (byte) (tag.getBoolean("trail") ? 1 : 0));
    }

    public void setTrail(boolean trail) {
        this.entityData.set(MODE, (byte) (trail ? 1 : 0));
    }

    public void setVolcano(boolean volcano) {
        if (volcano) this.entityData.set(MODE, (byte) 2);
    }

    public void setWatz(boolean watz) {
        if (watz) this.entityData.set(MODE, (byte) 3);
    }

    public void setRadVolcano(boolean radioactive) {
        if (radioactive) this.entityData.set(MODE, (byte) 4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D;
    }
}
