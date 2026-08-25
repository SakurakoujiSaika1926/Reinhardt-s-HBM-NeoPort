package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Renders armed explosives as their exact block rather than vanilla TNT. */
public final class TimedExplosiveEntityRenderer extends EntityRenderer<TimedExplosiveEntity> {
    public TimedExplosiveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
    }

    @Override
    public void render(TimedExplosiveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BlockState renderState = switch (entity.kind()) {
            case DYNAMITE -> HbmBlocks.DYNAMITE.get().defaultBlockState();
            case TNT -> HbmBlocks.TNT_NTM.get().defaultBlockState();
            case C4 -> HbmBlocks.C4.get().defaultBlockState();
            case SEMTEX -> HbmBlocks.SEMTEX.get().defaultBlockState();
            case FISSURE -> HbmBlocks.FISSURE_BOMB.get().defaultBlockState();
        };
        poseStack.pushPose();
        poseStack.translate(-0.5D, -0.5D, -0.5D);
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
    public ResourceLocation getTextureLocation(TimedExplosiveEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
