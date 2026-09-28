package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.LambdaLaunchpadBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

/** Countdown siren and oxidizer venting from TileEntityLaunchpadLambda. */
public final class LambdaLaunchpadClientEffects {
    private static final Map<BlockPos, CountdownSound> SOUNDS = new HashMap<>();

    private LambdaLaunchpadClientEffects() {
    }

    public static void tick(LambdaLaunchpadBlockEntity launchpad) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != launchpad.getLevel()) return;
        BlockPos pos = launchpad.getBlockPos();
        CountdownSound sound = SOUNDS.get(pos);
        if (launchpad.countdown() <= 0) {
            if (sound != null) sound.finish();
        } else if (sound == null || sound.isStopped()
                || !client.getSoundManager().isActive(sound)) {
            sound = new CountdownSound(launchpad);
            SOUNDS.put(pos.immutable(), sound);
            client.getSoundManager().play(sound);
        }

        if (launchpad.erected() && launchpad.hasOxidizer()) {
            for (int i = 0; i < 3; i++) {
                client.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.getX() + 0.5D + client.level.random.nextGaussian() * 0.25D,
                        pos.getY() + 2.0D,
                        pos.getZ() + 0.5D + client.level.random.nextGaussian() * 0.25D,
                        (client.level.random.nextDouble() - 0.5D) * 0.075D,
                        0.02D,
                        (client.level.random.nextDouble() - 0.5D) * 0.075D);
            }
        }
    }

    private static final class CountdownSound extends AbstractTickableSoundInstance {
        private final LambdaLaunchpadBlockEntity launchpad;

        private CountdownSound(LambdaLaunchpadBlockEntity launchpad) {
            super(HbmSoundEvents.SIREN_REGULAR.get(), SoundSource.BLOCKS,
                    SoundInstance.createUnseededRandom());
            this.launchpad = launchpad;
            this.x = launchpad.getBlockPos().getX() + 0.5D;
            this.y = launchpad.getBlockPos().getY() + 3.0D;
            this.z = launchpad.getBlockPos().getZ() + 0.5D;
            this.looping = true;
            this.volume = 10.0F;
            this.pitch = 1.0F;
            this.attenuation = Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            Minecraft client = Minecraft.getInstance();
            if (client.level != this.launchpad.getLevel()
                    || this.launchpad.isRemoved()
                    || !client.level.hasChunkAt(this.launchpad.getBlockPos())
                    || client.level.getBlockEntity(this.launchpad.getBlockPos()) != this.launchpad
                    || this.launchpad.countdown() <= 0) {
                finish();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void finish() {
            stop();
            SOUNDS.remove(this.launchpad.getBlockPos(), this);
        }
    }
}
