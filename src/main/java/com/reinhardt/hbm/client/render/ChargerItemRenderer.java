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

/** Direct 1.7.10 ItemRenderLibrary path for the charger item. */
public final class ChargerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public ChargerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        switch (context) {
            case GUI -> {
                // ItemRenderBase#INVENTORY for the charger alone, followed
                // by ItemRenderLibrary#charger's renderInventory.
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                poseStack.translate(0.0F, -7.0F, 0.0F);
                poseStack.scale(10.0F, 10.0F, 10.0F);
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
                    "ItemRenderLibrary#charger had no 1.7.10 ItemRenderType for " + context);
        }
        // ItemRenderLibrary#charger's renderCommon, which renders no arms or lamp.
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.5F, 0.0F, 0.0F);
        ChargerBlockEntityRenderer.renderItemAssembly(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
