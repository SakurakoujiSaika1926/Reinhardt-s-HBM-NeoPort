package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBlackHoleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Renderer equivalent to the old black-hole sphere plus its blue vortex disc. */
public final class LegacyBlackHoleEntityRenderer extends EntityRenderer<LegacyBlackHoleEntity> {
    private static final ResourceLocation CORE = ReinhardtsHBM.id("textures/models/blackhole.png");
    private static final ResourceLocation SWIRL = ReinhardtsHBM.id("textures/entity/bhole.png");
    private static final RenderType CORE_TYPE = RenderType.entityTranslucentEmissive(CORE);
    private static final RenderType SWIRL_TYPE = RenderType.entityTranslucentEmissive(SWIRL);

    public LegacyBlackHoleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LegacyBlackHoleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float size = entity.size();
        poseStack.pushPose();
        renderCore(entity, poseStack, bufferSource.getBuffer(CORE_TYPE), size);
        renderSwirl(entity, partialTick, poseStack, bufferSource.getBuffer(SWIRL_TYPE), size);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyBlackHoleEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(
                entity, visualBounds(entity), frustum, cameraX, cameraY, cameraZ);
    }

    private static AABB visualBounds(LegacyBlackHoleEntity entity) {
        float innerRadius = Math.abs(entity.size()) * 3.0F;
        float outerRadius = innerRadius * 2.0F;
        /* The outer disc is the largest primitive. Every corner is exactly
         * outerRadius*sqrt(2) from the entity origin; rotations preserve that
         * norm. gamma(64) bounds the complete float quaternion/PoseStack path. */
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        double extent = Math.nextUp((double) outerRadius * Math.sqrt(2.0D) * (1.0D + gamma64));
        return new AABB(
                entity.getX() - extent, entity.getY() - extent, entity.getZ() - extent,
                entity.getX() + extent, entity.getY() + extent, entity.getZ() + extent);
    }

    private static void renderCore(LegacyBlackHoleEntity entity, PoseStack poseStack, VertexConsumer consumer, float size) {
        Quaternionf camera = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        float radius = size;
        emitBillboard(poseStack.last(), consumer, camera, radius, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderSwirl(LegacyBlackHoleEntity entity, float partialTick, PoseStack poseStack,
                                    VertexConsumer consumer, float size) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        poseStack.mulPose(Axis.YP.rotationDegrees(-(entity.tickCount + partialTick) * 5.0F));
        float inner = size * 3.0F;
        for (int ring = 0; ring < 2; ring++) {
            emitHorizontalDisc(poseStack.last(), consumer, inner * (ring + 1), 0.22F / (ring + 1), 0x38, 0x98, 0xB3);
        }
        poseStack.popPose();
    }

    private static void emitHorizontalDisc(PoseStack.Pose pose, VertexConsumer consumer, float radius, float alpha,
                                           int red, int green, int blue) {
        Matrix4f matrix = pose.pose();
        vertex(consumer, pose, matrix, -radius, 0.0F, -radius, 0.0F, 1.0F, red, green, blue, alpha);
        vertex(consumer, pose, matrix, -radius, 0.0F, radius, 0.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, pose, matrix, radius, 0.0F, radius, 1.0F, 0.0F, red, green, blue, alpha);
        vertex(consumer, pose, matrix, radius, 0.0F, -radius, 1.0F, 1.0F, red, green, blue, alpha);
    }

    private static void emitBillboard(PoseStack.Pose pose, VertexConsumer consumer, Quaternionf camera,
                                      float radius, float red, float green, float blue, float alpha) {
        float qx = camera.x();
        float qy = camera.y();
        float qz = camera.z();
        float qw = camera.w();
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float ww = qw * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float xw = qx * qw;
        float zw = qz * qw;
        float yw = qy * qw;
        float inverseNorm = 1.0F / (xx + yy + zz + ww);
        float m00 = (xx - yy - zz + ww) * inverseNorm;
        float m01 = 2.0F * (xy - zw) * inverseNorm;
        float m10 = 2.0F * (xy + zw) * inverseNorm;
        float m11 = (yy - xx - zz + ww) * inverseNorm;
        float m20 = 2.0F * (xz - yw) * inverseNorm;
        float m21 = 2.0F * (yz + xw) * inverseNorm;
        float c0x = Math.fma(m00, -radius, Math.fma(m01, -radius, 0.0F));
        float c0y = Math.fma(m10, -radius, Math.fma(m11, -radius, 0.0F));
        float c0z = Math.fma(m20, -radius, Math.fma(m21, -radius, 0.0F));
        float c1x = Math.fma(m00, -radius, Math.fma(m01, radius, 0.0F));
        float c1y = Math.fma(m10, -radius, Math.fma(m11, radius, 0.0F));
        float c1z = Math.fma(m20, -radius, Math.fma(m21, radius, 0.0F));
        float c2x = Math.fma(m00, radius, Math.fma(m01, radius, 0.0F));
        float c2y = Math.fma(m10, radius, Math.fma(m11, radius, 0.0F));
        float c2z = Math.fma(m20, radius, Math.fma(m21, radius, 0.0F));
        float c3x = Math.fma(m00, radius, Math.fma(m01, -radius, 0.0F));
        float c3y = Math.fma(m10, radius, Math.fma(m11, -radius, 0.0F));
        float c3z = Math.fma(m20, radius, Math.fma(m21, -radius, 0.0F));
        Matrix4f matrix = pose.pose();
        int redByte = Math.round(red * 255.0F);
        int greenByte = Math.round(green * 255.0F);
        int blueByte = Math.round(blue * 255.0F);
        vertex(consumer, pose, matrix, c0x, c0y, c0z,
                1.0F, 1.0F, redByte, greenByte, blueByte, alpha);
        vertex(consumer, pose, matrix, c1x, c1y, c1z,
                1.0F, 0.0F, redByte, greenByte, blueByte, alpha);
        vertex(consumer, pose, matrix, c2x, c2y, c2z,
                0.0F, 0.0F, redByte, greenByte, blueByte, alpha);
        vertex(consumer, pose, matrix, c3x, c3y, c3z,
                0.0F, 1.0F, redByte, greenByte, blueByte, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f matrix,
                               double x, double y, double z, float u, float v, int red, int green, int blue, float alpha) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, Math.max(0, Math.min(255, Math.round(alpha * 255.0F))))
                .setUv(u, v)
                .setOverlay(0)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBlackHoleEntity entity) {
        return CORE;
    }
}
