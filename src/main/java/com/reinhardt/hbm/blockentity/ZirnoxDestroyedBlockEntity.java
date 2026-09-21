package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.radiation.RadiationShielding;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ZirnoxDestroyedBlockEntity extends BlockEntity {
    private static final double RADIATION_RANGE = 100.0D;
    private static final float BURNING_SOURCE = 500_000.0F;
    private static final float COLD_SOURCE = 75_000.0F;

    private boolean onFire = true;

    public ZirnoxDestroyedBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ZIRNOX_DESTROYED.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ZirnoxDestroyedBlockEntity destroyed) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        destroyed.radiate(serverLevel);
        if (destroyed.onFire && serverLevel.random.nextInt(5_000) == 0) {
            destroyed.extinguish();
        }
        if (destroyed.onFire && serverLevel.getGameTime() % 50L == 0L) {
            serverLevel.sendParticles(HbmParticleTypes.RBMK_FIRE.get(),
                    pos.getX() + 0.25D + serverLevel.random.nextDouble() * 0.5D,
                    pos.getY() + 1.75D,
                    pos.getZ() + 0.25D + serverLevel.random.nextDouble() * 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0F + serverLevel.random.nextFloat(),
                    serverLevel.random.nextFloat() * 0.7F + 0.3F);
        }
    }

    public void extinguish() {
        if (!this.onFire) {
            return;
        }
        this.onFire = false;
        setChanged();
    }

    public boolean isOnFire() {
        return this.onFire;
    }

    private void radiate(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        float source = this.onFire ? BURNING_SOURCE : COLD_SOURCE;
        AABB area = new AABB(this.worldPosition).inflate(RADIATION_RANGE);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            Vec3 eye = entity.position().add(0.0D, entity.getEyeHeight(), 0.0D);
            double distance = Math.max(1.0D, eye.distanceTo(center));
            float dose = (float) (source / (distance * distance));
            dose = RadiationShielding.attenuateDirectDose(level, center, eye, dose);
            if (dose <= 0.0F) {
                continue;
            }
            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addEnvironmentRadiation(dose);
            if (!(entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                radiation.addRadiation((float) (dose * HbmArmorProtection.radiationMultiplier(entity)));
            }
            HbmLivingRadiation.set(entity, radiation);
            if (this.onFire && distance < 5.0D) {
                entity.hurt(level.damageSources().onFire(), 2.0F);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("OnFire", this.onFire);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.onFire = !tag.contains("OnFire") || tag.getBoolean("OnFire");
    }
}
