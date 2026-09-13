package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.network.TurretCasingEffectPayload;
import com.reinhardt.hbm.network.TurretMuzzleFlashPayload;
import com.reinhardt.hbm.network.MaxwellGibEffectPayload;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TurretCasingEffects {
    enum CasingKind {
        FRIENDLY,
        BMG50,
        P9,
        SHELL_240,
        ARTY_16,
        ARTY_16_PHOS,
        ARTY_16_NUKE,
        HOWARD,
        FRIENDLY_STEEL,
        BMG50_STEEL,
        P9_STEEL
    }

    private TurretCasingEffects() {
    }

    static void spawnEjectorCasing(Level level, Vec3 position, double yaw, double pitch, CasingKind kind,
                                   double motionX, double motionY, double motionZ,
                                   double ignoredYawFactor, double pitchFactor, int count) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        RandomSource random = serverLevel.random;
        int amount = Math.max(1, count);
        for (int i = 0; i < amount; i++) {
            double ejectionPitch = -pitch;
            double variedPitch = ejectionPitch + random.nextGaussian() * pitchFactor;
            double variedYaw = yaw + random.nextGaussian() * pitchFactor;
            Vec3 jittered = new Vec3(
                    motionX + random.nextGaussian() * pitchFactor,
                    motionY + random.nextGaussian() * pitchFactor,
                    motionZ + random.nextGaussian() * pitchFactor
            );
            Vec3 motion = rotateY(rotateX(jittered, variedPitch), -variedYaw);
            sendCasing(serverLevel, position, motion, ejectionPitch, yaw,
                    (float) (random.nextGaussian() * 5.0D),
                    (float) (random.nextGaussian() * 10.0D), kind,
                    false, 0, 0.0D, 0);
        }
    }

    static void spawnDirectCasing(Level level, Vec3 position, double yaw, double pitch, CasingKind kind,
                                  double frontMotion, double heightMotion, double sideMotion,
                                  double motionVariance, float momentumPitch, float momentumYaw,
                                  int smokeLife, double smokeLift, int smokeNodeLife) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 localMotion = new Vec3(sideMotion, heightMotion, frontMotion);
        Vec3 rotatedMotion = rotateY(rotateX(localMotion, pitch), -yaw);
        Vec3 motion = rotatedMotion.add(
                serverLevel.random.nextGaussian() * motionVariance,
                serverLevel.random.nextGaussian() * motionVariance,
                serverLevel.random.nextGaussian() * motionVariance
        );
        sendCasing(serverLevel, position, motion, -pitch, yaw, momentumPitch, momentumYaw, kind,
                true, smokeLife, smokeLift, smokeNodeLife);
    }

    private static void sendCasing(ServerLevel level, Vec3 position, Vec3 motion,
                                   double rotationPitch, double rotationYaw,
                                   float momentumPitch, float momentumYaw, CasingKind kind,
                                   boolean smoking, int smokeLife, double smokeLift, int smokeNodeLife) {
        TurretCasingEffectPayload payload = new TurretCasingEffectPayload(
                position.x, position.y, position.z,
                motion.x, motion.y, motion.z,
                (float) Math.toDegrees(rotationPitch),
                (float) Math.toDegrees(rotationYaw),
                momentumPitch, momentumYaw, kind.ordinal(),
                smoking, smokeLife, smokeLift, smokeNodeLife
        );
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(position) <= 2_500.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private static Vec3 rotateX(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x, vector.y * cos - vector.z * sin, vector.y * sin + vector.z * cos);
    }

    private static Vec3 rotateY(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y, vector.z * cos - vector.x * sin);
    }

    static Vec3 localVector(double x, double y, double z, double yawRadians, double pitchRadians) {
        double cosPitch = Math.cos(-pitchRadians);
        double sinPitch = Math.sin(-pitchRadians);
        double py = y * cosPitch - x * sinPitch;
        double px = y * sinPitch + x * cosPitch;
        double yaw = -(yawRadians + Math.PI * 0.5D);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double rx = px * cosYaw + z * sinYaw;
        double rz = z * cosYaw - px * sinYaw;
        return new Vec3(rx, py, rz);
    }

    static void spawnMuzzleFlash(Level level, Vec3 position, float size, int count, double range) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        TurretMuzzleFlashPayload payload = new TurretMuzzleFlashPayload(
                position.x, position.y, position.z, size, Math.max(0, count)
        );
        double rangeSquared = range * range;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(position) <= rangeSquared) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void spawnMaxwellGib(Level level, LivingEntity target, boolean screm) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5D;
        double z = target.getZ();
        MaxwellGibEffectPayload payload = new MaxwellGibEffectPayload(
                target.getId(), x, y, z, target.getBbWidth(), target.getBbHeight(), 0
        );
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) <= 22_500.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
        if (screm) {
            level.playSound(null, target.blockPosition(), HbmSoundEvents.BLOCK_SCREM.get(), SoundSource.BLOCKS, 20.0F, 1.0F);
        } else {
            level.playSound(null, target.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.95F + level.random.nextFloat() * 0.2F);
        }
    }
}
