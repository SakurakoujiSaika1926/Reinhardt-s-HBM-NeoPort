package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.SoyuzLaunchpadBlockEntity;
import com.reinhardt.hbm.entity.SoyuzEntity;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Latest TileEntityLaunchpadSoyuz sounds and launch-pad vapor/smoke effects. */
public final class SoyuzLaunchpadClientEffects {
    private static final Map<Key, MovingSound> SOUNDS = new HashMap<>();

    private SoyuzLaunchpadClientEffects() {
    }

    public static void tick(SoyuzLaunchpadBlockEntity launchpad) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != launchpad.getLevel()) {
            return;
        }

        updateSound(launchpad, 0,
                launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING);
        float carriage = launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_CARRIAGE, 1.0F);
        updateSound(launchpad, 1, launchpad.renderPower() >= SoyuzLaunchpadBlockEntity.CONSUMPTION
                && carriage > 0.0F && carriage < 1.0F);
        float rotor = launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_ROTOR, 1.0F);
        updateSound(launchpad, 2, launchpad.renderPower() >= SoyuzLaunchpadBlockEntity.CONSUMPTION
                && rotor > 0.0F && rotor < 1.0F);

        Direction facing = launchpad.getBlockState().getValue(LargeMachineBlock.FACING);
        Direction rotated = facing.getClockWise();
        BlockPos core = launchpad.getBlockPos();
        double launchX = core.getX() + 0.5D - facing.getStepX() * 4.0D - rotated.getStepX() * 4.0D;
        double launchZ = core.getZ() + 0.5D - facing.getStepZ() * 4.0D - rotated.getStepZ() * 4.0D;

        boolean venting = (launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.FUELING
                || launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.READY
                || launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING)
                && launchpad.hasOxidizer();
        if (venting) {
            double x = core.getX() + 0.5D - facing.getStepX() * 4.0D - rotated.getStepX();
            double z = core.getZ() + 0.5D - facing.getStepZ() * 4.0D - rotated.getStepZ();
            for (int i = 0; i < 3; i++) {
                client.level.addParticle(ParticleTypes.CLOUD,
                        x + client.level.random.nextGaussian() * 0.75D,
                        core.getY() + 4.0D,
                        z + client.level.random.nextGaussian() * 0.75D,
                        (client.level.random.nextDouble() - 0.5D) * 0.075D,
                        0.015D,
                        (client.level.random.nextDouble() - 0.5D) * 0.075D);
            }
        }

        boolean rocketPresent = !client.level.getEntitiesOfClass(SoyuzEntity.class,
                new AABB(launchX - 1.0D, core.getY() + 4.0D, launchZ - 1.0D,
                        launchX + 1.0D, core.getY() + 14.0D, launchZ + 1.0D)).isEmpty();
        if (rocketPresent || launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING
                && launchpad.countdown() <= 20) {
            double strength = client.level.random.nextGaussian() * 3.0D + 6.0D;
            Vec3 vector = new Vec3(strength, 0.0D, 0.0D)
                    .yRot(client.level.random.nextInt(360));
            for (int i = 0; i < 50; i++) {
                double factor = client.level.random.nextDouble();
                client.level.addParticle(HbmParticleTypes.LAUNCHER_EX_SMOKE.get(),
                        launchX, core.getY() + 1.0D, launchZ,
                        vector.x * factor, 0.0D, vector.z * factor);
                vector = vector.yRot(360.0F / 50.0F);
            }
        }
    }

    private static void updateSound(SoyuzLaunchpadBlockEntity launchpad, int index, boolean running) {
        Key key = new Key(launchpad.getBlockPos().immutable(), index);
        MovingSound sound = SOUNDS.get(key);
        Minecraft client = Minecraft.getInstance();
        if (!running) {
            if (sound != null) {
                sound.finish();
                playStop(launchpad, index);
            }
            return;
        }
        if (sound == null || sound.isStopped() || !client.getSoundManager().isActive(sound)) {
            sound = new MovingSound(launchpad, index);
            SOUNDS.put(key, sound);
            client.getSoundManager().play(sound);
        }
    }

    private static void playStop(SoyuzLaunchpadBlockEntity launchpad, int index) {
        if (index == 0 || launchpad.getLevel() == null) {
            return;
        }
        Vec3 position = soundPosition(launchpad, index);
        launchpad.getLevel().playLocalSound(position.x, position.y, position.z,
                index == 1 ? HbmSoundEvents.DOOR_GARAGE_STOP.get()
                        : HbmSoundEvents.DOOR_WGH_BIG_STOP.get(),
                SoundSource.BLOCKS, 35.0F, index == 1 ? 1.0F : 0.75F, false);
    }

    private static Vec3 soundPosition(SoyuzLaunchpadBlockEntity launchpad, int index) {
        Direction facing = launchpad.getBlockState().getValue(LargeMachineBlock.FACING);
        Direction rotated = facing.getClockWise();
        BlockPos core = launchpad.getBlockPos();
        double x = core.getX() + 0.5D - facing.getStepX() * 4.0D - rotated.getStepX() * 4.0D;
        double z = core.getZ() + 0.5D - facing.getStepZ() * 4.0D - rotated.getStepZ() * 4.0D;
        if (index == 1) {
            double distance = 24.0D + 20.5D * (1.0D
                    - launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_CARRIAGE, 1.0F));
            x -= rotated.getStepX() * distance;
            z -= rotated.getStepZ() * distance;
        } else if (index == 2) {
            double distance = 18.0D + 20.5D * (1.0D
                    - launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_CARRIAGE, 1.0F));
            x -= rotated.getStepX() * distance;
            z -= rotated.getStepZ() * distance;
        }
        return new Vec3(x, core.getY() + 4.0D, z);
    }

    private record Key(BlockPos pos, int index) {
    }

    private static final class MovingSound extends AbstractTickableSoundInstance {
        private final SoyuzLaunchpadBlockEntity launchpad;
        private final int index;

        private MovingSound(SoyuzLaunchpadBlockEntity launchpad, int index) {
            super(event(index), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.launchpad = launchpad;
            this.index = index;
            this.looping = true;
            this.attenuation = Attenuation.LINEAR;
            this.volume = 2.0F;
            this.pitch = index == 2 ? 0.75F : 1.0F;
            updatePosition();
        }

        @Override
        public void tick() {
            Minecraft client = Minecraft.getInstance();
            if (client.level != this.launchpad.getLevel() || this.launchpad.isRemoved()
                    || !client.level.hasChunkAt(this.launchpad.getBlockPos())
                    || client.level.getBlockEntity(this.launchpad.getBlockPos()) != this.launchpad
                    || !isStillRunning()) {
                finish();
                return;
            }
            updatePosition();
        }

        private boolean isStillRunning() {
            if (this.index == 0) {
                return this.launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING;
            }
            int animation = this.index == 1
                    ? SoyuzLaunchpadBlockEntity.INDEX_CARRIAGE : SoyuzLaunchpadBlockEntity.INDEX_ROTOR;
            float position = this.launchpad.renderPosition(animation, 1.0F);
            return this.launchpad.renderPower() >= SoyuzLaunchpadBlockEntity.CONSUMPTION
                    && position > 0.0F && position < 1.0F;
        }

        private void updatePosition() {
            Vec3 position = soundPosition(this.launchpad, this.index);
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void finish() {
            stop();
            SOUNDS.remove(new Key(this.launchpad.getBlockPos(), this.index), this);
        }

        private static SoundEvent event(int index) {
            return switch (index) {
                case 0 -> HbmSoundEvents.SOYUZ_READY.get();
                case 1 -> HbmSoundEvents.DOOR_GARAGE_MOVE.get();
                case 2 -> HbmSoundEvents.DOOR_WGH_BIG_START.get();
                default -> throw new IllegalArgumentException("Unknown Soyuz sound index " + index);
            };
        }
    }
}
