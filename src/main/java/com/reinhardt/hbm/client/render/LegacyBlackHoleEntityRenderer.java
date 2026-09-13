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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

    private static void renderCore(LegacyBlackHoleEntity entity, PoseStack poseStack, VertexConsumer consumer, float size) {
        Quaternionf camera = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        float radius = size;
        emitBillboard(poseStack.last(), consumer, camera, Vec3.ZERO, radius, 1.0F, 1.0F, 1.0F, 1.0F);
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

    private static void emitBillboard(PoseStack.Pose pose, VertexConsumer consumer, Quaternionf camera, Vec3 center,
                                      float radius, float red, float green, float blue, float alpha) {
        Vec3[] corners = {
                rotate(new Vec3(-radius, -radius, 0.0D), camera).add(center),
                rotate(new Vec3(-radius, radius, 0.0D), camera).add(center),
                rotate(new Vec3(radius, radius, 0.0D), camera).add(center),
                rotate(new Vec3(radius, -radius, 0.0D), camera).add(center)
        };
        Matrix4f matrix = pose.pose();
        vertex(consumer, pose, matrix, corners[0].x, corners[0].y, corners[0].z, 1.0F, 1.0F, Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), alpha);
        vertex(consumer, pose, matrix, corners[1].x, corners[1].y, corners[1].z, 1.0F, 0.0F, Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), alpha);
        vertex(consumer, pose, matrix, corners[2].x, corners[2].y, corners[2].z, 0.0F, 0.0F, Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), alpha);
        vertex(consumer, pose, matrix, corners[3].x, corners[3].y, corners[3].z, 0.0F, 1.0F, Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), alpha);
    }

    private static Vec3 rotate(Vec3 value, Quaternionf rotation) {
        Vector3f vector = new Vector3f((float) value.x, (float) value.y, (float) value.z).rotate(rotation);
        return new Vec3(vector.x, vector.y, vector.z);
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
