package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Direct 1.7.10 EntityBoxcar port used by the type-6 airstrike. */
public final class LegacyBoxcarEntity extends Entity {
    public LegacyBoxcarEntity(EntityType<? extends LegacyBoxcarEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyBoxcarEntity(Level level, Vec3 position) {
        this(HbmEntityTypes.LEGACY_BOXCAR.get(), level);
        setPos(position);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount == 1 && level() instanceof ServerLevel serverLevel) {
            // EntityBoxcar emits fifty legacy "bf" particles when it begins falling.
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 50,
                    1.5D, 7.5D, 1.5D, 0.0D);
        }
        xo = getX();
        yo = getY();
        zo = getZ();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double nextY = Math.max(-1.5D, motion.y - 0.03D);
        setDeltaMovement(motion.x, nextY, motion.z);
        if (!level().isClientSide && !level().getBlockState(legacyBlockPos()).isAir()) {
            impact();
        }
    }

    private void impact() {
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_TRAIN_IMPACT.get(),
                SoundSource.HOSTILE, 100.0F, 1.0F);
        Vec3 center = new Vec3(getX(), getY() + 1.0D, getZ());
        for (Entity entity : level().getEntities(this, new AABB(getX() - 2.0D, getY() - 2.0D, getZ() - 2.0D,
                getX() + 2.0D, getY() + 2.0D, getZ() + 2.0D), Entity::isAlive)) {
            entity.hurt(damageSources().source(HbmDamageTypes.BOXCAR, this, this), 1000.0F);
        }
        if (level() instanceof ServerLevel serverLevel) {
            LegacyProjectileUtil.sendSmallExplosionEffect(serverLevel, center, 24, 3.0F, 1.0F);
            LegacyProjectileUtil.sendSmallExplosionEffect(serverLevel, center, 24, 2.5F, 1.0F);
            LegacyProjectileUtil.sendSmallExplosionEffect(serverLevel, center, 24, 2.0F, 1.0F);
            serverLevel.setBlock(new BlockPos((int) Math.floor(getX()), (int) Math.floor(getY() + 0.5D),
                    (int) Math.floor(getZ())), HbmBlocks.BOXCAR.get().defaultBlockState(), 3);
        }
        discard();
    }

    private BlockPos legacyBlockPos() {
        return new BlockPos((int) getX(), (int) getY(), (int) getZ());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000.0D;
    }
}
