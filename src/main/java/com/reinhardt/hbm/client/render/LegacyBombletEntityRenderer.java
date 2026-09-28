package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBombletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Literal RenderBombletTheta port (the registered old entity is Zeta). */
public final class LegacyBombletEntityRenderer extends EntityRenderer<LegacyBombletEntity> {
    private static final ResourceLocation MODEL_LOCATION = ReinhardtsHBM.id("models/entity/legacy_bomblet_theta.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/legacy_bomblet_zeta.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(MODEL_LOCATION);
    private static final double VISUAL_RADIUS = rotationSafeRadius(
            MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D) * 0.5D);

    public LegacyBombletEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyBombletEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, 0, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyBombletEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), VISUAL_RADIUS);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBombletEntity entity) {
        return TEXTURE;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
