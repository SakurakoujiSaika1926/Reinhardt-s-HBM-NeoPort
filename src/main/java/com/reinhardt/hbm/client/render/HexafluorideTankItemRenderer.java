package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Direct port of ItemRenderLibrary lines 492-509 for the UF6 and PuF6 tanks. */
public final class HexafluorideTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public HexafluorideTankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = (stack.is(HbmBlocks.MACHINE_PUF6_TANK.get().asItem())
                ? HbmBlocks.MACHINE_PUF6_TANK.get()
                : HbmBlocks.MACHINE_UF6_TANK.get()).defaultBlockState()
                .setValue(com.reinhardt.hbm.block.HexafluorideTankBlock.FACING, Direction.SOUTH);

        poseStack.pushPose();
        applyItemRenderBasePose(context, poseStack);
        // ItemRenderLibrary: translate(0,-4,0), scale(6), rotateY(90) around -Y.
        poseStack.translate(0.0F, -4.0F, 0.0F);
        poseStack.scale(6.0F, 6.0F, 6.0F);
        poseStack.mulPose(HexafluorideTankBlockEntityRenderer.yaw(-90.0F));
        HexafluorideTankBlockEntityRenderer.renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyItemRenderBasePose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(0.0620F, 0.0620F, 0.0620F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            return;
        }

        poseStack.translate(0.5F, 0.25F, 0.0F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }
}
