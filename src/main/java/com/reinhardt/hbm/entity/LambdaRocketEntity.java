package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Exact one-payload flight behavior of the latest EntityRocketLambda. */
public final class LambdaRocketEntity extends Entity {
    private ItemStack payload = ItemStack.EMPTY;
    private double acceleration;

    public LambdaRocketEntity(EntityType<? extends LambdaRocketEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public LambdaRocketEntity(Level level) {
        this(HbmEntityTypes.LAMBDA_ROCKET.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public void setPayload(ItemStack payload) {
        this.payload = payload.copy();
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            this.acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + this.acceleration, motion.z);
        }
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level().isClientSide) {
            spawnClientExhaust();
        } else if (getY() > 600.0D && level() instanceof ServerLevel serverLevel) {
            SatelliteSavedData.get(serverLevel).orbit(serverLevel, this.payload, getX(), getZ());
            discard();
        }
    }

    private void spawnClientExhaust() {
        level().addParticle(ParticleTypes.FLAME, getX(), getY() + 1.0D, getZ(),
                0.0D, -0.15D, 0.0D);
        level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 1.0D, getZ(),
                0.0D, -0.1D, 0.0D);
        if (tickCount % 10 == 0) {
            int direction = random.nextInt(8);
            double angle = Math.toRadians(direction * 45.0D);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    getX() - x * 0.5D, getY(), getZ() - z * 0.5D,
                    x, getDeltaMovement().y - 0.5D, z);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500_000.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("Acceleration", this.acceleration);
        if (!this.payload.isEmpty()) {
            tag.put("Payload", this.payload.saveOptional(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.acceleration = tag.getDouble("Acceleration");
        this.payload = ItemStack.parseOptional(registryAccess(), tag.getCompound("Payload"));
    }
}
