package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class GeothermalHeatExchangerClientSounds {
    private static final double START_DISTANCE = 16.0D;
    private static final double AUDIBLE_DISTANCE = 10.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, WorkingSound> WORKING_SOUNDS = new HashMap<>();

    private GeothermalHeatExchangerClientSounds() {
    }

    public static void tick(GeothermalHeatExchangerBlockEntity exchanger) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || exchanger.bufferedHeat() <= 0) {
            stop(exchanger.getBlockPos());
            return;
        }
        BlockPos pos = exchanger.getBlockPos().immutable();
        double distanceSqr = minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 5.0D, pos.getZ() + 0.5D);
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
        if (sound != null) sound.requestStop();
    }

    private static void forget(BlockPos pos, WorkingSound sound) {
        WORKING_SOUNDS.remove(pos, sound);
    }

    private static final class WorkingSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private WorkingSound(BlockPos pos) {
            super(HbmSoundEvents.HEPHAESTUS_RUNNING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 5.0D;
            this.z = pos.getZ() + 0.5D;
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
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
            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (!(blockEntity instanceof GeothermalHeatExchangerBlockEntity exchanger)
                    || exchanger.bufferedHeat() <= 0 || keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
            volume = distance >= AUDIBLE_DISTANCE ? 0.0F : (float) (0.75D * (1.0D - distance / AUDIBLE_DISTANCE));
            pitch = 1.0F;
        }

        @Override public boolean canStartSilent() { return true; }
        private void keepAlive() { keepAliveTicks = KEEP_ALIVE_TICKS; }
        private void requestStop() { stop(); forget(pos, this); }
    }
}
