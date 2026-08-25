package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Direct port of the drain's legacy ParticleSplash parameters. */
public final class DrainSplashParticle extends TextureSheetParticle {
    private DrainSplashParticle(ClientLevel level, double x, double y, double z,
                                double red, double green, double blue, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(this.random));
        float shade = 1.0F - this.random.nextFloat() * 0.2F;
        setColor((float) red * shade, (float) green * shade, (float) blue * shade);
        this.alpha = 0.5F;
        this.quadSize = 0.4F;
        this.lifetime = 200 + this.random.nextInt(50);
        this.gravity = 0.4F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive() || this.onGround) {
            this.remove();
            return;
        }
        this.xd += this.random.nextGaussian() * 0.002D;
        this.zd += this.random.nextGaussian() * 0.002D;
        if (this.yd < -0.5D) {
            this.yd = -0.5D;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double red, double green, double blue) {
            return new DrainSplashParticle(level, x, y, z, red, green, blue, this.sprites);
        }
    }
}
