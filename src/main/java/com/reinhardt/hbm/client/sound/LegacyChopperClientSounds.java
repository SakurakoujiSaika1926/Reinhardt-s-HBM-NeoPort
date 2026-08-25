package com.reinhardt.hbm.client.sound;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyChopperEntity;
import com.reinhardt.hbm.entity.LegacyChopperMineEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** The three old MovingSoundPlayerLoop variants, attached once per live entity rather than emitted globally. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class LegacyChopperClientSounds {
    private static final Map<Integer, EntityLoop> LOOPS = new HashMap<>();

    private LegacyChopperClientSounds() {
    }

    /**
     * Entity classes are common-side.  Keeping the loop attachment here avoids
     * loading any net.minecraft.client type on a dedicated server.
     */
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopAll();
            return;
        }
        for (LegacyChopperEntity chopper : minecraft.level.getEntitiesOfClass(
                LegacyChopperEntity.class, minecraft.player.getBoundingBox().inflate(256.0D))) {
            tick(chopper);
        }
        for (LegacyChopperMineEntity mine : minecraft.level.getEntitiesOfClass(
                LegacyChopperMineEntity.class, minecraft.player.getBoundingBox().inflate(256.0D))) {
            tick(mine);
        }
    }

    public static void tick(LegacyChopperEntity chopper) {
        ensure(chopper, chopper.isCrashing() ? HbmSoundEvents.ENTITY_CHOPPER_CRASHING_LOOP.get()
                : HbmSoundEvents.ENTITY_CHOPPER_FLYING_LOOP.get(), 0.5F);
    }

    public static void tick(LegacyChopperMineEntity mine) {
        ensure(mine, HbmSoundEvents.ENTITY_CHOPPER_MINE_LOOP.get(), 1.0F);
    }

    private static void ensure(Entity entity, SoundEvent sound, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || entity.isRemoved()) {
            stop(entity.getId());
            return;
        }
        EntityLoop existing = LOOPS.get(entity.getId());
        if (existing == null || existing.isStopped() || !minecraft.getSoundManager().isActive(existing)
                || existing.sound != sound || existing.soundPitch != pitch) {
            stop(entity.getId());
            EntityLoop loop = new EntityLoop(entity, sound, pitch);
            LOOPS.put(entity.getId(), loop);
            minecraft.getSoundManager().play(loop);
            existing = loop;
        }
        existing.keepAlive();
    }

    private static void stop(int id) {
        EntityLoop sound = LOOPS.remove(id);
        if (sound != null) sound.requestStop();
    }

    private static void stopAll() {
        for (EntityLoop loop : new ArrayList<>(LOOPS.values())) {
            loop.requestStop();
        }
    }

    private static final class EntityLoop extends AbstractTickableSoundInstance {
        private final int entityId;
        private final SoundEvent sound;
        private final float soundPitch;
        private int keepAlive = 10;

        private EntityLoop(Entity entity, SoundEvent sound, float pitch) {
            super(sound, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.entityId = entity.getId();
            this.sound = sound;
            this.soundPitch = pitch;
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
            looping = true;
            delay = 0;
            volume = 10.0F;
            this.pitch = pitch;
            attenuation = Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || !(minecraft.level.getEntity(entityId) instanceof Entity entity)
                    || entity.isRemoved() || keepAlive-- <= 0) {
                requestStop();
                return;
            }
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        }

        @Override
        public boolean canStartSilent() { return true; }
        private void keepAlive() { keepAlive = 10; }
        private void requestStop() { stop(); LOOPS.remove(entityId, this); }
    }
}
