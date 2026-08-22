package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.VacuumDistillBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class VacuumDistillClientSounds {
    private static final double START_DISTANCE = 50.0D;
    private static final double AUDIBLE_DISTANCE = 15.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, WorkingSound> WORKING_SOUNDS = new HashMap<>();

    private VacuumDistillClientSounds() {
    }

    public static void tick(VacuumDistillBlockEntity refinery) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !refinery.isWorking()) {
            stop(refinery.getBlockPos());
            return;
        }

        BlockPos pos = refinery.getBlockPos().immutable();
        double distanceSqr = minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
        if (distanceSqr >= START_DISTANCE * START_DISTANCE) {
            stop(pos);
            return;
        }

        WorkingSound sound = WORKING_SOUNDS.get(pos);
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new WorkingSound(pos);
            WORKING_SOUNDS.put(pos, sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void stop(BlockPos pos) {
        WorkingSound sound = WORKING_SOUNDS.remove(pos.immutable());
        if (sound != null) {
            sound.requestStop();
        }
    }

    private static void forget(BlockPos pos, WorkingSound sound) {
        WORKING_SOUNDS.remove(pos, sound);
    }

    private static final class WorkingSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private WorkingSound(BlockPos pos) {
            super(HbmSoundEvents.BOILER.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.volume = 0.0F;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                requestStop();
                return;
            }

            BlockEntity blockEntity = minecraft.level.getBlockEntity(this.pos);
            if (!(blockEntity instanceof VacuumDistillBlockEntity refinery) || !refinery.isWorking()) {
                requestStop();
                return;
            }

            if (this.keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }

            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = distance >= AUDIBLE_DISTANCE ? 0.0F : (float) (0.25D * (1.0D - distance / AUDIBLE_DISTANCE));
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
            forget(this.pos, this);
        }
    }
}

