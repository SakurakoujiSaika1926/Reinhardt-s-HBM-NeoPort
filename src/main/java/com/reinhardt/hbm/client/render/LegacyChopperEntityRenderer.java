package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyChopperEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

/** Direct modern transform of RenderHunterChopper, including the original Techne geometry and rotor animation. */
public final class LegacyChopperEntityRenderer extends EntityRenderer<LegacyChopperEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/chopper.png");
    private final LegacyChopperModel model;

    public LegacyChopperEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new LegacyChopperModel(context.bakeLayer(LegacyChopperModel.LAYER));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyChopperEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 2.75D, 0.0D);
        poseStack.scale(4.0F, 4.0F, 4.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
        model.render(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyChopperEntity entity) {
        return TEXTURE;
    }
}
