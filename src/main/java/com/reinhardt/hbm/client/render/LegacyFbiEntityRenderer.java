package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyFbiEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** FBI keeps the old ModelFBI (vanilla biped) with both arms aimed. */
public final class LegacyFbiEntityRenderer
        extends MobRenderer<LegacyFbiEntity, LegacyFbiModel> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/fbi.png");

    public LegacyFbiEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyFbiModel(context.bakeLayer(LegacyFbiModel.LAYER)), 0.5F);
    }

    @Override
    public void render(LegacyFbiEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
        model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
        model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyFbiEntity entity) {
        return TEXTURE;
    }
}
