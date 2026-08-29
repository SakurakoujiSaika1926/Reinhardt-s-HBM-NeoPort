package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.BroadcasterBlockEntity;
import com.reinhardt.hbm.client.sound.BroadcasterClientSounds;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Keeps the legacy broadcaster's local loop alive; geometry is supplied by the block model. */
public final class BroadcasterBlockEntityRenderer implements BlockEntityRenderer<BroadcasterBlockEntity> {
    public BroadcasterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BroadcasterBlockEntity broadcaster, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BroadcasterClientSounds.tick(broadcaster);
    }

    @Override
    public boolean shouldRenderOffScreen(BroadcasterBlockEntity broadcaster) {
        return true;
    }
}
