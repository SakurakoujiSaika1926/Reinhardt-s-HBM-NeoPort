package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DigammaSpearEntity extends Entity {
    private static final EntityDataAccessor<Integer> TICKS_IN_GROUND =
            SynchedEntityData.defineId(DigammaSpearEntity.class, EntityDataSerializers.INT);

    public DigammaSpearEntity(EntityType<? extends DigammaSpearEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public DigammaSpearEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.DIGAMMA_SPEAR.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TICKS_IN_GROUND, 0);
    }

    public int ticksInGround() {
        return this.entityData.get(TICKS_IN_GROUND);
    }

    private void setTicksInGround(int ticks) {
        this.entityData.set(TICKS_IN_GROUND, ticks);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        setDeltaMovement(0.0D, -0.2D, 0.0D);

        BlockPos pos = blockPosition();
        if (level().getBlockState(pos.below()).isAir()) {
            move(MoverType.SELF, getDeltaMovement());
            if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
                strikeNearbySurface(serverLevel);
            }
            if (level().getBlockState(pos.below(3)).isAir()) {
                setTicksInGround(0);
            }
        } else {
            int ticks = ticksInGround() + 1;
            setTicksInGround(ticks);
            if (!level().isClientSide && ticks > 100) {
                detonate();
            }
        }
    }

    private void strikeNearbySurface(ServerLevel serverLevel) {
        double ix = getX() + random.nextGaussian() * 25.0D;
        double iz = getZ() + random.nextGaussian() * 25.0D;
        BlockPos surface = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(ix, getY(), iz)).above(2);
        double distance = new Vec3(ix - getX(), 0.0D, iz - getZ()).length();
        double chunkDose = distance < 20.0D ? 250.0D : 75.0D;
        ChunkRadiationData.get(serverLevel).incrementRadiation(surface, chunkDose, distance < 20.0D ? 50_000.0D : 15_000.0D);
        for (Player player : serverLevel.players()) {
            contaminate(player, 0.05F, false);
            HbmAdvancements.award(player, "digamma_kauai_moho");
        }
    }

    private void detonate() {
        Level level = level();
        AABB area = getBoundingBox().inflate(256.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            contaminate(living, 10.0F, true);
            living.hurt(living.damageSources().source(HbmDamageTypes.DIGAMMA, this, this), 2.0F);
        }
        level.playSound(null, getX(), getY(), getZ(), HbmSoundEvents.DIGAMMA_FLASH.get(), SoundSource.PLAYERS, 25000.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(blockPosition(), 5000.0D, 250_000.0D);
        }
        discard();
    }

    private static void contaminate(LivingEntity living, float amount, boolean lethal) {
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        data.addDigamma(amount);
        if (lethal) {
            data.addEnvironmentRadiation(100.0F);
        }
        HbmLivingRadiation.set(living, data);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ticks_in_ground", ticksInGround());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setTicksInGround(tag.getInt("ticks_in_ground"));
    }
}
