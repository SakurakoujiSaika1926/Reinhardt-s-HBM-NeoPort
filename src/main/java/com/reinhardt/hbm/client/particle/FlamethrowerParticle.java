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

public final class FlamethrowerParticle extends TextureSheetParticle {
    private final float initialRed;
    private final float initialGreen;
    private final float initialBlue;
    private final float rollStep;

    private FlamethrowerParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            boolean balefire
    ) {
        super(level, x, y, z);
        this.setSprite(sprites.get(this.random));
        this.lifetime = 20 + this.random.nextInt(10);
        this.quadSize = 0.5F;
        if (xSpeed != 0.0D || ySpeed != 0.0D || zSpeed != 0.0D) {
            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;
        } else {
            this.xd = this.random.nextGaussian() * 0.02D;
            this.yd = 0.0D;
            this.zd = this.random.nextGaussian() * 0.02D;
        }
        this.rollStep = (this.random.nextBoolean() ? 15.0F : -15.0F) * Mth.DEG_TO_RAD;

        float hue = balefire
                ? 65.0F + this.random.nextFloat() * 35.0F
                : 15.0F + this.random.nextFloat() * 25.0F;
        Color color = Color.getHSBColor(hue / 255.0F, 1.0F, 1.0F);
        this.initialRed = color.getRed() / 255.0F;
        this.initialGreen = color.getGreen() / 255.0F;
        this.initialBlue = color.getBlue() / 255.0F;
        this.rCol = this.initialRed;
        this.gCol = this.initialGreen;
        this.bCol = this.initialBlue;
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
        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        this.yd += 0.01D;
        this.oRoll = this.roll;
        this.roll += this.rollStep;
        move(this.xd, this.yd, this.zd);
    }

    @Override
    public float getQuadSize(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;
        return (float) ((ageScaled * 1.25D + 0.25D) * 0.5D);
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTick) {
        float ageScaled = Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        float add = 0.75F - ageScaled;
        this.rCol = this.initialRed + add;
        this.gCol = this.initialGreen + add;
        this.bCol = this.initialBlue + add;
        this.alpha = (float) Math.sqrt(1.0F - ageScaled) * 0.5F;
        super.render(buffer, camera, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean balefire;

        public Provider(SpriteSet sprites, boolean balefire) {
            this.sprites = sprites;
            this.balefire = balefire;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new FlamethrowerParticle(
                    level,
                    x,
                    y,
                    z,
                    xSpeed,
                    ySpeed,
                    zSpeed,
                    this.sprites,
                    this.balefire
            );
        }
    }
}
