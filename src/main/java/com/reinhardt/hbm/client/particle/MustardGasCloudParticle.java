package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Small, contained mustard-gas visibility particle for ashglasses.
 *  The legacy generic cloud particle is wider than a block and can visually
 *  protrude through a sealed roof; this one stays inside the gas volume. */
public final class MustardGasCloudParticle extends TextureSheetParticle {
    private static final float ALPHA = 0.22F;
    private static final float BASE_SCALE = 0.45F;
    private static final float SCALE_VARIANCE = 0.20F;

    private final SpriteSet sprites;

    private MustardGasCloudParticle(ClientLevel level, double x, double y, double z,
                                    double motionX, double motionY, double motionZ,
                                    SpriteSet sprites) {
        super(level, x, y, z, motionX, motionY, motionZ);
        this.sprites = sprites;
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.alpha = ALPHA;
        this.quadSize = BASE_SCALE + this.random.nextFloat() * SCALE_VARIANCE;
        this.lifetime = 80 + this.random.nextInt(40);
        this.hasPhysics = true;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }

        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA * (1.0F - ageScale);
        this.xd *= 0.74D;
        this.yd *= 0.74D;
        this.zd *= 0.74D;

        double oldX = this.x;
        double oldY = this.y;
        double oldZ = this.z;
        move(this.xd, this.yd, this.zd);
        if (movedWasBlocked(oldX, oldY, oldZ)) {
            remove();
            return;
        }
        setSpriteFromAge(this.sprites);
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
                                       double motionX, double motionY, double motionZ) {
            return new MustardGasCloudParticle(level, x, y, z, motionX, motionY, motionZ, this.sprites);
        }
    }
}
