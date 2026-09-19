package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Contained mustard-gas drain plume. Unlike the legacy drain tower particle,
 *  this one collides with ceilings and walls so enclosed gas chambers do not
 *  show gas leaking visually through their roof. */
public final class MustardGasDrainParticle extends TextureSheetParticle {
    private static final float BASE_SCALE = 0.375F;
    private static final float MAX_SCALE = 1.25F;
    private static final float LIFT = 0.12F;
    private static final float STRAFE = 0.025F;
    private static final float ALPHA = 0.28F;

    private MustardGasDrainParticle(ClientLevel level, double x, double y, double z,
                                    double red, double green, double blue, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(this.random));
        setColor((float) red, (float) green, (float) blue);
        this.alpha = ALPHA;
        this.quadSize = BASE_SCALE;
        this.lifetime = 60 + this.random.nextInt(30);
        this.hasPhysics = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA - ageScale * ALPHA;
        this.quadSize = BASE_SCALE + (MAX_SCALE - BASE_SCALE) * ageScale;
        this.age++;
        if (this.yd < LIFT) {
            this.yd += 0.006D;
        }
        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        double oldX = this.x;
        double oldY = this.y;
        double oldZ = this.z;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround || movedWasBlocked(oldX, oldY, oldZ)) {
            this.remove();
            return;
        }
        this.xd *= 0.85D;
        this.yd *= 0.85D;
        this.zd *= 0.85D;
    }

    private boolean movedWasBlocked(double oldX, double oldY, double oldZ) {
        return Math.abs(this.x - oldX) + 1.0E-6D < Math.abs(this.xd)
                || Math.abs(this.y - oldY) + 1.0E-6D < Math.abs(this.yd)
                || Math.abs(this.z - oldZ) + 1.0E-6D < Math.abs(this.zd);
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
            return new MustardGasDrainParticle(level, x, y, z, red, green, blue, this.sprites);
        }
    }
}
