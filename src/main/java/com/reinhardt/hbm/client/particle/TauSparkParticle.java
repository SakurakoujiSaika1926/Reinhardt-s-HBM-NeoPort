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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TauSparkParticle extends Particle {
    private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, net.minecraft.client.renderer.texture.TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.lineWidth(3.0F);
            return tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override
        public String toString() {
            return "HBM_TAU_SPARK";
        }
    };

    private final List<Vec3> steps = new ArrayList<>();
    private final int threshold;

    protected TauSparkParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.threshold = 4 + this.random.nextInt(3);
        this.steps.add(new Vec3(this.xd, this.yd, this.zd));
        this.lifetime = 20 + this.random.nextInt(10);
        this.gravity = 0.5F;
        this.hasPhysics = true;
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

        this.steps.add(new Vec3(this.xd, this.yd, this.zd));
        while (this.steps.size() > this.threshold) {
            this.steps.removeFirst();
        }

        this.yd -= 0.04D * this.gravity;
        double lastY = this.yd;
        move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            this.onGround = false;
            this.yd = -lastY * 0.8D;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (this.steps.size() < 3) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        Vec3 previous = new Vec3(
                Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x,
                Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y,
                Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z
        );
        for (int index = 1; index < this.steps.size() - 1; index++) {
            Vec3 current = previous.add(this.steps.get(index));
            vertex(buffer, previous);
            vertex(buffer, current);
            previous = current;
        }
    }

    private static void vertex(VertexConsumer buffer, Vec3 position) {
        buffer.addVertex((float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 255);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return getBoundingBox().inflate(1.5D);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
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
            return new TauSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
