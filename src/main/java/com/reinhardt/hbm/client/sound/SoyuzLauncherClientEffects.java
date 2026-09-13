package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.entity.SoyuzEntity;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** TileEntitySoyuzLauncher client countdown audio and ClientProxy shockRand smoke. */
public final class SoyuzLauncherClientEffects {
    private static final Map<BlockPos, ReadySound> SOUNDS = new HashMap<>();

    private SoyuzLauncherClientEffects() {}

    public static void tick(SoyuzLauncherBlockEntity launcher) {
        Minecraft client = Minecraft.getInstance();
        BlockPos pos = launcher.getBlockPos();
        if (client.level != launcher.getLevel()) return;
        boolean running = launcher.renderStarting() && launcher.canLaunch() && launcher.renderCountdown() > 0;
        ReadySound sound = SOUNDS.get(pos);
        if (!running) {
            if (sound != null) sound.finish();
        } else if (sound == null || sound.isStopped() || !client.getSoundManager().isActive(sound)) {
            sound = new ReadySound(launcher);
            SOUNDS.put(pos.immutable(), sound);
            client.getSoundManager().play(sound);
        }
        if (!client.level.getEntitiesOfClass(SoyuzEntity.class,
                new AABB(pos.getX() - 0.5D, pos.getY(), pos.getZ() - 0.5D,
                        pos.getX() + 1.5D, pos.getY() + 10.0D, pos.getZ() + 1.5D)).isEmpty()) {
            double strength = client.level.random.nextGaussian() * 3.0D + 6.0D;
            // The old Vec3 rotation API takes radians; preserve the source's integer arguments.
            Vec3 vector = new Vec3(strength, 0, 0).yRot(client.level.random.nextInt(360));
            for (int i = 0; i < 50; i++) {
                double factor = client.level.random.nextDouble();
                client.level.addParticle(HbmParticleTypes.LAUNCHER_EX_SMOKE.get(),
                        pos.getX() + 0.5D, pos.getY() - 3.0D, pos.getZ() + 0.5D,
                        vector.x * factor, 0, vector.z * factor);
                vector = vector.yRot(360 / 50);
            }
        }
    }

    private static final class ReadySound extends AbstractTickableSoundInstance {
        private final SoyuzLauncherBlockEntity launcher;

        private ReadySound(SoyuzLauncherBlockEntity launcher) {
            super(HbmSoundEvents.SOYUZ_READY.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.launcher = launcher;
            this.x = launcher.getBlockPos().getX();
            this.y = launcher.getBlockPos().getY();
            this.z = launcher.getBlockPos().getZ();
            this.looping = true;
            this.pitch = 1.0F;
            this.volume = 100.0F;
            this.attenuation = Attenuation.NONE;
        }

        @Override public void tick() {
            Minecraft client = Minecraft.getInstance();
            if (client.level != launcher.getLevel() || client.player == null || launcher.isRemoved()
                    || !client.level.hasChunkAt(launcher.getBlockPos())
                    || client.level.getBlockEntity(launcher.getBlockPos()) != launcher
                    || !launcher.renderStarting() || !launcher.canLaunch()) {
                finish();
                return;
            }
            this.volume = Math.max(0.0F, 100.0F - (float) Math.sqrt(client.player.distanceToSqr(x, y, z)));
        }

        @Override public boolean canStartSilent() { return true; }
        private void finish() { stop(); SOUNDS.remove(launcher.getBlockPos(), this); }
    }
}
