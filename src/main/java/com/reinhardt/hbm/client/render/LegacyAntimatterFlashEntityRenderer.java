package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.reinhardt.hbm.entity.LegacyAntimatterFlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Direct modern draw path for the old 100-ray ParticleAmatFlash. */
public final class LegacyAntimatterFlashEntityRenderer extends EntityRenderer<LegacyAntimatterFlashEntity> {
    private static final RenderType FLASH = RenderType.create(
            "reinhardtshbm_antimatter_flash",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            32768,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .createCompositeState(false));

    public LegacyAntimatterFlashEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyAntimatterFlashEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        double intensity = Mth.clamp((entity.tickCount + partialTick) / LegacyAntimatterFlashEntity.MAX_AGE,
                0.0D, 1.0D);
        int alpha = Mth.clamp((int) Math.round((1.0D - intensity) * 255.0D), 0, 255);
        if (alpha <= 0) {
            return;
        }

        Random random = new Random(432L);
        VertexConsumer consumer = buffers.getBuffer(FLASH);
        PoseStack.Pose pose = poseStack.last();
        double scale = 0.1D * entity.scale() * intensity;
        for (int index = 0; index < 100; index++) {
            Vec3 direction = randomUnit(random);
            Vec3 reference = Math.abs(direction.y) < 0.95D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 u = direction.cross(reference).normalize();
            Vec3 v = direction.cross(u).normalize();
            double length = (random.nextFloat() * 20.0F + 15.0F) * scale;
            double width = (random.nextFloat() * 2.0F + 3.0F) * scale;
            Vec3 tip = direction.scale(length);
            Vec3 a = tip.add(u.scale(-0.866D * width)).add(v.scale(-0.5D * width));
            Vec3 b = tip.add(u.scale(0.866D * width)).add(v.scale(-0.5D * width));
            Vec3 c = tip.add(v.scale(width));
            triangle(consumer, pose, a, b, alpha);
            triangle(consumer, pose, b, c, alpha);
            triangle(consumer, pose, c, a, alpha);
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static Vec3 randomUnit(Random random) {
        double y = random.nextDouble() * 2.0D - 1.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double horizontal = Math.sqrt(1.0D - y * y);
        return new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
    }

    private static void triangle(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, int alpha) {
        consumer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
        consumer.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(255, 255, 255, 0);
        consumer.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(255, 255, 255, 0);
    }

    @Override
    public boolean shouldRender(LegacyAntimatterFlashEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double extent = Math.nextUp(3.5D * Math.abs(entity.scale()));
        AABB bounds = new AABB(
                entity.getX() - extent, entity.getY() - extent, entity.getZ() - extent,
                entity.getX() + extent, entity.getY() + extent, entity.getZ() + extent);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyAntimatterFlashEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
