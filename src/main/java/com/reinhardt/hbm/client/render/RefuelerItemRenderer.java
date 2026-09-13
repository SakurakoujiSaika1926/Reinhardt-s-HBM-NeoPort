package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Exact 1.7.10 RenderRefueler inventory and common item pose. */
public final class RefuelerItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation FUELER = MachineModelRenderer.standalone("block/refueler_body");

    public RefuelerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((net.minecraft.world.item.BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        switch (context) {
            case GUI -> {
                // ItemRenderBase#INVENTORY, expressed here only for the
                // refueler, followed by RenderRefueler#renderInventory.
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                poseStack.translate(0.0F, -3.0F, 0.0F);
                poseStack.scale(6.0F, 6.0F, 6.0F);
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
                    "RenderRefueler had no 1.7.10 ItemRenderType for " + context);
        }
        // RenderRefueler#getRenderer -> renderCommon: scale is deliberately before the X translation.
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.5F, 0.0F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FUELER), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
