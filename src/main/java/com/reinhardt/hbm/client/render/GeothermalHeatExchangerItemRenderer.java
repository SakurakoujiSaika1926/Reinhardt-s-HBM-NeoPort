package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class GeothermalHeatExchangerItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_X = 0.0F;
    private static final float MODEL_CENTER_Y = 6.0F;
    private static final float MODEL_CENTER_Z = 0.0F;
    private static final float MODEL_FIT_SCALE = 0.95F / 12.0F;

    public GeothermalHeatExchangerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_HEPHAESTUS.get().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
        GeothermalHeatExchangerBlockEntityRenderer.renderParts(
                0.0F, false, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
