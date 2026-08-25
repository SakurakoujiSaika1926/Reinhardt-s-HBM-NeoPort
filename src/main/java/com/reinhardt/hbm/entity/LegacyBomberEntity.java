package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.client.sound.BomberClientSounds;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.item.LegacyBombCallerItem;
import com.reinhardt.hbm.registry.HbmChunkTickets;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Modern equivalent of 1.7.10 EntityBomber. It traverses the target area for
 * 200 ticks, drops its payload during the original per-type release window,
 * and keeps its route loaded while it is active.
 */
public final class LegacyBomberEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(LegacyBomberEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STYLE =
            SynchedEntityData.defineId(LegacyBomberEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(LegacyBomberEntity.class, EntityDataSerializers.FLOAT);

    private int bombStart = 75;
    private int bombStop = 125;
    private int bombRate = 3;
    private int lifetime = 200;
    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;

    public LegacyBomberEntity(EntityType<? extends LegacyBomberEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    public static LegacyBomberEntity create(Level level, BlockPos target, LegacyBombCallerItem.Type type) {
        LegacyBomberEntity bomber = new LegacyBomberEntity(HbmEntityTypes.LEGACY_BOMBER.get(), level);
        Vec3 route = new Vec3(level.random.nextDouble() - 0.5D, 0.0D, level.random.nextDouble() - 0.5D).normalize();
        double speed = HbmConfig.ENABLE_BOMBER_SHORT_MODE.get() ? 1.0D : 2.0D;
        route = route.scale(speed);
        bomber.setPos(target.getX() - route.x * 100.0D, target.getY() + 50.0D, target.getZ() - route.z * 100.0D);
        bomber.setDeltaMovement(route);
        bomber.configure(type);
        bomber.updateRotation(route);
        return bomber;
    }

    private void configure(LegacyBombCallerItem.Type type) {
        entityData.set(TYPE, type.id());
        entityData.set(STYLE, randomLegacyStyle());
        switch (type) {
            case CARPET -> configure(50, 100, 2);
            case NAPALM -> configure(50, 100, 5);
            case CHLORINE -> configure(50, 100, 4);
            case AGENT_ORANGE -> configure(75, 125, 1);
            case ATOMIC -> {
                configure(60, 70, 65);
                entityData.set(STYLE, random.nextInt(100) == 0 ? 8 : 5 + random.nextInt(3));
            }
            case STINGER -> {
                configure(50, 150, 10);
                entityData.set(STYLE, 4);
            }
            case BOXCARS -> {
                configure(50, 150, 10);
                entityData.set(STYLE, 6);
            }
            case CLOUD -> {
                configure(75, 125, 1);
                entityData.set(STYLE, 6);
            }
        }
    }

    private int randomLegacyStyle() {
        int roll = random.nextInt(7);
        int style = switch (roll) {
            case 0, 1 -> 1;
            case 2, 3 -> 2;
            case 4 -> 5;
            case 5 -> 6;
            default -> 7;
        };
        if (random.nextInt(100) == 0) {
            style = random.nextInt(4) == 3 ? 8 : random.nextInt(4);
        }
        return style;
    }

    private void configure(int start, int stop, int rate) {
        this.bombStart = start;
        this.bombStop = stop;
        this.bombRate = rate;
    }

    public LegacyBombCallerItem.Type payloadType() {
        return LegacyBombCallerItem.Type.byId(entityData.get(TYPE));
    }

    public int style() {
        return entityData.get(STYLE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, LegacyBombCallerItem.Type.CARPET.id());
        builder.define(STYLE, 0);
        builder.define(HEALTH, 50.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation(motion);

        if (level().isClientSide) {
            BomberClientSounds.tick(this);
            return;
        }
        updateForcedChunk();
        if (entityData.get(HEALTH) <= 0.0F) {
            tickCrash();
            return;
        }
        if (tickCount > bombStart && tickCount < bombStop && tickCount % bombRate == 0) {
            dropPayload();
        }
        if (tickCount > lifetime) {
            discard();
        }
    }

    private void dropPayload() {
        Vec3 payloadPosition = new Vec3(getX() + random.nextDouble() - 0.5D,
                getY() - random.nextDouble(), getZ() + random.nextDouble() - 0.5D);
        Vec3 motion = getDeltaMovement();
        switch (payloadType()) {
            case AGENT_ORANGE -> {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(HbmParticleTypes.LEGACY_ORANGE_CLOUD.get(),
                            getX(), getY() - 1.0D, getZ(), 10, 0.0D, 0.0D, 0.0D, 0.5D);
                }
            }
            case STINGER -> {
                // EntityBomber's type 5 branch is intentionally empty in 1.7.10.
            }
            case BOXCARS -> {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_MISSILE_TAKEOFF.get(),
                        SoundSource.HOSTILE, 10.0F, 0.9F + random.nextFloat() * 0.2F);
                level().addFreshEntity(new LegacyBoxcarEntity(level(), payloadPosition));
            }
            case CLOUD -> {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                int surface = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) getX(), (int) getZ()) + 2;
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(HbmParticleTypes.LEGACY_PINK_CLOUD.get(),
                            getX(), surface, getZ(), 10, 0.0D, 0.0D, 0.0D, 1.0D);
                }
            }
            default -> {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_BOMB_WHISTLE.get(),
                        SoundSource.HOSTILE, 10.0F, 0.9F + random.nextFloat() * 0.2F);
                Vec3 payloadMotion = payloadType() == LegacyBombCallerItem.Type.CARPET
                        ? new Vec3(motion.x + random.nextGaussian() * 0.15D, 0.0D,
                        motion.z + random.nextGaussian() * 0.15D)
                        : new Vec3(motion.x, 0.0D, motion.z);
                level().addFreshEntity(new LegacyBombletEntity(level(), payloadPosition, payloadMotion, payloadType().id()));
            }
        }
    }

    private void tickCrash() {
        Vec3 motion = getDeltaMovement().add(0.0D, -0.025D, 0.0D);
        setDeltaMovement(motion);
        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 10; i++) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        getX() + random.nextGaussian() * 0.5D - motion.x * 2.0D,
                        getY() + random.nextGaussian() * 0.5D - motion.y * 2.0D,
                        getZ() + random.nextGaussian() * 0.5D - motion.z * 2.0D,
                        1, 0.0D, 0.1D, 0.0D, 0.0D);
            }
        }
        BlockPos collision = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        if (!level().getBlockState(collision).isAir() || getY() < 0.0D) {
            LegacyProjectileUtil.standardExplosion(this, position(), 15.0F, 1.0F, true, true);
            level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(),
                    SoundSource.HOSTILE, 25.0F, 1.0F);
            discard();
        }
    }

    private void updateRotation(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-7D) {
            return;
        }
        yRotO = getYRot();
        xRotO = getXRot();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setYRot(Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot(Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(motion.y, horizontal)) - 90.0F));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("type", entityData.get(TYPE));
        tag.putInt("style", entityData.get(STYLE));
        tag.putInt("bomb_start", bombStart);
        tag.putInt("bomb_stop", bombStop);
        tag.putInt("bomb_rate", bombRate);
        tag.putInt("lifetime", lifetime);
        tag.putFloat("health", entityData.get(HEALTH));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("type"));
        entityData.set(STYLE, tag.getInt("style"));
        bombStart = tag.getInt("bomb_start");
        bombStop = tag.getInt("bomb_stop");
        bombRate = tag.getInt("bomb_rate");
        lifetime = tag.getInt("lifetime");
        entityData.set(HEALTH, tag.contains("health") ? tag.getFloat("health") : 50.0F);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public boolean isFlying() {
        return entityData.get(HEALTH) > 0.0F;
    }

    @Override
    public boolean isPickable() {
        return isFlying();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !isFlying()) {
            return false;
        }
        float health = entityData.get(HEALTH) - amount;
        entityData.set(HEALTH, health);
        if (health <= 0.0F) {
            LegacyProjectileUtil.sendSmallExplosionEffect(level(), position(), 25, 3.5F, 2.0F);
        }
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunk();
        super.remove(reason);
    }

    private void updateForcedChunk() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int chunkX = Mth.floor(getX()) >> 4;
        int chunkZ = Mth.floor(getZ()) >> 4;
        if (chunkX == forcedChunkX && chunkZ == forcedChunkZ) {
            return;
        }
        releaseForcedChunk(serverLevel);
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(serverLevel, this, chunkX, chunkZ, true, true);
        forcedChunkX = chunkX;
        forcedChunkZ = chunkZ;
    }

    private void releaseForcedChunk() {
        if (level() instanceof ServerLevel serverLevel) {
            releaseForcedChunk(serverLevel);
        }
    }

    private void releaseForcedChunk(ServerLevel serverLevel) {
        if (forcedChunkX == Integer.MIN_VALUE) {
            return;
        }
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(serverLevel, this, forcedChunkX, forcedChunkZ, false, true);
        forcedChunkX = Integer.MIN_VALUE;
        forcedChunkZ = Integer.MIN_VALUE;
    }
}
