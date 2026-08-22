package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class VomitParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected VomitParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            boolean blood
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
        this.quadSize = 0.18F + this.random.nextFloat() * 0.08F;
        this.lifetime = 150 + this.random.nextInt(50);
        this.gravity = 0.65F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        if (blood) {
            this.rCol = 0.62F + this.random.nextFloat() * 0.18F;
            this.gCol = 0.02F + this.random.nextFloat() * 0.04F;
            this.bCol = 0.01F + this.random.nextFloat() * 0.03F;
        } else if (this.random.nextBoolean()) {
            this.rCol = 0.30F + this.random.nextFloat() * 0.08F;
            this.gCol = 0.62F + this.random.nextFloat() * 0.18F;
            this.bCol = 0.08F + this.random.nextFloat() * 0.06F;
        } else {
            this.rCol = 0.52F + this.random.nextFloat() * 0.08F;
            this.gCol = 0.74F + this.random.nextFloat() * 0.12F;
            this.bCol = 0.12F + this.random.nextFloat() * 0.07F;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean blood;

        public Provider(SpriteSet sprites, boolean blood) {
            this.sprites = sprites;
            this.blood = blood;
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
            return new VomitParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.blood);
        }
    }
}
