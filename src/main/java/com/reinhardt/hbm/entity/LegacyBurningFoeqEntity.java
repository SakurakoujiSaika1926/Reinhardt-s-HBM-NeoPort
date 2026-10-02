package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.worldgen.NuclearFalloutTerrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Literal gameplay port of 1.7.10's EntityBurningFOEQ cataclysm projectile. */
public final class LegacyBurningFoeqEntity extends Entity {
    private static final double TERMINAL_VELOCITY = -4.0D;

    public LegacyBurningFoeqEntity(EntityType<? extends LegacyBurningFoeqEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        yRotO = getYRot();
        xRotO = getXRot();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (motion.y > TERMINAL_VELOCITY) {
            motion = motion.add(0.0D, -0.1D, 0.0D);
            setDeltaMovement(motion);
        }
        updateLegacyRotation(motion);

        // The old code intentionally used Java integer casts rather than a
        // ray trace. Preserve that impact coordinate rule at negative X/Z.
        BlockPos impact = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        if (!level().getBlockState(impact).isAir()) {
            if (level() instanceof ServerLevel serverLevel) {
                impact(serverLevel);
            }
            discard();
        }
    }

    private void updateLegacyRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG) - 90.0F);

        while (getXRot() - xRotO < -180.0F) xRotO -= 360.0F;
        while (getXRot() - xRotO >= 180.0F) xRotO += 360.0F;
        while (getYRot() - yRotO < -180.0F) yRotO -= 360.0F;
        while (getYRot() - yRotO >= 180.0F) yRotO += 360.0F;
    }

    private void impact(ServerLevel level) {
        for (int index = 0; index < 25; index++) {
            double x = getX() + 0.5D + random.nextGaussian() * 5.0D;
            double y = getY() + 0.5D + random.nextGaussian() * 5.0D;
            double z = getZ() + 0.5D + random.nextGaussian() * 5.0D;
            boolean cloud = random.nextBoolean();
            level.explode(null, x, y, z, 10.0F, false, Level.ExplosionInteraction.BLOCK);
            if (cloud) {
                // ExplosionLarge.cloudFunction(10) evaluates to 15 in the
                // original code because its exponent uses integer division.
                level.sendParticles(HbmParticleTypes.LEGACY_EXPLOSION_CLOUD.get(),
                        x, y, z, 15, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        NuclearFalloutTerrainEffects.schedule(level,
                new BlockPos((int) getX(), (int) getY(), (int) getZ()), 35);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 100_000.0D;
    }
}
