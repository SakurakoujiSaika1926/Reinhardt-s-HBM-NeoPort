package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/** Dedicated falling Horizons payload (1.7.10 EntityTom). */
public final class LegacyTomEntity extends Entity {
    public LegacyTomEntity(EntityType<? extends LegacyTomEntity> type, Level level) {
        super(type, level); noCulling = true;
    }
    public static void spawn(ServerLevel level, int x, int z) {
        LegacyTomEntity tom = new LegacyTomEntity(com.reinhardt.hbm.registry.HbmEntityTypes.LEGACY_TOM.get(), level);
        tom.setPos(x + 0.5D, 600.0D, z + 0.5D); level.addFreshEntity(tom);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        setPos(getX(), getY() - 0.5D, getZ());
        if (tickCount % 100 == 0) {
            level().playSound(null, blockPosition(), HbmSoundEvents.TOM_CHIME.get(),
                    SoundSource.MASTER, 10000.0F, 1.0F);
        }
        if (!(level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        if (!level.getBlockState(pos).isAir() || getY() < 10.0D) {
            NukeExplosionManager.scheduleMk5NoRadiation(level, getX(), getY(), getZ(), 600);
            level.sendParticles(com.reinhardt.hbm.registry.HbmParticleTypes.LEGACY_CLOUD.get(), getX(), getY(), getZ(),
                    500, 12.0D, 12.0D, 12.0D, 0.5D);
            discard();
        }
    }
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 500000.0D; }
}
