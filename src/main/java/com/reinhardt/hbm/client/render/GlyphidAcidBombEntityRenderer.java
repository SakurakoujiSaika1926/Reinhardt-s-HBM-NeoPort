package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.GlyphidAcidBombEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Direct equivalent of the 1.7.10 RenderSnowball(Items.slime_ball). */
public final class GlyphidAcidBombEntityRenderer extends EntityRenderer<GlyphidAcidBombEntity> {
    private static final ItemStack RENDER_STACK = new ItemStack(Items.SLIME_BALL);
    private static AABB localBounds;

    public GlyphidAcidBombEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(GlyphidAcidBombEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applyRenderPose(poseStack);
        Minecraft.getInstance().getItemRenderer().renderStatic(RENDER_STACK,
                ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(GlyphidAcidBombEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = localBounds(entity);
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(bounds, entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    static synchronized void invalidateBounds() {
        localBounds = null;
    }

    private static synchronized AABB localBounds(GlyphidAcidBombEntity entity) {
        if (localBounds == null) {
            PoseStack poseStack = new PoseStack();
            applyRenderPose(poseStack);
            localBounds = BakedItemGeometrySnapshot.build(
                    RENDER_STACK, poseStack, BlockPos.ZERO, entity.level(),
                    ItemDisplayContext.GROUND).bounds();
        }
        return localBounds;
    }

    private static void applyRenderPose(PoseStack poseStack) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphidAcidBombEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
