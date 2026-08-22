package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PurexItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_Y = 2.5F;
    private static final float MODEL_FIT_SCALE = 1.05F / 5.0F;

    public PurexItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_PUREX.get().defaultBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(0.0F, -MODEL_CENTER_Y, 0.0F);
        render(PurexBlockEntityRenderer.BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        render(PurexBlockEntityRenderer.FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        render(PurexBlockEntityRenderer.FAN, poseStack, bufferSource, state, packedLight, packedOverlay);
        render(PurexBlockEntityRenderer.PUMP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void render(net.minecraft.client.resources.model.ModelResourceLocation location,
                               PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                               int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(location),
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
    }
}
