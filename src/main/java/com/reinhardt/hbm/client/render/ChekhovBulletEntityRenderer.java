package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.ChekhovBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ChekhovBulletEntityRenderer extends EntityRenderer<ChekhovBulletEntity> {
    public ChekhovBulletEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ChekhovBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        LegacyProjectileEntityRenderer.renderRifleBullet(
                entity, entity.ammoType().standardType(), partialTick, poseStack, bufferSource, packedLight);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ChekhovBulletEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
