package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class LegacyShrapnelEntityRenderer extends EntityRenderer<LegacyShrapnelEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/shrapnel.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE);

    public LegacyShrapnelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LegacyShrapnelEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.of(new org.joml.Vector3f(1.0F, 1.0F, 1.0F).normalize())
                .rotationDegrees((entity.tickCount % 360) * 10.0F + partialTick));
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        drawCube(poseStack.last(), consumer, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawCube(PoseStack.Pose pose, VertexConsumer consumer, int light) {
        float x0 = 1.0F / 16.0F;
        float x1 = 5.0F / 16.0F;
        float y0 = -0.5F / 16.0F;
        float y1 = 3.5F / 16.0F;
        float z0 = -0.5F / 16.0F;
        float z1 = 3.5F / 16.0F;
        face(pose, consumer, light, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 4, 0, 8, 4, 0, -1, 0);
        face(pose, consumer, light, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 8, 0, 12, 4, 0, 1, 0);
        face(pose, consumer, light, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, 0, 4, 4, 8, -1, 0, 0);
        face(pose, consumer, light, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 4, 4, 8, 8, 0, 0, -1);
        face(pose, consumer, light, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 8, 4, 12, 8, 1, 0, 0);
        face(pose, consumer, light, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 12, 4, 16, 8, 0, 0, 1);
    }

    private static void face(PoseStack.Pose pose, VertexConsumer consumer, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        vertex(pose, consumer, light, ax, ay, az, u0 / 16.0F, v1 / 8.0F, nx, ny, nz);
        vertex(pose, consumer, light, bx, by, bz, u1 / 16.0F, v1 / 8.0F, nx, ny, nz);
        vertex(pose, consumer, light, cx, cy, cz, u1 / 16.0F, v0 / 8.0F, nx, ny, nz);
        vertex(pose, consumer, light, dx, dy, dz, u0 / 16.0F, v0 / 8.0F, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyShrapnelEntity entity) {
        return TEXTURE;
    }
}
