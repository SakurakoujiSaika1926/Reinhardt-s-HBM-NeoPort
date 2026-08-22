package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.entity.MeteorEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class MeteorClientSounds {
    private static final double START_DISTANCE = 210.0D;
    private static final int KEEP_ALIVE_TICKS = 10;
    private static final Map<Integer, FallingSound> FALLING_SOUNDS = new HashMap<>();

    private MeteorClientSounds() {
    }

    public static void tick(MeteorEntity meteor) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || meteor.isRemoved()) {
            stop(meteor.getId());
            return;
        }

        double distanceSqr = minecraft.player.distanceToSqr(meteor.getX(), meteor.getY(), meteor.getZ());
        if (distanceSqr >= START_DISTANCE * START_DISTANCE) {
            stop(meteor.getId());
            return;
        }

        FallingSound sound = FALLING_SOUNDS.get(meteor.getId());
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new FallingSound(meteor);
            FALLING_SOUNDS.put(meteor.getId(), sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void stop(int entityId) {
        FallingSound sound = FALLING_SOUNDS.remove(entityId);
        if (sound != null) {
            sound.requestStop();
        }
    }

    private static void forget(int entityId, FallingSound sound) {
        FALLING_SOUNDS.remove(entityId, sound);
    }

    private static final class FallingSound extends AbstractTickableSoundInstance {
        private final int entityId;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private FallingSound(MeteorEntity meteor) {
            super(HbmSoundEvents.METEORITE_FALLING_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.entityId = meteor.getId();
            this.x = meteor.getX();
            this.y = meteor.getY() + meteor.getBbHeight() * 0.5D;
            this.z = meteor.getZ();
            this.looping = true;
            this.delay = 0;
            this.pitch = 0.95F + this.random.nextFloat() * 0.1F;
            this.volume = 0.0F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                requestStop();
                return;
            }

            Entity entity = minecraft.level.getEntity(this.entityId);
            if (!(entity instanceof MeteorEntity meteor) || meteor.isRemoved()) {
                requestStop();
                return;
            }

            if (this.keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }

            this.x = meteor.getX();
            this.y = meteor.getY() + meteor.getBbHeight() * 0.5D;
            this.z = meteor.getZ();
            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = distance >= START_DISTANCE ? 0.0F : 1.0F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void keepAlive() {
            this.keepAliveTicks = KEEP_ALIVE_TICKS;
        }

        private void requestStop() {
            stop();
            forget(this.entityId, this);
        }
    }
}
