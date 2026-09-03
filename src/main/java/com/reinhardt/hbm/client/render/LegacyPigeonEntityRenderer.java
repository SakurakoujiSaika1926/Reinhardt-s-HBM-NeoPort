package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyPigeonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dedicated renderer for ModelPigeon, including its wing animation. */
public final class LegacyPigeonEntityRenderer extends MobRenderer<LegacyPigeonEntity, LegacyPigeonModel> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/pigeon.png");

    public LegacyPigeonEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyPigeonModel(context.bakeLayer(LegacyPigeonModel.LAYER)), 0.3F);
    }

    @Override
    public void render(LegacyPigeonEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyPigeonEntity entity) {
        return TEXTURE;
    }
}
