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

/** Real OBJ item renderer using RenderRadarScreen's inventory transform source. */
public final class RadarScreenItemRenderer extends BlockEntityWithoutLevelRenderer {
    public RadarScreenItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RADAR_SCREEN.get().defaultBlockState();
        poseStack.pushPose();
        applyLegacyTransform(context, poseStack);
        RadarScreenBlockEntityRenderer.renderAssembly(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyLegacyTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
            // Literal RenderRadarScreen inventory and common transforms.
            poseStack.translate(0.0D, -3.0D, -0.5D);
            poseStack.scale(5.5F, 5.5F, 5.5F);
            return;
        }
        poseStack.translate(0.5D, 0.25D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.translate(0.0D, 0.0D, -0.5D);
    }
}
