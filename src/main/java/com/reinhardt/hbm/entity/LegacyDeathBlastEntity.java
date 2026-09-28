package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Sixty-tick visible charge stage used by the orbital death ray. */
public final class LegacyDeathBlastEntity extends Entity {
    public static final int MAX_AGE = 60;
    private static final int BOLT_COUNT = 100;
    private static final int MK5_STRENGTH = 40;

    public LegacyDeathBlastEntity(EntityType<? extends LegacyDeathBlastEntity> type, Level level) {
        super(type, level);
    }

    public LegacyDeathBlastEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.LEGACY_DEATH_BLAST.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= MAX_AGE) {
            discard();
            detonate((ServerLevel) level());
        }
    }

    private void detonate(ServerLevel level) {
        // EntityNukeExplosionMK5.statFacNoRad(world, 40): strength 40,
        // length 20, no fallout, no Torex and no generic MK5 start effects.
        NukeExplosionManager.scheduleDeathBlastMk5NoRadiation(
                level, getX(), getY(), getZ(), MK5_STRENGTH);

        for (int index = 0; index < BOLT_COUNT; index++) {
            double angle = Math.PI * 2.0D * index / BOLT_COUNT;
            Vec3 motion = new Vec3(
                    Math.cos(angle) * 0.2D,
                    -0.01D,
                    Math.sin(angle) * 0.2D);
            level.addFreshEntity(new LegacyDeathRayBoltEntity(
                    level, getX(), getY() + 2.0D, getZ(), motion));
        }

        Vec3 center = position();
        level.playSound(null, center.x, center.y, center.z,
                HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(),
                SoundSource.BLOCKS, 25.0F, 0.9F);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) > 250.0D * 250.0D) {
                continue;
            }
            level.sendParticles(player, HbmParticleTypes.MUKE_FLASH.get(), true,
                    center.x, center.y + 0.5D, center.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, HbmParticleTypes.MUKE_WAVE.get(), true,
                    center.x, center.y + 0.5D, center.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }
}
