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

public final class LegacySmallExplosionParticle extends TextureSheetParticle {
    private final float hue;
    private float baseScale = 1.0F;

    private LegacySmallExplosionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 25 + this.random.nextInt(10);
        this.gravity = this.random.nextFloat() * -0.01F;
        this.hue = 20.0F + this.random.nextFloat() * 20.0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 1));
        updateColor(0.0F);
    }

    public void configure(float scale) {
        this.baseScale = scale * (0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) {
            remove();
            return;
        }
        this.yd -= this.gravity;
        this.oRoll = this.roll;
        float progress = (float) this.age / (float) this.lifetime;
        this.roll += (1.0F - progress) * Mth.DEG_TO_RAD * 5.0F
                * (((this.hashCode() & 1) == 0 ? 0.0F : 1.0F) - 0.5F);
        this.xd *= 0.65D;
        this.zd *= 0.65D;
        move(this.xd, this.yd, this.zd);
        updateColor(progress);
    }

    @Override
    public float getQuadSize(float partialTick) {
        double progress = (this.age + partialTick) / (double) this.lifetime;
        return (float) ((0.25D + 1.0D - Math.pow(1.0D - progress, 4.0D)
                + (this.age + partialTick) * 0.02D) * this.baseScale);
    }

    private void updateColor(float progress) {
        float saturation = Math.max(1.0F - progress * 2.0F, 0.0F);
        float brightness = Mth.clamp(1.25F - progress * 2.0F, this.hue * 0.01F - 0.1F, 1.0F);
        int rgb = Mth.hsvToRgb(this.hue / 255.0F, saturation, brightness);
        this.rCol = (rgb >> 16 & 0xFF) / 255.0F;
        this.gCol = (rgb >> 8 & 0xFF) / 255.0F;
        this.bCol = (rgb & 0xFF) / 255.0F;
        this.alpha = (float) Math.pow(1.0F - Math.min(progress, 1.0F), 0.25D) * 0.5F;
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
            return new LegacySmallExplosionParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
