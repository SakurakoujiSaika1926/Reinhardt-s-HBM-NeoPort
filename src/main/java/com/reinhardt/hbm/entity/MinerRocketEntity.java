package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Return vehicle used by the two legacy mining satellites. */
public final class MinerRocketEntity extends Entity {
    private static final int LANDING = 0;
    private static final int UNLOADING = 1;
    private static final int LIFTING = 2;
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(MinerRocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FREQUENCY =
            SynchedEntityData.defineId(MinerRocketEntity.class, EntityDataSerializers.INT);

    private int timer;

    public MinerRocketEntity(EntityType<? extends MinerRocketEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public MinerRocketEntity(Level level, int frequency) {
        this(HbmEntityTypes.MINER_ROCKET.get(), level);
        setFrequency(frequency);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, LANDING);
        builder.define(FREQUENCY, 0);
    }

    public int frequency() { return entityData.get(FREQUENCY); }
    public void setFrequency(int frequency) { entityData.set(FREQUENCY, frequency); }
    public boolean shouldUnloadCargo() { return entityData.get(MODE) == UNLOADING && timer == 50; }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        int mode = entityData.get(MODE);
        double motionY = mode == LANDING ? -0.75D : mode == LIFTING ? 1.0D : 0.0D;
        // EntityMinerRocket zeroed its horizontal motion and orientation on
        // every update; retaining any externally supplied X/Z velocity would
        // make the return vehicle drift away from its satellite dock.
        setDeltaMovement(0.0D, motionY, 0.0D);
        setYRot(0.0F);
        setXRot(0.0F);
        setPos(getX(), getY() + motionY, getZ());

        BlockPos landing = new BlockPos((int) (getX() - 0.5D), (int) (getY() - 0.5D), (int) (getZ() - 0.5D));
        if (mode == LANDING && level().getBlockState(landing).is(com.reinhardt.hbm.registry.HbmBlocks.SAT_DOCK.get())) {
            entityData.set(MODE, UNLOADING);
            setPos(getX(), (int) getY(), getZ());
        } else if (mode != UNLOADING && !level().isClientSide
                && !level().getBlockState(new BlockPos((int) (getX() - 0.5D), (int) (getY() + 1.0D), (int) (getZ() - 0.5D))).isAir()) {
            // EntityMinerRocket used the flaming ExplosionLarge path
            // (break blocks, place fire, and emit its standard cloud effects).
            level().explode(null, getX() - 0.5D, getY(), getZ() - 0.5D,
                    10.0F, true, Level.ExplosionInteraction.BLOCK);
            discard();
            return;
        }

        if (entityData.get(MODE) == UNLOADING) {
            if (!level().isClientSide && tickCount % 4 == 0 && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 1,
                        0.15D + random.nextInt(3) * 0.05D, 0.0D, 0.15D + random.nextInt(3) * 0.05D, random.nextGaussian());
            }
            timer++;
            if (timer > 100) {
                entityData.set(MODE, LIFTING);
            }
        }

        if (entityData.get(MODE) != UNLOADING) {
            if (level().isClientSide && tickCount % 2 == 0) {
                level().addParticle(HbmParticleTypes.GAS_FLARE_FLAME.get(), getX(), getY() - 0.5D, getZ(), 0.0D, -1.0D, 0.0D);
            } else if (!level().isClientSide && tickCount % 2 == 0 && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.5D, getZ(), 1, 0.02D, 0.0D, 0.02D, 0.02D);
            }
        }

        if (entityData.get(MODE) == LIFTING && getY() > 300.0D) {
            discard();
        }
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 500_000.0D; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", entityData.get(MODE));
        tag.putInt("Satellite", frequency());
        tag.putInt("Timer", timer);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MODE, tag.getInt("Mode"));
        setFrequency(tag.getInt("Satellite"));
        timer = tag.getInt("Timer");
    }
}
