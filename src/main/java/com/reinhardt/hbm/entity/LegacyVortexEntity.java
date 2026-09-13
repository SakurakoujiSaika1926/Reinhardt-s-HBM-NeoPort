package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Direct 1.7.10 EntityVortex port. It is a shrinking, non-colliding black
 * hole which keeps the old pull, kill and optional block-consumption behavior.
 */
public final class LegacyVortexEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(LegacyVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHRINK_RATE =
            SynchedEntityData.defineId(LegacyVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BREAKS_BLOCKS =
            SynchedEntityData.defineId(LegacyVortexEntity.class, EntityDataSerializers.BOOLEAN);

    public LegacyVortexEntity(EntityType<? extends LegacyVortexEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static void spawn(Level level, Vec3 position, float size, float shrinkRate, boolean noBreak) {
        if (level.isClientSide) {
            return;
        }
        LegacyVortexEntity vortex = new LegacyVortexEntity(HbmEntityTypes.LEGACY_VORTEX.get(), level);
        vortex.setPos(position.x, position.y, position.z);
        vortex.entityData.set(SIZE, size);
        vortex.entityData.set(SHRINK_RATE, shrinkRate);
        vortex.entityData.set(BREAKS_BLOCKS, !noBreak);
        level.addFreshEntity(vortex);
    }

    /** EntityBlackHole's fixed-size launcher variant (no shrink and block intake). */
    public static void spawnBlackHole(Level level, Vec3 position) {
        spawn(level, position, 1.5F, 0.0F, false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 0.5F);
        builder.define(SHRINK_RATE, 0.0025F);
        builder.define(BREAKS_BLOCKS, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            float nextSize = size() - shrinkRate();
            entityData.set(SIZE, nextSize);
            if (nextSize <= 0.0F) {
                discard();
                return;
            }
            if (entityData.get(BREAKS_BLOCKS)) {
                consumeBlocks(nextSize);
            }
            pullEntities(nextSize);
        }

        setDeltaMovement(getDeltaMovement().scale(0.99D));
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
    }

    private void consumeBlocks(float size) {
        int rays = (int) Math.ceil(size * 2.0F);
        int length = (int) Math.ceil(size * 15.0F);
        ServerLevel level = (ServerLevel) level();
        for (int ray = 0; ray < rays; ray++) {
            double phi = random.nextDouble() * Math.PI * 2.0D;
            double cosTheta = random.nextDouble() * 2.0D - 1.0D;
            double theta = Math.acos(cosTheta);
            Vec3 direction = new Vec3(
                    Math.sin(theta) * Math.cos(phi),
                    Math.sin(theta) * Math.sin(phi),
                    Math.cos(theta)
            );
            for (int step = 0; step < length; step++) {
                BlockPos pos = BlockPos.containing(getX() + direction.x * step, getY() + direction.y * step, getZ() + direction.z * step);
                FluidState fluid = level.getFluidState(pos);
                if (!fluid.isEmpty()) {
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F) {
                    level.destroyBlock(pos, false);
                    break;
                }
            }
        }
    }

    private void pullEntities(float size) {
        double range = size * 15.0D;
        AABB bounds = new AABB(getX() - range, getY() - range, getZ() - range, getX() + range, getY() + range, getZ() + range);
        for (Entity target : level().getEntities(this, bounds, Entity::isAlive)) {
            if (target instanceof Player player && player.getAbilities().instabuild) {
                continue;
            }
            if (target instanceof LegacyVortexEntity) {
                continue;
            }
            Vec3 delta = position().subtract(target.position());
            double distance = delta.length();
            if (distance > range || distance < 1.0E-5D) {
                continue;
            }
            Vec3 pull = delta.scale(1.0D / distance);
            if (!(target instanceof net.minecraft.world.entity.item.ItemEntity)) {
                pull = pull.yRot((float) Math.toRadians(15.0D));
            }
            target.setDeltaMovement(target.getDeltaMovement().add(pull.x * 0.1D, pull.y * 0.2D, pull.z * 0.1D));
            target.hurtMarked = true;
            if (distance < size * 1.5D) {
                target.hurt(damageSources().source(HbmDamageTypes.BLACK_HOLE, this, null), 1000.0F);
                if (!(target instanceof LivingEntity)) {
                    target.discard();
                }
            }
        }
    }

    public float size() {
        return entityData.get(SIZE);
    }

    private float shrinkRate() {
        return entityData.get(SHRINK_RATE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("size", size());
        tag.putFloat("shrink_rate", shrinkRate());
        tag.putBoolean("breaks_blocks", entityData.get(BREAKS_BLOCKS));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SIZE, tag.getFloat("size"));
        entityData.set(SHRINK_RATE, tag.getFloat("shrink_rate"));
        entityData.set(BREAKS_BLOCKS, tag.getBoolean("breaks_blocks"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000.0D;
    }
}
