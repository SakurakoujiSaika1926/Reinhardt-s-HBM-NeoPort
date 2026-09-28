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

public final class MicrowaveItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MicrowaveItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_MICROWAVE.get().defaultBlockState();

        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -4.0F, 4.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.translate(-2.0F, -2.0F, 1.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        // The legacy item renderer deliberately omitted the animated plate.
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(MicrowaveBlockEntityRenderer.BODY),
                poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(MicrowaveBlockEntityRenderer.WINDOW),
                poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
