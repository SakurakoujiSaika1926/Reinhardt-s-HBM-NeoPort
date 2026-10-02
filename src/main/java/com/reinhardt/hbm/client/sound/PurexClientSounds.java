package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/** Client lifecycle for TileEntityMachinePUREX's CHEMPLANT_LOOP sound. */
public final class PurexClientSounds {
    private static final double START_DISTANCE = 25.0D;
    private static final double AUDIBLE_DISTANCE = 15.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, WorkingSound> WORKING_SOUNDS = new HashMap<>();

    private PurexClientSounds() {
    }

    public static void tick(PurexBlockEntity purex) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = purex.getBlockPos().immutable();
        if (minecraft.level == null || minecraft.player == null || !purex.isClientWorking()
                || minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >= START_DISTANCE * START_DISTANCE) {
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
        WorkingSound sound = WORKING_SOUNDS.remove(pos);
        if (sound != null) {
            sound.requestStop();
        }
    }

    private static final class WorkingSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private WorkingSound(BlockPos pos) {
            super(HbmSoundEvents.CHEMICAL_PLANT_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.looping = true;
            this.delay = 0;
            this.pitch = 0.75F;
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
            if (!(blockEntity instanceof PurexBlockEntity purex) || !purex.isClientWorking()
                    || this.keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = distance >= AUDIBLE_DISTANCE ? 0.0F : (float) (1.0D - distance / AUDIBLE_DISTANCE);
            this.volume = com.reinhardt.hbm.util.MachineMuffler.attenuate(blockEntity, this.volume);
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
            WORKING_SOUNDS.remove(this.pos, this);
        }
    }
}
