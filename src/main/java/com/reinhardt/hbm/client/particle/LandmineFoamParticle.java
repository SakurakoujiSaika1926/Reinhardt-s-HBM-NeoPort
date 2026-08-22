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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LandmineFoamParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final List<TrailPoint> trail = new ArrayList<>();
    private final float baseScale;
    private final float maxScale;
    private final float buoyancy = 0.05F;
    private final float jitter = 0.15F;
    private final float drag = 0.96F;
    private final long renderSeed;
    private int explosionPhase;

    private LandmineFoamParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.renderSeed = this.random.nextLong();
        this.baseScale = 0.3F + this.random.nextFloat() * 0.7F;
        this.maxScale = 1.5F;
        this.lifetime = 50;
        this.gravity = 0.005F + this.random.nextFloat() * 0.015F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 1));
        this.quadSize = this.baseScale;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.trail.add(0, new TrailPoint(this.x, this.y, this.z));
        while (this.trail.size() > 15) {
            this.trail.remove(this.trail.size() - 1);
        }
        ++this.age;
        if (this.age >= this.lifetime) {
            remove();
            return;
        }
        float phaseRatio = (float) this.age / (float) this.lifetime;
        if (phaseRatio < 0.3F) {
            this.explosionPhase = 0;
            if (phaseRatio < 0.15F) {
                this.yd += this.buoyancy * 6.0F;
            } else {
                this.yd += this.buoyancy * (1.0F - phaseRatio / 0.3F) * 2.0F;
            }
            this.quadSize = this.baseScale + (this.maxScale - this.baseScale) * (phaseRatio / 0.3F);
        } else if (phaseRatio < 0.6F) {
            this.explosionPhase = 1;
            this.yd *= 0.98F;
            this.quadSize = this.maxScale;
        } else {
            this.explosionPhase = 2;
            this.yd -= this.gravity;
            this.quadSize = this.maxScale * (1.0F - ((phaseRatio - 0.6F) / 0.4F) * 0.7F);
        }
        this.alpha = 0.8F * (1.0F - phaseRatio * phaseRatio);
        this.xd += (this.random.nextFloat() - 0.5F) * this.jitter;
        this.zd += (this.random.nextFloat() - 0.5F) * this.jitter;
        this.xd *= this.drag;
        this.yd *= this.drag;
        this.zd *= this.drag;
        move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        renderBubbles(buffer, camera, partialTick, this.x, this.y, this.z, this.quadSize, this.alpha);
        for (int i = 1; i < this.trail.size(); i++) {
            TrailPoint point = this.trail.get(i);
            float factor = 1.0F - (float) i / 15.0F;
            renderBubbles(buffer, camera, partialTick, point.x, point.y, point.z,
                    this.quadSize * factor, this.alpha * factor * 0.7F);
        }
    }

    private void renderBubbles(VertexConsumer buffer, Camera camera, float partialTick,
                               double x, double y, double z, float scale, float alpha) {
        Random stable = new Random(this.renderSeed + (long) (x * 100.0D) + (long) (y * 10.0D) + (long) z);
        int bubbleCount = this.explosionPhase == 0 ? 8 : this.explosionPhase == 1 ? 6 : 4;
        double offset = this.explosionPhase == 0 ? 0.4D : this.explosionPhase == 1 ? 0.6D : 0.9D;
        double oldX = this.x;
        double oldY = this.y;
        double oldZ = this.z;
        double oldXo = this.xo;
        double oldYo = this.yo;
        double oldZo = this.zo;
        float oldScale = this.quadSize;
        float oldAlpha = this.alpha;
        for (int i = 0; i < bubbleCount; i++) {
            float whiteness = 0.9F + stable.nextFloat() * 0.1F;
            this.rCol = whiteness;
            this.gCol = whiteness;
            this.bCol = whiteness;
            this.alpha = alpha;
            this.quadSize = scale * (stable.nextFloat() * 0.5F + 0.75F);
            double offsetX = stable.nextGaussian() * offset;
            double offsetY = stable.nextGaussian() * offset * 0.7D;
            double offsetZ = stable.nextGaussian() * offset;
            this.x = x + offsetX;
            this.y = y + offsetY;
            this.z = z + offsetZ;
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            super.render(buffer, camera, partialTick);
        }
        this.x = oldX;
        this.y = oldY;
        this.z = oldZ;
        this.xo = oldXo;
        this.yo = oldYo;
        this.zo = oldZo;
        this.quadSize = oldScale;
        this.alpha = oldAlpha;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private record TrailPoint(double x, double y, double z) {
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
            return new LandmineFoamParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
