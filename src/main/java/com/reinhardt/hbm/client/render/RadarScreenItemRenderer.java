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
    private static final float GUI_SCALE = 5.5F / 16.0F;

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
            // RenderRadarScreen's IItemRenderer was called after ItemRenderBase
            // had already applied the inventory basis. Keep that basis here;
            // otherwise the old inventory transform is displaced and enlarged.
            LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
            poseStack.translate(0.0D, -3.0D / 16.0D, -0.5D / 16.0D);
            poseStack.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
            return;
        }
        poseStack.translate(0.5D, 0.25D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.translate(0.0D, 0.0D, -0.5D);
    }
}
