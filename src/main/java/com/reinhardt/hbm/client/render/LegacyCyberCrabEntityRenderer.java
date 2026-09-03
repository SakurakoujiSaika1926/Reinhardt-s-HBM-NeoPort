package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Dedicated renderer for the original twenty-box Cyber Crab model. */
public final class LegacyCyberCrabEntityRenderer
        extends MobRenderer<LegacyCyberCrabEntity, LegacyCyberCrabModel> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/crab.png");

    public LegacyCyberCrabEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyCyberCrabModel(context.bakeLayer(LegacyCyberCrabModel.LAYER)), 1.0F);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyCyberCrabEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(LegacyCyberCrabEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        model.setupAnim(entity, entity.walkAnimation.position(partialTick),
                entity.walkAnimation.speed(partialTick), entity.tickCount + partialTick,
                entity.getYRot(), entity.getXRot());
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
