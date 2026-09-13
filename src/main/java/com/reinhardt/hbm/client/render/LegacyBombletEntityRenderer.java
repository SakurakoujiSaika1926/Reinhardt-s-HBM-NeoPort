package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBombletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Literal RenderBombletTheta port (the registered old entity is Zeta). */
public final class LegacyBombletEntityRenderer extends EntityRenderer<LegacyBombletEntity> {
    private static final ResourceLocation MODEL_LOCATION = ReinhardtsHBM.id("models/entity/legacy_bomblet_theta.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/legacy_bomblet_zeta.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(MODEL_LOCATION);

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
    public ResourceLocation getTextureLocation(LegacyBombletEntity entity) {
        return TEXTURE;
    }
}
