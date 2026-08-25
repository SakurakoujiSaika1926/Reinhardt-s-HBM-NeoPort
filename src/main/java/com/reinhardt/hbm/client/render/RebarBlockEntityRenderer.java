package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.RebarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders the selected concrete from the bottom upward while the frame cures. */
public final class RebarBlockEntityRenderer implements BlockEntityRenderer<RebarBlockEntity> {
    public RebarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RebarBlockEntity rebar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float fill = Math.min(1.0F, rebar.progress() / (float) RebarBlockEntity.CONCRETE_REQUIRED);
        if (fill <= 0.0F) return;
        poseStack.pushPose();
        poseStack.scale(0.998F, fill, 0.998F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(rebar.targetState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
