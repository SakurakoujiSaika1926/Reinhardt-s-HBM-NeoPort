package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public final class ContrailParticle extends TextureSheetParticle {
    private static final int LEGACY_QUADS = 6;
    private static final ParticleRenderType CONTRAIL_NO_DEPTH = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "HBM_LEGACY_CONTRAIL_NO_DEPTH";
        }
    };

    private final int legacySeed;
    private final float red, green, blue;

    private ContrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, float red, float green, float blue) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.setSprite(sprites.get(this.random));
        this.lifetime = 100 + this.random.nextInt(40);
        this.legacySeed = level.random.nextInt();
        this.red = red; this.green = green; this.blue = blue;
        this.quadSize = 1.0F;
        this.hasPhysics = false;
        this.rCol = 0.0F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.alpha = 1.0F - this.age / (float) this.lifetime;
        if (++this.age >= this.lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Random jitter = new Random(this.legacySeed);
        double baseX = this.x;
        double baseY = this.y;
        double baseZ = this.z;
        double oldBaseX = this.xo;
        double oldBaseY = this.yo;
        double oldBaseZ = this.zo;
        float oldQuadSize = this.quadSize;
        float oldRed = this.rCol;
        float oldGreen = this.gCol;
        float oldBlue = this.bCol;

        for (int i = 0; i < LEGACY_QUADS; i++) {
            float brightness = jitter.nextFloat() * 0.2F + 0.2F;
            this.rCol = red + brightness;
            this.gCol = green + brightness;
            this.bCol = blue + brightness;
            this.quadSize = this.alpha + 0.5F;

            double offsetX = jitter.nextGaussian() * 0.5D;
            double offsetY = jitter.nextGaussian() * 0.5D;
            double offsetZ = jitter.nextGaussian() * 0.5D;
            this.x = baseX + offsetX;
            this.y = baseY + offsetY;
            this.z = baseZ + offsetZ;
            this.xo = oldBaseX + offsetX;
            this.yo = oldBaseY + offsetY;
            this.zo = oldBaseZ + offsetZ;
            super.render(buffer, camera, partialTick);
        }

        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;
        this.xo = oldBaseX;
        this.yo = oldBaseY;
        this.zo = oldBaseZ;
        this.quadSize = oldQuadSize;
        this.rCol = oldRed;
        this.gCol = oldGreen;
        this.bCol = oldBlue;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return CONTRAIL_NO_DEPTH;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        double radius = this.alpha + 1.0D;
        return new AABB(this.x - radius, this.y - radius, this.z - radius,
                this.x + radius, this.y + radius, this.z + radius);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float red, green, blue;

        public Provider(SpriteSet sprites) {
            this(sprites, 0, 0, 0);
        }

        public Provider(SpriteSet sprites, float red, float green, float blue) {
            this.sprites = sprites;
            this.red = red; this.green = green; this.blue = blue;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ContrailParticle(level, x, y, z, this.sprites, red, green, blue);
        }
    }
}
