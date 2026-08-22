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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class MukeWaveParticle extends TextureSheetParticle {
    private float maxScale = 45.0F;

    private MukeWaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.setSprite(sprites.get(0, 1));
        this.lifetime = 25;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float progress = this.age + partialTick;
        float alpha = Mth.clamp(1.0F - progress / this.lifetime, 0.0F, 1.0F);
        float scale = (float) ((1.0D - Math.exp(progress * -0.125D)) * this.maxScale);
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x);
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y - 0.25D);
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z);
        int light = LightTexture.FULL_BRIGHT;
        buffer.addVertex(x - scale, y, z - scale).setUv(getU1(), getV1()).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
        buffer.addVertex(x - scale, y, z + scale).setUv(getU1(), getV0()).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
        buffer.addVertex(x + scale, y, z + scale).setUv(getU0(), getV0()).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
        buffer.addVertex(x + scale, y, z - scale).setUv(getU0(), getV1()).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return MukeParticleRenderType.ADDITIVE;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return getBoundingBox().inflate(this.maxScale, 1.0D, this.maxScale);
    }

    public void configure(float maxScale, int lifetime) {
        this.maxScale = Math.max(0.0F, maxScale);
        this.lifetime = Math.max(1, lifetime);
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
            return new MukeWaveParticle(level, x, y, z, this.sprites);
        }
    }
}
