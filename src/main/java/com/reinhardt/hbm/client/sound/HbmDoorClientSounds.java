package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.door.HbmDoorDecl;
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

/** Client equivalent of TileEntityDoorGeneric.handleNewState(). */
public final class HbmDoorClientSounds {
    // TileEntityDoorGeneric packets were sent to clients within 250 blocks;
    // retain that radius so a client entering the 10-block audible range
    // during a transition still receives the same sound state.
    private static final double START_DISTANCE = 250.0D;
    private static final double AUDIBLE_DISTANCE = 10.0D;
    private static final int KEEP_ALIVE_TICKS = 20;
    private static final Map<BlockPos, Byte> LAST_STATES = new HashMap<>();
    private static final Map<BlockPos, DoorLoopSound> PRIMARY = new HashMap<>();
    private static final Map<BlockPos, DoorLoopSound> SECONDARY = new HashMap<>();

    private HbmDoorClientSounds() {
    }

    public static void tick(HbmHeavyDoorBlockEntity door) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        BlockPos pos = door.getBlockPos().immutable();
        if (minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >= START_DISTANCE * START_DISTANCE) {
            stop(PRIMARY, pos);
            stop(SECONDARY, pos);
            LAST_STATES.remove(pos);
            return;
        }

        byte current = door.state();
        Byte previous = LAST_STATES.put(pos, current);
        if (previous == null) {
            return;
        }
        if (previous == current) {
            keepAlive(PRIMARY, pos);
            keepAlive(SECONDARY, pos);
            return;
        }

        HbmDoorDecl decl = ((HbmHeavyDoorBlock) door.getBlockState().getBlock()).decl();
        Profile profile = Profile.forDoor(decl);
        if (previous == HbmHeavyDoorBlockEntity.STATE_CLOSED && current == HbmHeavyDoorBlockEntity.STATE_OPENING) {
            start(PRIMARY, pos, profile.openLoop(), profile.volume());
            start(SECONDARY, pos, profile.secondaryLoop(), profile.volume());
            play(minecraft, pos, profile.openStart(), profile.volume());
        } else if (previous == HbmHeavyDoorBlockEntity.STATE_OPEN && current == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            stop(PRIMARY, pos);
            start(PRIMARY, pos, profile.closeLoop(), profile.volume());
            start(SECONDARY, pos, profile.secondaryLoop(), profile.volume());
            play(minecraft, pos, profile.closeStart(), profile.volume());
        }

        if (current == HbmHeavyDoorBlockEntity.STATE_OPEN || current == HbmHeavyDoorBlockEntity.STATE_CLOSED) {
            stop(PRIMARY, pos);
            stop(SECONDARY, pos);
            if (previous == HbmHeavyDoorBlockEntity.STATE_OPENING && current == HbmHeavyDoorBlockEntity.STATE_OPEN) {
                play(minecraft, pos, profile.openEnd(), profile.volume());
            } else if (previous == HbmHeavyDoorBlockEntity.STATE_CLOSING && current == HbmHeavyDoorBlockEntity.STATE_CLOSED) {
                play(minecraft, pos, profile.closeEnd(), profile.volume());
            }
        }
    }

    private static void play(Minecraft minecraft, BlockPos pos, SoundEvent event, float volume) {
        if (event != null && minecraft.level != null) {
            minecraft.level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    event, SoundSource.BLOCKS, volume, 1.0F, false);
        }
    }

    private static void start(Map<BlockPos, DoorLoopSound> sounds, BlockPos pos, SoundEvent event, float volume) {
        if (event == null) {
            stop(sounds, pos);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        DoorLoopSound sound = sounds.get(pos);
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            sound = new DoorLoopSound(sounds, pos, event, volume);
            sounds.put(pos, sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepAlive();
    }

    private static void keepAlive(Map<BlockPos, DoorLoopSound> sounds, BlockPos pos) {
        DoorLoopSound sound = sounds.get(pos);
        if (sound != null) {
            sound.keepAlive();
        }
    }

    private static void stop(Map<BlockPos, DoorLoopSound> sounds, BlockPos pos) {
        DoorLoopSound sound = sounds.remove(pos);
        if (sound != null) {
            sound.requestStop();
        }
    }

    private record Profile(SoundEvent openStart, SoundEvent openLoop, SoundEvent secondaryLoop,
                           SoundEvent openEnd, SoundEvent closeStart, SoundEvent closeLoop,
                           SoundEvent closeEnd, float volume) {
        private static Profile forDoor(HbmDoorDecl door) {
            return switch (door) {
                case TRANSITION_SEAL -> new Profile(HbmSoundEvents.DOOR_TRANSITION_SEAL_OPEN.get(), null, null, null, HbmSoundEvents.DOOR_TRANSITION_SEAL_OPEN.get(), null, null, 6.0F);
                case FIRE_DOOR -> new Profile(null, HbmSoundEvents.WGH_START.get(), HbmSoundEvents.DOOR_ALARM6.get(), HbmSoundEvents.WGH_STOP.get(), null, HbmSoundEvents.WGH_START.get(), HbmSoundEvents.WGH_STOP.get(), 2.0F);
                case SLIDING_BLAST_DOOR, SLIDING_BLAST_DOOR_2 -> new Profile(null, HbmSoundEvents.DOOR_SLIDING_OPENING.get(), HbmSoundEvents.DOOR_SLIDING_OPENING.get(), HbmSoundEvents.DOOR_SLIDING_OPENED.get(), null, HbmSoundEvents.DOOR_SLIDING_OPENING.get(), HbmSoundEvents.DOOR_SLIDING_SHUT.get(), 2.0F);
                case SLIDING_SEAL_DOOR -> new Profile(HbmSoundEvents.DOOR_SLIDING_SEAL_OPEN.get(), null, null, HbmSoundEvents.DOOR_SLIDING_SEAL_STOP.get(), HbmSoundEvents.DOOR_SLIDING_SEAL_OPEN.get(), null, HbmSoundEvents.DOOR_SLIDING_SEAL_STOP.get(), 2.0F);
                case SECURE_ACCESS_DOOR -> new Profile(null, HbmSoundEvents.DOOR_GARAGE_MOVE.get(), null, HbmSoundEvents.DOOR_GARAGE_STOP.get(), null, HbmSoundEvents.DOOR_GARAGE_MOVE.get(), HbmSoundEvents.DOOR_GARAGE_STOP.get(), 2.0F);
                case ROUND_AIRLOCK_DOOR, LARGE_VEHICLE_DOOR -> new Profile(null, HbmSoundEvents.DOOR_GARAGE_MOVE.get(), null, HbmSoundEvents.DOOR_GARAGE_STOP.get(), null, HbmSoundEvents.DOOR_GARAGE_MOVE.get(), HbmSoundEvents.DOOR_GARAGE_STOP.get(), 2.0F);
                case QE_SLIDING_DOOR, SLIDING_GATE_DOOR -> new Profile(null, HbmSoundEvents.DOOR_QE_SLIDING_OPENING.get(), null, HbmSoundEvents.DOOR_QE_SLIDING_OPENED.get(), null, HbmSoundEvents.DOOR_QE_SLIDING_OPENING.get(), HbmSoundEvents.DOOR_QE_SLIDING_SHUT.get(), 2.0F);
                case QE_CONTAINMENT -> new Profile(null, HbmSoundEvents.WGH_START.get(), null, HbmSoundEvents.WGH_STOP.get(), null, HbmSoundEvents.WGH_START.get(), HbmSoundEvents.WGH_STOP.get(), 2.0F);
                case WATER_DOOR -> new Profile(HbmSoundEvents.DOOR_LEVER.get(), HbmSoundEvents.DOOR_WGH_BIG_START.get(), null, HbmSoundEvents.DOOR_WGH_BIG_STOP.get(), null, HbmSoundEvents.DOOR_WGH_BIG_START.get(), HbmSoundEvents.DOOR_LEVER.get(), 2.0F);
                case SILO_HATCH, SILO_HATCH_LARGE -> new Profile(null, HbmSoundEvents.DOOR_WGH_BIG_START.get(), null, HbmSoundEvents.DOOR_WGH_BIG_STOP.get(), null, HbmSoundEvents.DOOR_WGH_BIG_START.get(), HbmSoundEvents.DOOR_WGH_BIG_STOP.get(), 2.0F);
                case VAULT_DOOR -> new Profile(null, null, null, null, null, null, null, 1.0F);
            };
        }
    }

    private static final class DoorLoopSound extends AbstractTickableSoundInstance {
        private final Map<BlockPos, DoorLoopSound> owner;
        private final BlockPos pos;
        private final float baseVolume;
        private int keepAliveTicks = KEEP_ALIVE_TICKS;

        private DoorLoopSound(Map<BlockPos, DoorLoopSound> owner, BlockPos pos, SoundEvent event, float baseVolume) {
            super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.owner = owner;
            this.pos = pos;
            this.baseVolume = baseVolume;
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
            if (minecraft.level == null || minecraft.player == null || keepAliveTicks-- <= 0) {
                requestStop();
                return;
            }
            BlockEntity entity = minecraft.level.getBlockEntity(pos);
            if (!(entity instanceof HbmHeavyDoorBlockEntity door)
                    || (door.state() != HbmHeavyDoorBlockEntity.STATE_OPENING && door.state() != HbmHeavyDoorBlockEntity.STATE_CLOSING)) {
                requestStop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = distance >= AUDIBLE_DISTANCE ? 0.0F : (float) (baseVolume * (1.0D - distance / AUDIBLE_DISTANCE));
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
            owner.remove(this.pos, this);
        }
    }
}
