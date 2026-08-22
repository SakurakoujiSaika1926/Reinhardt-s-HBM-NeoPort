package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class IndustrialTurbineClientSounds {
    private static final double START_DISTANCE = 35.0D;
    private static final double AUDIBLE_DISTANCE = 35.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, WorkingSound> WORKING_SOUNDS = new HashMap<>();

    private IndustrialTurbineClientSounds() {
    }

    public static void tick(IndustrialTurbineBlockEntity turbine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || turbine.spin() <= 0.0D) {
            stop(turbine.getBlockPos());
            return;
        }

        BlockPos pos = turbine.getBlockPos().immutable();
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
        private final float audioDesync;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private WorkingSound(BlockPos pos) {
            super(HbmSoundEvents.LARGE_TURBINE_RUNNING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.audioDesync = (Math.floorMod(pos.hashCode(), 50) / 1000.0F);
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
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
            if (!(blockEntity instanceof IndustrialTurbineBlockEntity turbine) || turbine.spin() <= 0.0D) {
                requestStop();
                return;
            }

            if (this.keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }

            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            float spinNum = (float) Math.min(1.0D, turbine.spin() * 2.0D);
            float baseVolume = 0.25F + spinNum * 0.75F;
            this.volume = distance >= AUDIBLE_DISTANCE
                    ? 0.0F
                    : (float) (baseVolume * (1.0D - distance / AUDIBLE_DISTANCE));
            this.pitch = 0.5F + spinNum * 0.5F + this.audioDesync;
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
