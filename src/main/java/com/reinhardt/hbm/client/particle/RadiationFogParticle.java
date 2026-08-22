package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class RadiationFogParticle extends TextureSheetParticle {
    private static final int OLD_MAX_AGE = 400;
    private static final int QUAD_COUNT = 25;
    private static final Random OLD_RANDOM = new Random(50L);
    private static final FogQuad[] OLD_QUADS = createOldQuads();
    private final SpriteSet sprites;

    protected RadiationFogParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = OLD_MAX_AGE;
        this.quadSize = 7.5F;
        this.rCol = 0.85F;
        this.gCol = 0.9F;
        this.bCol = 0.5F;
        this.alpha = 0.0F;
        this.hasPhysics = false;
        setSprite(sprites.get(this.random));
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
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float oldAlpha = this.alpha;
        float oldSize = this.quadSize;
        float alpha = Mth.sin((float) (this.age * Math.PI / OLD_MAX_AGE)) * 0.125F;
        this.alpha = alpha;
        for (FogQuad quad : OLD_QUADS) {
            this.quadSize = oldSize * quad.size();
            double baseX = this.x;
            double baseY = this.y;
            double baseZ = this.z;
            double oldBaseX = this.xo;
            double oldBaseY = this.yo;
            double oldBaseZ = this.zo;
            this.x = baseX + quad.x();
            this.y = baseY + quad.y();
            this.z = baseZ + quad.z();
            this.xo = oldBaseX + quad.x();
            this.yo = oldBaseY + quad.y();
            this.zo = oldBaseZ + quad.z();
            super.render(buffer, camera, partialTick);
            this.x = baseX;
            this.y = baseY;
            this.z = baseZ;
            this.xo = oldBaseX;
            this.yo = oldBaseY;
            this.zo = oldBaseZ;
        }
        this.quadSize = oldSize;
        this.alpha = oldAlpha;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    private static FogQuad[] createOldQuads() {
        FogQuad[] quads = new FogQuad[QUAD_COUNT];
        synchronized (OLD_RANDOM) {
            double translateX = 0.0D;
            double translateY = 0.0D;
            double translateZ = 0.0D;
            for (int i = 0; i < QUAD_COUNT; i++) {
                double dX = (OLD_RANDOM.nextGaussian() - 1.0D) * 2.5D;
                double dY = (OLD_RANDOM.nextGaussian() - 1.0D) * 0.15D;
                double dZ = (OLD_RANDOM.nextGaussian() - 1.0D) * 2.5D;
                float size = (float) OLD_RANDOM.nextDouble();
                translateX += dX;
                translateY += dY;
                translateZ += dZ;
                double x = translateX + OLD_RANDOM.nextGaussian() * 0.5D;
                double y = translateY + OLD_RANDOM.nextGaussian() * 0.5D;
                double z = translateZ + OLD_RANDOM.nextGaussian() * 0.5D;
                quads[i] = new FogQuad(x, y, z, size);
            }
        }
        return quads;
    }

    private record FogQuad(double x, double y, double z, float size) {
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RadiationFogParticle(level, x, y, z, this.sprites);
        }
    }
}
