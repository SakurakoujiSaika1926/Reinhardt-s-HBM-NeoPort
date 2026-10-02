package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.LegacyDisperserCanisterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Vanilla thrown-item presentation with a finite envelope derived from the actual baked item model. */
public final class LegacyDisperserCanisterEntityRenderer extends EntityRenderer<LegacyDisperserCanisterEntity> {
    private static final double MIN_CAMERA_DISTANCE_SQUARED = 12.25D;
    private static final List<BakedItemGeometrySnapshot.Result> MODEL_BOUNDS = new ArrayList<>();
    private final ItemRenderer itemRenderer;

    public LegacyDisperserCanisterEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyDisperserCanisterEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 2
                && entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < MIN_CAMERA_DISTANCE_SQUARED) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyDisperserCanisterEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = modelRadius(entity);
        AABB bounds = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    static synchronized void invalidateBounds() {
        MODEL_BOUNDS.clear();
    }

    private static synchronized double modelRadius(LegacyDisperserCanisterEntity entity) {
        ItemStack stack = entity.getItem();
        for (BakedItemGeometrySnapshot.Result cached : MODEL_BOUNDS) {
            if (cached.matches(stack)) {
                return cached.maximumRadius();
            }
        }
        BakedItemGeometrySnapshot.Result snapshot = BakedItemGeometrySnapshot.build(
                stack, new PoseStack(), BlockPos.ZERO, entity.level(), ItemDisplayContext.GROUND);
        MODEL_BOUNDS.add(snapshot);
        return snapshot.maximumRadius();
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDisperserCanisterEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
