package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Direct 1.7.10 RenderTurbineGas item path. */
public final class GasTurbineItemRenderer extends BlockEntityWithoutLevelRenderer {
    public GasTurbineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        switch (context) {
            case GUI -> {
                // ItemRenderBase#INVENTORY for this one renderer, followed by
                // RenderTurbineGas#renderInventory.
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                poseStack.translate(0.0F, -1.0F, 1.5F);
                poseStack.scale(2.5F, 2.5F, 2.5F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderBase#EQUIPPED.
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // ItemRenderBase#EQUIPPED_FIRST_PERSON.
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case GROUND -> {
                // ItemRenderBase#ENTITY: 1.5 * 0.25, then its Y rotation.
                poseStack.scale(0.375F, 0.375F, 0.375F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case NONE, HEAD, FIXED -> throw new IllegalArgumentException(
                    "RenderTurbineGas had no 1.7.10 ItemRenderType for " + context);
        }
        // RenderTurbineGas#renderCommon, unique to the gas turbine.
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GasTurbineBlockEntityRenderer.MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
