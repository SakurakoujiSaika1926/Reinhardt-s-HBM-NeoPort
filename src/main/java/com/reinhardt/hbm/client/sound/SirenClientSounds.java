package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.item.SirenTrackItem;
import com.reinhardt.hbm.network.SirenSoundPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class SirenClientSounds {
    private static final Map<Long, SirenSound> ACTIVE = new HashMap<>();

    private SirenClientSounds() {
    }

    public static void accept(SirenSoundPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long key = payload.pos().asLong();
        SirenSound current = ACTIVE.get(key);
        if (!payload.active()) {
            if (current != null) {
                current.requestStop();
                ACTIVE.remove(key);
            }
            return;
        }

        SirenTrackItem.Track track = SirenTrackItem.Track.fromDamage(payload.trackId());
        if (!track.valid()) {
            return;
        }
        if (current != null && current.trackId() == payload.trackId() && !current.isStopped()) {
            return;
        }
        if (current != null) {
            current.requestStop();
        }
        SirenSound sound = new SirenSound(payload.pos(), payload.trackId(), track);
        ACTIVE.put(key, sound);
        minecraft.getSoundManager().play(sound);
    }

    private static final class SirenSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private final int id;
        private final float maxDistance;

        private SirenSound(BlockPos pos, int id, SirenTrackItem.Track track) {
            super(track.sound(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.id = id;
            this.maxDistance = Math.max(1.0F, track.range());
            this.looping = track.playback() == SirenTrackItem.Playback.LOOP;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.0F;
            updatePosition();
        }

        private int trackId() {
            return this.id;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null || minecraft.level.getBlockEntity(this.pos) == null) {
                requestStop();
                return;
            }
            updatePosition();
            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = Math.max(0.0F, 2.0F * (1.0F - (float) distance / this.maxDistance));
        }

        private void updatePosition() {
            this.x = this.pos.getX() + 0.5D;
            this.y = this.pos.getY() + 0.5D;
            this.z = this.pos.getZ() + 0.5D;
        }

        private void requestStop() {
            stop();
            ACTIVE.remove(this.pos.asLong(), this);
        }
    }
}
