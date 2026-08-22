package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.ArcFurnaceBlockEntity;
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

public final class ArcFurnaceClientSounds {
    private static final double START_DISTANCE = 50.0D;
    private static final double AUDIBLE_DISTANCE = 15.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, MachineLoopSound> LID_SOUNDS = new HashMap<>();
    private static final Map<BlockPos, MachineLoopSound> WORKING_SOUNDS = new HashMap<>();

    private ArcFurnaceClientSounds() {
    }

    public static void tick(ArcFurnaceBlockEntity furnace, float previousLid, float lid) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = furnace.getBlockPos().immutable();
        if (minecraft.level == null || minecraft.player == null
                || minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >= START_DISTANCE * START_DISTANCE) {
            stop(LID_SOUNDS, pos);
            stop(WORKING_SOUNDS, pos);
            return;
        }

        boolean moving = Math.abs(lid - previousLid) > 1.0E-5F;
        updateLoop(LID_SOUNDS, pos, moving, HbmSoundEvents.WGH_START.get(), 0.75F, 1.0F);
        updateLoop(WORKING_SOUNDS, pos, furnace.working(), HbmSoundEvents.ARC_FURNACE_OPERATE.get(), 1.5F, 0.75F);

        if (moving && (lid == 0.0F || lid == 1.0F) && !(previousLid == 0.0F && lid == 1.0F)) {
            minecraft.level.playLocalSound(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    HbmSoundEvents.WGH_STOP.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false
            );
        }
    }

    private static void updateLoop(Map<BlockPos, MachineLoopSound> sounds, BlockPos pos, boolean active,
                                   SoundEvent event, float volume, float pitch) {
        if (!active) {
            stop(sounds, pos);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        MachineLoopSound sound = sounds.get(pos);
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new MachineLoopSound(sounds, pos, event, volume, pitch);
            sounds.put(pos, sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void stop(Map<BlockPos, MachineLoopSound> sounds, BlockPos pos) {
        MachineLoopSound sound = sounds.remove(pos);
        if (sound != null) sound.requestStop();
    }

    private static final class MachineLoopSound extends AbstractTickableSoundInstance {
        private final Map<BlockPos, MachineLoopSound> owner;
        private final BlockPos pos;
        private final float baseVolume;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private MachineLoopSound(Map<BlockPos, MachineLoopSound> owner, BlockPos pos, SoundEvent event,
                                 float baseVolume, float pitch) {
            super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.owner = owner;
            this.pos = pos;
            this.baseVolume = baseVolume;
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
            this.looping = true;
            this.delay = 0;
            this.pitch = pitch;
            this.volume = 0.0F;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null || keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (!(blockEntity instanceof ArcFurnaceBlockEntity)) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
            this.volume = distance >= AUDIBLE_DISTANCE
                    ? 0.0F
                    : (float) (baseVolume * (1.0D - distance / AUDIBLE_DISTANCE));
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
            owner.remove(pos, this);
        }
    }
}
