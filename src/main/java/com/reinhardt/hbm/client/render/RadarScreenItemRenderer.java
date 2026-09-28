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
            // Exact ItemRenderBase#INVENTORY transform, with its pixel-space
            // translation converted once (and only once) to model units.
            poseStack.translate(0.5D, 0.625D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            // radar_screen.obj exposes its display on +X. ItemRenderBase's
            // negative inventory scale reverses that axis, so the modern GUI
            // camera needs the opposite Y basis to keep the display visible.
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(-1.0F / 16.0F, -1.0F / 16.0F, -1.0F / 16.0F);
            poseStack.translate(0.0D, -3.0D, 0.0D);
            poseStack.scale(5.5F, 5.5F, 5.5F);
            poseStack.translate(0.0D, 0.0D, -0.5D);
            return;
        }

        if (context == ItemDisplayContext.GROUND) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        } else {
            poseStack.translate(0.5D, 0.25D, 0.0D);
        }
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            // radar_screen.obj's display faces +X. In the modern first-person
            // camera, -90 degrees turns that authored face toward +Z/the player.
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.translate(0.0D, 0.0D, -0.5D);
    }
}
