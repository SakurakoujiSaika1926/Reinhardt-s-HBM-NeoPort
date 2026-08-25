package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/** Client loop copied from TileEntityMachineAutosaw#createAudioLoop. */
public final class AutosawClientSounds {
    private static final double START_DISTANCE = 15.0D;
    private static final double AUDIBLE_DISTANCE = 10.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, WorkingSound> WORKING_SOUNDS = new HashMap<>();

    private AutosawClientSounds() {
    }

    public static void tick(LegacyMachineBlockEntity autosaw) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !autosaw.autosawOn() || autosaw.autosawSuspended()) {
            stop(autosaw.getBlockPos());
            return;
        }
        BlockPos pos = autosaw.getBlockPos().immutable();
        if (minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >= START_DISTANCE * START_DISTANCE) {
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
            super(HbmSoundEvents.ENGINE_LOOP.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos.immutable();
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.pitch = 1.0F + SoundInstance.createUnseededRandom().nextFloat() * 0.1F;
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
            if (!(blockEntity instanceof LegacyMachineBlockEntity autosaw)
                    || !autosaw.machineId().equals("machine_autosaw")
                    || !autosaw.autosawOn() || autosaw.autosawSuspended()) {
                requestStop();
                return;
            }
            if (this.keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = distance >= AUDIBLE_DISTANCE ? 0.0F : (float) (1.0D - distance / AUDIBLE_DISTANCE);
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
