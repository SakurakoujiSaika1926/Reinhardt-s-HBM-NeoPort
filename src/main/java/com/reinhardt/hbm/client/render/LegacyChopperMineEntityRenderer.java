package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyChopperMineEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Direct RenderChopperMine transform and texture. */
public final class LegacyChopperMineEntityRenderer extends EntityRenderer<LegacyChopperMineEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/models/chopper_bomb.png");
    private final LegacyChopperMineModel model;

    public LegacyChopperMineEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new LegacyChopperMineModel(context.bakeLayer(LegacyChopperMineModel.LAYER));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyChopperMineEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        model.render(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyChopperMineEntity entity) {
        return TEXTURE;
    }
}
