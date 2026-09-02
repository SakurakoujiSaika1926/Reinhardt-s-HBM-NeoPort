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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class MeteorTailParticle extends TextureSheetParticle {
    private static final int OLD_QUADS = 10;

    private final SpriteSet sprites;
    private final int oldSeed;
    private float oldScale;

    protected MeteorTailParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.setSprite(sprites.get(this.random));
        this.lifetime = 300 + this.random.nextInt(50);
        this.oldSeed = level.random.nextInt();
        this.oldScale = 1.0F;
        this.quadSize = this.oldScale;
        this.gravity = 0.0F;
        this.friction = 0.91F;
        this.hasPhysics = false;
        this.alpha = 1.0F;
        this.rCol = 1.0F;
        this.gCol = 0.6F;
        this.bCol = 0.0F;
    }

    public MeteorTailParticle configure(float scale, int lifetime) {
        this.oldScale = scale;
        this.quadSize = scale;
        this.lifetime = lifetime;
        return this;
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
        this.xd *= 0.9099999785423279D;
        this.yd *= 0.9099999785423279D;
        this.zd *= 0.9099999785423279D;
        move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Random jitter = new Random(this.oldSeed);
        float life = this.age / (float) this.lifetime;
        float spread = ((float) Math.pow(life * 4.0F, 1.5D) + 1.0F) * this.oldScale;
        float alpha = Mth.clamp((float) Math.pow(1.0F - Math.min(life, 1.0F), 0.5D), 0.0F, 1.0F) * 0.75F;

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
        float oldAlpha = this.alpha;

        for (int i = 0; i < OLD_QUADS; i++) {
            float add = jitter.nextFloat() * 0.3F;
            float dark = 1.0F - Math.min(this.age / (this.lifetime * 0.25F), 1.0F);
            this.rCol = Mth.clamp(dark + add, 0.0F, 1.0F);
            this.gCol = Mth.clamp(0.6F * dark + add, 0.0F, 1.0F);
            this.bCol = Mth.clamp(add, 0.0F, 1.0F);
            this.alpha = alpha;
            this.quadSize = (jitter.nextFloat() * 0.5F + 0.1F + life * 2.0F) * this.oldScale;

            double offsetX = (jitter.nextGaussian() - 1.0D) * 0.2F * spread;
            double offsetY = (jitter.nextGaussian() - 1.0D) * 0.5F * spread;
            double offsetZ = (jitter.nextGaussian() - 1.0D) * 0.2F * spread;
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

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        float life = (this.age + partialTicks) / (float) this.lifetime;
        double radius = ((float) Math.pow(life * 4.0F, 1.5D) + 3.0F) * this.oldScale;
        return new AABB(this.x - radius, this.y - radius, this.z - radius, this.x + radius, this.y + radius, this.z + radius);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new MeteorTailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
