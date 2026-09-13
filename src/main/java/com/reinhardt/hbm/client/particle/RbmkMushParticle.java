package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class RbmkMushParticle extends TextureSheetParticle {
    private static final ParticleRenderType LEGACY_ADDITIVE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            // ParticleRBMKMush used SRC_ALPHA, ONE, disabled depth writes,
            // and the particle atlas was bound for its 30-frame sheet.
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "HBM_LEGACY_RBMK_MUSH";
        }
    };

    private final SpriteSet sprites;
    private final float baseScale;

    protected RbmkMushParticle(ClientLevel level, double x, double y, double z, double scale, SpriteSet sprites) {
        // ParticleRBMKMush rendered its quad around (spawnY + particleScale),
        // rather than around spawnY.  Store that explicit legacy position so
        // the lower edge and the cap height remain identical for every
        // footprint size.
        super(level, x, y + scale, z);
        this.sprites = sprites;
        // The old particle accepted the supplied scale verbatim. A one-column
        // meltdown therefore creates the same zero-scale effect as 1.7.10.
        this.baseScale = (float) scale;
        this.lifetime = 50;
        this.quadSize = this.baseScale;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.hasPhysics = false;
        setSpriteFromAge(sprites);
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
        this.quadSize = this.baseScale;
        setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return LEGACY_ADDITIVE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RbmkMushParticle(level, x, y, z, xSpeed, this.sprites);
        }
    }
}
