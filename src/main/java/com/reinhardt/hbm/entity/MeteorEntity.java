package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.client.sound.MeteorClientSounds;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.worldgen.MeteoriteGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Boolean> SAFE =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.BOOLEAN);

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SAFE, false);
    }

    public boolean safe() {
        return this.entityData.get(SAFE);
    }

    public void setSafe(boolean safe) {
        this.entityData.set(SAFE, safe);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !HbmConfig.ENABLE_METEOR_STRIKES.get()) {
            discard();
            return;
        }

        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement().add(0.0D, -0.03D, 0.0D);
        if (motion.y < -2.5D) {
            motion = new Vec3(motion.x, -2.5D, motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        if (!level().isClientSide) {
            tickServerEffects(motion);
            tickServerImpact(motion);
        } else {
            tickClientEffects(motion);
        }
    }

    private void tickServerEffects(Vec3 motion) {
        if (!HbmConfig.ENABLE_METEOR_TAILS.get() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double x = getX() - motion.x;
        double y = getY() - motion.y;
        double z = getZ() - motion.z;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) > 350.0D * 350.0D) {
                continue;
            }
            serverLevel.sendParticles(player, HbmParticleTypes.METEOR_TAIL.get(), true, x, y, z, 10, 1.0D, 1.0D, 1.0D, 0.0D);
        }
    }

    private void tickServerImpact(Vec3 motion) {
        if (getY() >= 260.0D) {
            return;
        }
        clearMeteorPath(BlockPos.containing(getX(), getY(), getZ()));
        if (!onGround()) {
            return;
        }

        level().explode(this, getX(), getY(), getZ(), 5.0F + random.nextFloat(), !safe(), safe()
                ? Level.ExplosionInteraction.NONE
                : Level.ExplosionInteraction.BLOCK);

        int spawnX = (int) (Math.round(getX() - 0.5D) + (safe() ? 0 : motion.z * 4.0D));
        int spawnY = (int) Math.round(getY() - (safe() ? 0 : 4));
        int spawnZ = (int) (Math.round(getZ() - 0.5D) + (safe() ? 0 : motion.z * 4.0D));
        BlockPos center = new BlockPos(spawnX, spawnY, spawnZ);
        MeteoriteGenerator.generate(level(), random, center, safe(), true, true);
        clearMeteorPath(center);

        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(), SoundSource.HOSTILE, 10000.0F, 0.5F + random.nextFloat() * 0.1F);
        discard();
    }

    private void tickClientEffects(Vec3 motion) {
        MeteorClientSounds.tick(this);
    }

    private void clearMeteorPath(BlockPos center) {
        int radius = 5;
        int radiusSq = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    damageOrDestroyBlock(pos);
                }
            }
        }
    }

    private void damageOrDestroyBlock(BlockPos pos) {
        if (safe()) {
            return;
        }
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        float hardness = state.getDestroySpeed(level(), pos);
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || hardness >= 0.0F && hardness <= 0.3F) {
            level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (hardness < 0.0F || hardness > 5.0F) {
            return;
        }
        if (random.nextInt(6) != 1) {
            return;
        }
        if (state.is(Blocks.DIRT)) {
            level().setBlock(pos, HbmBlocks.DIRT_DEAD.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.SAND)) {
            level().setBlock(pos, random.nextBoolean() ? Blocks.SANDSTONE.defaultBlockState() : Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.STONE)) {
            level().setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            level().setBlock(pos, HbmBlocks.WASTE_EARTH.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("safe", safe());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSafe(tag.getBoolean("safe"));
    }
}
