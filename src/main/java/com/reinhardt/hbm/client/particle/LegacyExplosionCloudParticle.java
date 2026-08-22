package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
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

import java.util.Random;

public final class LegacyExplosionCloudParticle extends TextureSheetParticle {
    private final long renderSeed;
    private float baseScale = 1.0F;

    private LegacyExplosionCloudParticle(
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
        this.lifetime = 300 + this.random.nextInt(50);
        this.hasPhysics = false;
        this.renderSeed = this.random.nextLong();
        this.setSprite(sprites.get(0, 1));
    }

    public void configure(float scale, int maxAge) {
        this.baseScale = Math.max(0.0F, scale);
        this.lifetime = Math.max(1, maxAge);
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
        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        double baseX = this.x;
        double baseY = this.y;
        double baseZ = this.z;
        double oldX = this.xo;
        double oldY = this.yo;
        double oldZ = this.zo;
        float oldSize = this.quadSize;
        float oldRed = this.rCol;
        float oldGreen = this.gCol;
        float oldBlue = this.bCol;
        float oldAlpha = this.alpha;
        float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
        float dark = 1.0F - Math.min((float) this.age / (this.lifetime * 0.25F), 1.0F);
        float fade = (float) Math.sqrt(1.0F - progress) * 0.75F;
        float spread = ((float) Math.pow(progress * 4.0F, 1.5D) + 1.0F) * this.baseScale;
        Random stable = new Random(this.renderSeed);
        for (int i = 0; i < 10; i++) {
            float add = stable.nextFloat() * 0.3F;
            this.rCol = Mth.clamp(dark + add, 0.0F, 1.0F);
            this.gCol = Mth.clamp(0.6F * dark + add, 0.0F, 1.0F);
            this.bCol = Mth.clamp(add, 0.0F, 1.0F);
            this.alpha = fade;
            this.quadSize = (stable.nextFloat() * 0.5F + 0.1F + progress * 2.0F) * this.baseScale;
            double offsetX = (stable.nextGaussian() - 1.0D) * 0.2D * spread;
            double offsetY = (stable.nextGaussian() - 1.0D) * 0.5D * spread;
            double offsetZ = (stable.nextGaussian() - 1.0D) * 0.2D * spread;
            this.x = baseX + offsetX;
            this.y = baseY + offsetY;
            this.z = baseZ + offsetZ;
            this.xo = oldX + offsetX;
            this.yo = oldY + offsetY;
            this.zo = oldZ + offsetZ;
            super.render(buffer, camera, partialTick);
        }
        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;
        this.xo = oldX;
        this.yo = oldY;
        this.zo = oldZ;
        this.quadSize = oldSize;
        this.rCol = oldRed;
        this.gCol = oldGreen;
        this.bCol = oldBlue;
        this.alpha = oldAlpha;
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
            return new LegacyExplosionCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
