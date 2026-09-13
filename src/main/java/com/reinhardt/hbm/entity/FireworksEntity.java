package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.client.FireworksClientEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Rising shell and letter burst used by the 1.7.10 fireworks battery. */
public final class FireworksEntity extends Entity {
    public static final int FLIGHT_TICKS = 30;
    public static final int LETTER_TICKS = 30;

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(FireworksEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARACTER =
            SynchedEntityData.defineId(FireworksEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPLODED =
            SynchedEntityData.defineId(FireworksEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean clientBurstSpawned;

    public FireworksEntity(EntityType<? extends FireworksEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    public FireworksEntity(
            EntityType<? extends FireworksEntity> type,
            Level level,
            double x,
            double y,
            double z,
            int color,
            int character
    ) {
        this(type, level);
        setPos(x, y, z);
        entityData.set(COLOR, color & 0xffffff);
        entityData.set(CHARACTER, character);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xff0000);
        builder.define(CHARACTER, (int) ' ');
        builder.define(EXPLODED, false);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public char character() {
        return (char) entityData.get(CHARACTER).intValue();
    }

    public boolean exploded() {
        return entityData.get(EXPLODED);
    }

    public float letterAge(float partialTick) {
        return Math.max(0.0F, tickCount - FLIGHT_TICKS - 1.0F + partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        if (!exploded()) {
            move(MoverType.SELF, new Vec3(0.0D, 3.0D, 0.0D));
            if (level().isClientSide) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, -0.3D, 0.0D);
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, -0.2D, 0.0D);
            } else if (tickCount > FLIGHT_TICKS) {
                entityData.set(EXPLODED, true);
                setDeltaMovement(Vec3.ZERO);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                        SoundSource.BLOCKS, 20.0F, 1.0F + random.nextFloat() * 0.2F);
            }
            return;
        }

        if (level().isClientSide && !clientBurstSpawned) {
            clientBurstSpawned = true;
            spawnClientBurst();
        }
        if (!level().isClientSide && tickCount > FLIGHT_TICKS + LETTER_TICKS) {
            discard();
        }
    }

    private void spawnClientBurst() {
        FireworksClientEffects.spawnBurst(this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Color", color());
        tag.putInt("Character", entityData.get(CHARACTER));
        tag.putBoolean("Exploded", exploded());
        tag.putInt("Age", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(COLOR, tag.getInt("Color") & 0xffffff);
        entityData.set(CHARACTER, tag.getInt("Character"));
        entityData.set(EXPLODED, tag.getBoolean("Exploded"));
        tickCount = Math.max(0, tag.getInt("Age"));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }
}
