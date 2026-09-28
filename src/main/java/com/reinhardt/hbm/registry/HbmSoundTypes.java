package com.reinhardt.hbm.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

import java.util.Random;

public final class HbmSoundTypes {
    public static final SoundType PIPE = new PipeSoundType();
    public static final SoundType PLATEMETAL = new PlatemetalSoundType();

    private HbmSoundTypes() {
    }

    private static final class PipeSoundType extends SoundType {
        private final Random random = new Random();
        private SubType probableSubType = SubType.PLACE;

        private PipeSoundType() {
            super(
                    0.85F,
                    0.85F,
                    SoundType.METAL.getBreakSound(),
                    SoundType.METAL.getStepSound(),
                    SoundType.METAL.getPlaceSound(),
                    SoundType.METAL.getHitSound(),
                    SoundType.METAL.getFallSound()
            );
        }

        @Override
        public SoundEvent getPlaceSound() {
            this.probableSubType = SubType.PLACE;
            return HbmSoundEvents.PIPE_PLACED.get();
        }

        @Override
        public SoundEvent getBreakSound() {
            this.probableSubType = SubType.BREAK;
            return HbmSoundEvents.PIPE_PLACED.get();
        }

        @Override
        public SoundEvent getStepSound() {
            this.probableSubType = SubType.STEP;
            return super.getStepSound();
        }

        @Override
        public float getPitch() {
            float base = this.probableSubType == SubType.BREAK ? 0.7F : 0.85F;
            return base + this.random.nextFloat() * 0.2F;
        }

        private enum SubType {
            PLACE,
            BREAK,
            STEP
        }
    }

    private static final class PlatemetalSoundType extends SoundType {
        private PlatemetalSoundType() {
            // Deferred sound events are not bound while HbmBlocks is initialized.
            // Vanilla placeholders keep construction safe; the getters resolve the
            // exact upstream place/break and step events when a sound is requested.
            super(
                    1.0F,
                    1.0F,
                    SoundType.METAL.getBreakSound(),
                    SoundType.METAL.getStepSound(),
                    SoundType.METAL.getPlaceSound(),
                    SoundType.METAL.getHitSound(),
                    SoundType.METAL.getFallSound()
            );
        }

        @Override
        public SoundEvent getBreakSound() {
            return HbmSoundEvents.PLATEMETAL_PLACE.get();
        }

        @Override
        public SoundEvent getStepSound() {
            return HbmSoundEvents.PLATEMETAL_STEP.get();
        }

        @Override
        public SoundEvent getPlaceSound() {
            return HbmSoundEvents.PLATEMETAL_PLACE.get();
        }

        @Override
        public SoundEvent getHitSound() {
            return HbmSoundEvents.PLATEMETAL_STEP.get();
        }

        @Override
        public SoundEvent getFallSound() {
            return HbmSoundEvents.PLATEMETAL_STEP.get();
        }
    }
}
