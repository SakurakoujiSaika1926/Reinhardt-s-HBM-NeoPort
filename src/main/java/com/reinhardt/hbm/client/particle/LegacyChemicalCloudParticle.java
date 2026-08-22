package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public final class LegacyChemicalCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean orange;

    private LegacyChemicalCloudParticle(ClientLevel level, double x, double y, double z,
                                        double motionX, double motionY, double motionZ,
                                        SpriteSet sprites, Kind kind) {
        super(level, x, y, z, motionX, motionY, motionZ);
        this.sprites = sprites;
        this.orange = kind.orange;
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.lifetime = kind.minimumLifetime + random.nextInt(kind.lifetimeVariance);
        this.quadSize = 1.875F + random.nextFloat() * 0.9375F;
        this.hasPhysics = true;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }

        if (orange) {
            xd *= 0.86D;
            yd = yd * 0.86D - 0.1D;
            zd *= 0.86D;
        } else {
            xd *= 0.76D;
            yd *= 0.76D;
            zd *= 0.76D;
            if (level.isRainingAt(BlockPos.containing(x, y, z))) {
                yd -= 0.01D;
            }
        }
        move(xd, yd, zd);
        if (onGround && random.nextInt(5) != 0) {
            remove();
        }
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double motionX, double motionY, double motionZ) {
            return new LegacyChemicalCloudParticle(
                    level, x, y, z, motionX, motionY, motionZ, sprites, kind
            );
        }
    }

    public enum Kind {
        CHLORINE(700, 101, false),
        CLOUD(900, 301, false),
        ORANGE(900, 301, true);

        private final int minimumLifetime;
        private final int lifetimeVariance;
        private final boolean orange;

        Kind(int minimumLifetime, int lifetimeVariance, boolean orange) {
            this.minimumLifetime = minimumLifetime;
            this.lifetimeVariance = lifetimeVariance;
            this.orange = orange;
        }
    }
}
