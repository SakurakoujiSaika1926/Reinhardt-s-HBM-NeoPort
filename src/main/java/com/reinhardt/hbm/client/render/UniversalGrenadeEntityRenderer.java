package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.UniversalGrenadeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;

/** Renders the synchronised configured grenade stack in flight. */
public final class UniversalGrenadeEntityRenderer extends EntityRenderer<UniversalGrenadeEntity> {
    public UniversalGrenadeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(UniversalGrenadeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        Minecraft.getInstance().getItemRenderer().renderStatic(entity.grenadeStack(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(UniversalGrenadeEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB localBounds = boundsFor(entity);
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(localBounds, entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(UniversalGrenadeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    static synchronized void invalidateBounds() {
        UniversalGrenadeItemRenderer.invalidateGroundBounds();
    }

    private static AABB boundsFor(UniversalGrenadeEntity entity) {
        return UniversalGrenadeItemRenderer.groundBounds(entity.grenadeStack());
    }
}
