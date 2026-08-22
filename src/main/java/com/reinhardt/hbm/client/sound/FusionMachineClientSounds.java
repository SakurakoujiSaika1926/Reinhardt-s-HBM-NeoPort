package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
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

public final class FusionMachineClientSounds {
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, WorkingSound> WORKING_SOUNDS = new HashMap<>();

    private FusionMachineClientSounds() {
    }

    public static void tick(FusionMachineBlockEntity machine) {
        FusionMachineBlock.Kind kind = machine.kind();
        if (kind != FusionMachineBlock.Kind.TORUS
                && kind != FusionMachineBlock.Kind.KLYSTRON
                && kind != FusionMachineBlock.Kind.KLYSTRON_CREATIVE
                && kind != FusionMachineBlock.Kind.MHDT) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = machine.getBlockPos().immutable();
        if (minecraft.level == null || minecraft.player == null || machine.rotorSpeed() <= 0.0F) {
            stop(pos);
            return;
        }

        double audibleDistance = audibleDistance(kind);
        double y = soundY(kind, pos);
        double distanceSqr = minecraft.player.distanceToSqr(pos.getX() + 0.5D, y, pos.getZ() + 0.5D);
        if (distanceSqr >= audibleDistance * audibleDistance) {
            stop(pos);
            return;
        }

        WorkingSound sound = WORKING_SOUNDS.get(pos);
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound) || sound.kind != kind) {
            if (sound != null) {
                sound.requestStop();
            }
            sound = new WorkingSound(pos, kind);
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

    private static SoundEvent sound(FusionMachineBlock.Kind kind) {
        return switch (kind) {
            case TORUS -> HbmSoundEvents.FUSION_REACTOR_RUNNING.get();
            case MHDT -> HbmSoundEvents.LARGE_TURBINE_RUNNING.get();
            default -> HbmSoundEvents.FEL_LOOP.get();
        };
    }

    private static double audibleDistance(FusionMachineBlock.Kind kind) {
        return kind == FusionMachineBlock.Kind.TORUS ? 50.0D : 30.0D;
    }

    private static double soundY(FusionMachineBlock.Kind kind, BlockPos pos) {
        return pos.getY() + (kind == FusionMachineBlock.Kind.MHDT ? 1.5D : 2.5D);
    }

    private static float speedScale(FusionMachineBlock.Kind kind) {
        return switch (kind) {
            case TORUS -> 30.0F;
            case MHDT -> 15.0F;
            default -> 5.0F;
        };
    }

    private static final class WorkingSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private final FusionMachineBlock.Kind kind;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private WorkingSound(BlockPos pos, FusionMachineBlock.Kind kind) {
            super(sound(kind), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.kind = kind;
            this.x = pos.getX() + 0.5D;
            this.y = soundY(kind, pos);
            this.z = pos.getZ() + 0.5D;
            this.looping = true;
            this.delay = 0;
            this.pitch = 0.01F;
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
            BlockEntity blockEntity = minecraft.level.getBlockEntity(this.pos);
            if (!(blockEntity instanceof FusionMachineBlockEntity machine) || machine.kind() != this.kind || machine.rotorSpeed() <= 0.0F) {
                requestStop();
                return;
            }
            if (this.keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }

            float speed = Math.max(0.0F, Math.min(1.0F, machine.rotorSpeed() / speedScale(this.kind)));
            this.volume = speed;
            this.pitch = Math.max(0.01F, speed);
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
