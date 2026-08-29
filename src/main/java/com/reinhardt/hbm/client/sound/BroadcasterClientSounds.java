package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.BroadcasterBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Client-local equivalent of TileEntityBroadcaster's AudioWrapper loop. */
public final class BroadcasterClientSounds {
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final double RANGE = 25.0D;
    private static final Map<BlockPos, BroadcastSound> SOUNDS = new HashMap<>();

    private BroadcasterClientSounds() {
    }

    public static void tick(BroadcasterBlockEntity broadcaster) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stop(broadcaster.getBlockPos());
            return;
        }
        BlockPos pos = broadcaster.getBlockPos().immutable();
        if (minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > RANGE * RANGE) {
            stop(pos);
            return;
        }
        BroadcastSound sound = SOUNDS.get(pos);
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new BroadcastSound(pos);
            SOUNDS.put(pos, sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void stop(BlockPos pos) {
        BroadcastSound sound = SOUNDS.remove(pos.immutable());
        if (sound != null) {
            sound.requestStop();
        }
    }

    private static final class BroadcastSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private int keepAlive = KEEP_ALIVE_TICKS;

        private BroadcastSound(BlockPos pos) {
            super(soundFor(pos), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                requestStop();
                return;
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (!(blockEntity instanceof BroadcasterBlockEntity) || keepAlive-- <= 0) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
            // AudioDynamic.func(dist), with the original maxVolume=25 and range=25.
            volume = distance >= RANGE ? 0.0F : (float) ((RANGE - distance) / RANGE * 25.0D);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void keepAlive() {
            keepAlive = KEEP_ALIVE_TICKS;
        }

        private void requestStop() {
            stop();
            SOUNDS.remove(pos, this);
        }
    }

    private static SoundEvent soundFor(BlockPos pos) {
        return switch (new Random((long) pos.getX() + pos.getY() + pos.getZ()).nextInt(3)) {
            case 1 -> HbmSoundEvents.BROADCAST_2.get();
            case 2 -> HbmSoundEvents.BROADCAST_3.get();
            default -> HbmSoundEvents.BROADCAST_1.get();
        };
    }
}
