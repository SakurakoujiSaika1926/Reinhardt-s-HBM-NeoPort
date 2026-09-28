package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Renders armed explosives as their exact block rather than vanilla TNT. */
public final class TimedExplosiveEntityRenderer extends EntityRenderer<TimedExplosiveEntity> {
    private static final Map<TimedExplosiveEntity.Kind, AABB> LOCAL_BOUNDS =
            new EnumMap<>(TimedExplosiveEntity.Kind.class);

    public TimedExplosiveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
    }

    @Override
    public void render(TimedExplosiveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BlockState renderState = renderState(entity.kind());
        poseStack.pushPose();
        applyRenderPose(poseStack);
        net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                renderState,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(TimedExplosiveEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB localBounds = localBounds(entity.kind());
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(localBounds, entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    static synchronized void invalidateBounds() {
        LOCAL_BOUNDS.clear();
    }

    private static BlockState renderState(TimedExplosiveEntity.Kind kind) {
        return switch (kind) {
            case DYNAMITE -> HbmBlocks.DYNAMITE.get().defaultBlockState();
            case TNT -> HbmBlocks.TNT_NTM.get().defaultBlockState();
            case C4 -> HbmBlocks.C4.get().defaultBlockState();
            case SEMTEX -> HbmBlocks.SEMTEX.get().defaultBlockState();
            case FISSURE -> HbmBlocks.FISSURE_BOMB.get().defaultBlockState();
            case DET_CORD -> HbmBlocks.DET_CORD.get().defaultBlockState();
            case DET_CHARGE -> HbmBlocks.DET_CHARGE.get().defaultBlockState();
            case DET_NUKE -> HbmBlocks.DET_NUKE.get().defaultBlockState();
            case DET_MINER -> HbmBlocks.DET_MINER.get().defaultBlockState();
            case RED_BARREL -> HbmBlocks.RED_BARREL.get().defaultBlockState();
            case PINK_BARREL -> HbmBlocks.PINK_BARREL.get().defaultBlockState();
            case LOX_BARREL -> HbmBlocks.LOX_BARREL.get().defaultBlockState();
            case TAINT_BARREL -> HbmBlocks.TAINT_BARREL.get().defaultBlockState();
            case YELLOW_BARREL -> HbmBlocks.YELLOW_BARREL.get().defaultBlockState();
        };
    }

    private static synchronized AABB localBounds(TimedExplosiveEntity.Kind kind) {
        AABB cached = LOCAL_BOUNDS.get(kind);
        if (cached != null) {
            return cached;
        }
        PoseStack poseStack = new PoseStack();
        applyRenderPose(poseStack);
        AABB bounds = BakedBlockGeometrySnapshot.build(
                renderState(kind), List.of(poseStack), BlockPos.ZERO).bounds();
        LOCAL_BOUNDS.put(kind, bounds);
        return bounds;
    }

    private static void applyRenderPose(PoseStack poseStack) {
        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    @Override
    public ResourceLocation getTextureLocation(TimedExplosiveEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
