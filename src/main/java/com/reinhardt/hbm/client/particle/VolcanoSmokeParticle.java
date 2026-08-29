package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Large, long-lived smoke used by TileEntityVolcanoCore's vanillaExt effect. */
public final class VolcanoSmokeParticle extends TextureSheetParticle {
    private VolcanoSmokeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(this.random));
        this.quadSize = 100.0F;
        this.lifetime = 200 + this.random.nextInt(50);
        this.xd = this.random.nextGaussian() * 0.2D;
        this.yd = 2.5D + this.random.nextDouble();
        this.zd = this.random.nextGaussian() * 0.2D;
        this.rCol = 0.25F;
        this.gCol = 0.25F;
        this.bCol = 0.25F;
        this.alpha = 0.85F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        move(xd, yd, zd);
        xd *= 0.96D;
        yd *= 0.96D;
        zd *= 0.96D;
        alpha = 0.85F * (1.0F - (float) age / lifetime);
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new VolcanoSmokeParticle(level, x, y, z, sprites);
        }
    }
}
