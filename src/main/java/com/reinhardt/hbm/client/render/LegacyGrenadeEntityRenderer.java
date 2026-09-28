package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.LegacyGrenadeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.Map;

/** Renders the live grenade with the exact stick item model rather than a fallback sprite. */
public final class LegacyGrenadeEntityRenderer extends EntityRenderer<LegacyGrenadeEntity> {
    private static final Map<LegacyGrenadeEntity.Kind, AABB> LOCAL_BOUNDS =
            new EnumMap<>(LegacyGrenadeEntity.Kind.class);

    public LegacyGrenadeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyGrenadeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applyRenderPose(poseStack);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity.renderStack(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyGrenadeEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB localBounds = localBounds(entity);
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(localBounds, entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    static synchronized void invalidateBounds() {
        LOCAL_BOUNDS.clear();
    }

    private static synchronized AABB localBounds(LegacyGrenadeEntity entity) {
        AABB cached = LOCAL_BOUNDS.get(entity.kind());
        if (cached != null) {
            return cached;
        }
        PoseStack poseStack = new PoseStack();
        applyRenderPose(poseStack);
        AABB bounds = BakedItemGeometrySnapshot.build(
                entity.renderStack(), poseStack, BlockPos.ZERO, entity.level(),
                ItemDisplayContext.GROUND).bounds();
        LOCAL_BOUNDS.put(entity.kind(), bounds);
        return bounds;
    }

    private static void applyRenderPose(PoseStack poseStack) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyGrenadeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
