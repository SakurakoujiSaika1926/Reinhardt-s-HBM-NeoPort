package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.LegacyGrenadeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the live grenade with the exact stick item model rather than a fallback sprite. */
public final class LegacyGrenadeEntityRenderer extends EntityRenderer<LegacyGrenadeEntity> {
    public LegacyGrenadeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyGrenadeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity.renderStack(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyGrenadeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
