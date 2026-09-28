package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.ConveyorMovingPackage;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Matches the legacy moving package: a non-bobbing supply crate at 2x item-model scale. */
public final class ConveyorMovingPackageRenderer extends EntityRenderer<ConveyorMovingPackage> {
    private final ItemStack crate = new ItemStack(HbmBlocks.CRATE.get());
    private static volatile AABB cachedLocalBounds;
    private static volatile long cachedModelRevision = Long.MIN_VALUE;

    public ConveyorMovingPackageRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ConveyorMovingPackage entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applyRenderPose(poseStack);
        Minecraft.getInstance().getItemRenderer().renderStatic(crate, ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(ConveyorMovingPackage entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(localBounds(entity), entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    private static AABB localBounds(ConveyorMovingPackage entity) {
        long revision = BakedItemGeometrySnapshot.revision();
        AABB cached = cachedLocalBounds;
        if (cached != null && cachedModelRevision == revision) {
            return cached;
        }
        synchronized (ConveyorMovingPackageRenderer.class) {
            if (cachedLocalBounds == null || cachedModelRevision != revision) {
                PoseStack poseStack = new PoseStack();
                applyRenderPose(poseStack);
                cachedLocalBounds = BakedItemGeometrySnapshot.build(
                        new ItemStack(HbmBlocks.CRATE.get()), poseStack, BlockPos.ZERO,
                        entity.level(), ItemDisplayContext.FIXED).bounds();
                cachedModelRevision = revision;
            }
            return cachedLocalBounds;
        }
    }

    private static void applyRenderPose(PoseStack poseStack) {
        poseStack.translate(0.0D, -0.0125D, 0.0D);
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ConveyorMovingPackage entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
