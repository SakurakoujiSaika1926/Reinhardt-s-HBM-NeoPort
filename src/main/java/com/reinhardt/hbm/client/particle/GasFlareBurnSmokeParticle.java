package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** The 50-tick, no-clip EntitySmokeFX emitted beside a burning 1.7.10 gas flare. */
public final class GasFlareBurnSmokeParticle extends TextureSheetParticle {
    private static final int LEGACY_LIFETIME = 50;
    private final SpriteSet sprites;
    private final float smokeScale;

    private GasFlareBurnSmokeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;

        double randomX = (Math.random() * 2.0D - 1.0D) * 0.4D;
        double randomY = (Math.random() * 2.0D - 1.0D) * 0.4D;
        double randomZ = (Math.random() * 2.0D - 1.0D) * 0.4D;
        double randomSpeed = (Math.random() + Math.random() + 1.0D) * 0.15D;
        double length = Math.sqrt(randomX * randomX + randomY * randomY + randomZ * randomZ);
        this.xd = randomX / length * randomSpeed * 0.4D * 0.1D;
        this.yd = (randomY / length * randomSpeed * 0.4D + 0.1D) * 0.1D;
        this.zd = randomZ / length * randomSpeed * 0.4D * 0.1D;

        float shade = (float) (Math.random() * 0.3D);
        this.rCol = shade;
        this.gCol = shade;
        this.bCol = shade;
        float legacyParticleScale = (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.smokeScale = legacyParticleScale * 0.75F * 0.1F;
        this.lifetime = LEGACY_LIFETIME;
        this.hasPhysics = false;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            remove();
        }

        this.setSpriteFromAge(this.sprites);
        this.yd += 0.004D;
        move(this.xd, this.yd, this.zd);
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float growth = Mth.clamp((this.age + partialTick) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
        return this.smokeScale * growth;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new GasFlareBurnSmokeParticle(level, x, y, z, this.sprites);
        }
    }
}
