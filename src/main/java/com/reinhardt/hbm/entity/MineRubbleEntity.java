package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class MineRubbleEntity extends Entity {
    private static final EntityDataAccessor<Integer> BLOCK_STATE =
            SynchedEntityData.defineId(MineRubbleEntity.class, EntityDataSerializers.INT);

    public MineRubbleEntity(EntityType<? extends MineRubbleEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public MineRubbleEntity(Level level, double x, double y, double z, Vec3 motion) {
        this(level, x, y, z, motion, Blocks.STONE.defaultBlockState());
    }

    public MineRubbleEntity(Level level, double x, double y, double z, Vec3 motion, BlockState state) {
        this(HbmEntityTypes.MINE_RUBBLE.get(), level);
        setPos(x, y, z);
        setDeltaMovement(motion);
        setBlockState(state);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_STATE, Block.getId(Blocks.STONE.defaultBlockState()));
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_STATE, Block.getId(state));
    }

    public BlockState blockState() {
        BlockState state = Block.stateById(this.entityData.get(BLOCK_STATE));
        return state == null ? Blocks.STONE.defaultBlockState() : state;
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
            LegacyProjectileUtil.hurtNoIFrame(entityHit.getEntity(),
                    damageSources().source(HbmDamageTypes.RUBBLE, this, null), 15.0F);
        }
        if (!level().isClientSide && hit.getType() != HitResult.Type.MISS && this.tickCount > 2) {
            ServerLevel serverLevel = (ServerLevel) level();
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState()),
                    getX(), getY(), getZ(), 8, 0.1D, 0.1D, 0.1D, 0.05D);
            serverLevel.playSound(null, getX(), getY(), getZ(), HbmSoundEvents.BLOCK_DEBRIS.get(),
                    SoundSource.BLOCKS, 1.5F, 1.0F);
            discard();
            return;
        }
        move(MoverType.SELF, motion);
        setDeltaMovement(motion.x, motion.y - 0.03D, motion.z);
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity closest = null;
        Vec3 closestLocation = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, area, candidate -> candidate.isAlive() && candidate.isPickable())) {
            var hit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closest = entity;
                closestLocation = hit.get();
                closestDistance = distance;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestLocation);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("block", this.entityData.get(BLOCK_STATE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(BLOCK_STATE, tag.getInt("block"));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityRubble inherits EntityThrowableNT's 0.25-wide render range:
        // (average edge 0.25 * 4 * 64)^2 = 4096.
        return distance < 4096.0D;
    }
}
