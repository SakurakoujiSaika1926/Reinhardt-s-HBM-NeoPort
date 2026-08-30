package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Exact 1.7.10 ParticleCoolingTower settings used by PartEmitter effects 2 and 3. */
public final class PartEmitterTowerParticle extends TextureSheetParticle {
    private final float baseScale;
    private final float maxScale;
    private final float lift;

    private PartEmitterTowerParticle(ClientLevel level, double x, double y, double z, boolean large, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(random));
        hasPhysics = false;
        if (large) {
            baseScale = 1.0F;
            maxScale = 10.0F;
            lift = 0.5F;
            lifetime = 750 + random.nextInt(250);
        } else {
            baseScale = 0.25F;
            maxScale = 5.0F;
            lift = 5.0F;
            lifetime = 560 + random.nextInt(20);
            setColor(0.25F, 0.25F, 0.25F);
        }
        alpha = 0.25F;
        quadSize = baseScale;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        float progress = (float) age / (float) lifetime;
        alpha = 0.25F - progress * 0.25F;
        quadSize = baseScale + (float) Math.pow(maxScale * progress - baseScale, 2.0D);
        age++;
        if (yd < lift) {
            yd += 0.01D;
        }
        xd += random.nextGaussian() * 0.075D * progress + 0.02D * progress;
        zd += random.nextGaussian() * 0.075D * progress - 0.01D * progress;
        if (age >= lifetime) {
            remove();
        }
        move(xd, yd, zd);
        xd *= 0.925D;
        yd *= 0.925D;
        zd *= 0.925D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean large;

        public Provider(SpriteSet sprites, boolean large) {
            this.sprites = sprites;
            this.large = large;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new PartEmitterTowerParticle(level, x, y, z, large, sprites);
        }
    }
}
