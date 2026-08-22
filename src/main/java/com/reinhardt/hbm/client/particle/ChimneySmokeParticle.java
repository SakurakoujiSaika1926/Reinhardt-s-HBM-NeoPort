package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class ChimneySmokeParticle extends TextureSheetParticle {
    private static final float MAX_SCALE = 3.0F;
    private static final float LIFT = 10.0F;
    private static final float ALPHA = 0.25F;
    private static final float STRAFE = 0.075F;
    private final float baseScale;

    protected ChimneySmokeParticle(ClientLevel level, double x, double y, double z, float baseScale, SpriteSet sprites) {
        super(level, x, y, z);
        this.baseScale = baseScale;
        this.setSprite(sprites.get(this.random));
        this.rCol = 0x40 / 255.0F;
        this.gCol = 0x40 / 255.0F;
        this.bCol = 0x40 / 255.0F;
        this.alpha = ALPHA;
        this.quadSize = baseScale;
        this.lifetime = 250 + this.random.nextInt(50);
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA * (1.0F - ageScale);
        this.quadSize = this.baseScale + (float) Math.pow(MAX_SCALE * ageScale - this.baseScale, 2.0D);

        this.age++;
        if (this.yd < LIFT) {
            this.yd += 0.01D;
        }

        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;
        this.xd += 0.02D * ageScale;
        this.zd -= 0.01D * ageScale;

        if (this.age >= this.lifetime) {
            this.remove();
        }

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float baseScale;

        public Provider(SpriteSet sprites, float baseScale) {
            this.sprites = sprites;
            this.baseScale = baseScale;
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
            return new ChimneySmokeParticle(level, x, y, z, this.baseScale, this.sprites);
        }
    }
}
