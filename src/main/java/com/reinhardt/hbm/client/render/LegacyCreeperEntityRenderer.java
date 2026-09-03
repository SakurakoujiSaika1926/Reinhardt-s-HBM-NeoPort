package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** The five 1.7.10 creepers use the vanilla creeper model with their own skin. */
public final class LegacyCreeperEntityRenderer<T extends LegacyCreeperEntity>
        extends MobRenderer<T, CreeperModel<T>> {
    private final ResourceLocation texture;
    private final ResourceLocation poweredTexture;

    public LegacyCreeperEntityRenderer(EntityRendererProvider.Context context,
                                        String textureName, String poweredTextureName) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        texture = ReinhardtsHBM.id("textures/entity/" + textureName + ".png");
        poweredTexture = ReinhardtsHBM.id("textures/entity/" + poweredTextureName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (!entity.isPowered()) {
            return;
        }
        poseStack.pushPose();
        float scroll = (entity.tickCount + partialTick) * 0.01F;
        model.prepareMobModel(entity, entity.walkAnimation.position(partialTick),
                entity.walkAnimation.speed(partialTick), partialTick);
        model.setupAnim(entity, entity.walkAnimation.position(partialTick),
                entity.walkAnimation.speed(partialTick), entity.tickCount + partialTick,
                entity.getYRot(), entity.getXRot());
        model.renderToBuffer(poseStack, bufferSource.getBuffer(
                        RenderType.energySwirl(poweredTexture, scroll % 1.0F, scroll % 1.0F)),
                0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
