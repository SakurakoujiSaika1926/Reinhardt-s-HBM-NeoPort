package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

/** Exact behavioral port of 1.7.10 ParticleGasFlame and its EntitySmokeFX base. */
public final class GasFlameParticle extends TextureSheetParticle {
    private static final float LEGACY_SCALE = 6.5F;
    private final SpriteSet sprites;
    private final float smokeScale;
    private float colorMod = 1.0F;

    private GasFlameParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.sprites = sprites;

        double randomX = (Math.random() * 2.0D - 1.0D) * 0.4D;
        double randomY = (Math.random() * 2.0D - 1.0D) * 0.4D;
        double randomZ = (Math.random() * 2.0D - 1.0D) * 0.4D;
        double randomSpeed = (Math.random() + Math.random() + 1.0D) * 0.15D;
        double length = Math.sqrt(randomX * randomX + randomY * randomY + randomZ * randomZ);
        this.xd = randomX / length * randomSpeed * 0.4D * 0.1D + xSpeed;
        this.yd = (randomY / length * randomSpeed * 0.4D + 0.1D) * 0.1D + ySpeed * 1.5D;
        this.zd = randomZ / length * randomSpeed * 0.4D * 0.1D + zSpeed;

        float legacyParticleScale = (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.smokeScale = legacyParticleScale * 0.75F * LEGACY_SCALE * 0.1F;
        this.lifetime = 30 + this.random.nextInt(13);
        this.hasPhysics = false;
        this.alpha = 1.0F;
        this.setSpriteFromAge(this.sprites);

        updateColor();
        this.colorMod = 0.8F + this.random.nextFloat() * 0.2F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            remove();
        }

        this.setSpriteFromAge(this.sprites);
        double previousYSpeed = this.yd;
        this.yd += 0.004D;
        move(this.xd, this.yd, this.zd);
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;

        updateColor();
        this.yd = previousYSpeed;
        this.xd *= 0.75D;
        this.yd += 0.005D;
        this.zd *= 0.75D;
    }

    private void updateColor() {
        float time = (float) this.age / (float) this.lifetime;
        Color color = Color.getHSBColor(
                Math.max((60.0F - time * 100.0F) / 360.0F, 0.0F),
                1.0F - time * 0.25F,
                1.0F - time * 0.5F
        );
        this.rCol = color.getRed() / 255.0F * this.colorMod;
        this.gCol = color.getGreen() / 255.0F * this.colorMod;
        this.bCol = color.getBlue() / 255.0F * this.colorMod;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float growth = Mth.clamp((this.age + partialTick) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
        return this.smokeScale * growth;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
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
            return new GasFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
