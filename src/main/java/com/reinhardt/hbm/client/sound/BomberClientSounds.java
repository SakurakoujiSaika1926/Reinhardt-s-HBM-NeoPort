package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.entity.LegacyBomberEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

/** Matches EntityBomber's 250-block looping flyover audio in 1.7.10. */
public final class BomberClientSounds {
    private static final double AUDIBLE_DISTANCE = 250.0D;
    private static final int KEEP_ALIVE_TICKS = 10;
    private static final Map<Integer, FlyoverSound> SOUNDS = new HashMap<>();

    private BomberClientSounds() {
    }

    public static void tick(LegacyBomberEntity bomber) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || bomber.isRemoved() || !bomber.isFlying()
                || minecraft.player.distanceToSqr(bomber) >= AUDIBLE_DISTANCE * AUDIBLE_DISTANCE) {
            stop(bomber.getId());
            return;
        }
        FlyoverSound sound = SOUNDS.get(bomber.getId());
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new FlyoverSound(bomber);
            SOUNDS.put(bomber.getId(), sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void stop(int entityId) {
        FlyoverSound sound = SOUNDS.remove(entityId);
        if (sound != null) {
            sound.requestStop();
        }
    }

    private static final class FlyoverSound extends AbstractTickableSoundInstance {
        private final int entityId;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private FlyoverSound(LegacyBomberEntity bomber) {
            super(bomber.style() <= 4 ? HbmSoundEvents.ENTITY_BOMBER_SMALL_LOOP.get() : HbmSoundEvents.ENTITY_BOMBER_LOOP.get(),
                    SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.entityId = bomber.getId();
            this.x = bomber.getX();
            this.y = bomber.getY();
            this.z = bomber.getZ();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.volume = 0.0F;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null
                    || !(minecraft.level.getEntity(entityId) instanceof LegacyBomberEntity bomber) || bomber.isRemoved()
                    || !bomber.isFlying() || keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            x = bomber.getX();
            y = bomber.getY();
            z = bomber.getZ();
            double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
            volume = distance >= AUDIBLE_DISTANCE ? 0.0F : (float) (2.0D * (1.0D - distance / AUDIBLE_DISTANCE));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void keepAlive() {
            keepAliveTicks = KEEP_ALIVE_TICKS;
        }

        private void requestStop() {
            stop();
            SOUNDS.remove(entityId, this);
        }
    }
}
