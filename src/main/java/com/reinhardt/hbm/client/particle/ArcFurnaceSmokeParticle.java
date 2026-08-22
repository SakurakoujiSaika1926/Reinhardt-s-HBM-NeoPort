package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public final class ArcFurnaceSmokeParticle extends TextureSheetParticle {
    private static final float BASE_SCALE = 0.5F;
    private static final float MAX_SCALE = 2.0F;
    private static final float MAX_ALPHA = 0.25F;
    private static final float STRAFE = 0.05F;

    private ArcFurnaceSmokeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(this.random));
        this.rCol = 0.0F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
        this.alpha = MAX_ALPHA;
        this.quadSize = BASE_SCALE;
        this.lifetime = 70 + this.random.nextInt(30);
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = this.age / (float) this.lifetime;
        this.alpha = MAX_ALPHA * (1.0F - ageScale);
        this.quadSize = BASE_SCALE + (float) Math.pow(MAX_SCALE * ageScale - BASE_SCALE, 2.0D);
        this.age++;

        if (this.yd < 0.01D) this.yd += 0.01D;
        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;
        if (this.age >= this.lifetime) this.remove();

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
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
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ArcFurnaceSmokeParticle(level, x, y, z, sprites);
        }
    }
}
