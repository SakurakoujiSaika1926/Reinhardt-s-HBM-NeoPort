package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class MiningLaserItemRenderer extends BlockEntityWithoutLevelRenderer {

    public MiningLaserItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // ItemRenderLibrary#machine_mining_laser in HBM 1.7.10.
            LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
            poseStack.translate(0.0D, -0.5D, 0.0D);
            poseStack.scale(3.0F, 3.0F, 3.0F);
        } else {
            poseStack.translate(0.5D, 0.25D, 0.0D);
            poseStack.scale(0.25F, 0.25F, 0.25F);
        }
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(MiningLaserBlockEntityRenderer.BASE),
                poseStack, bufferSource, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay
        );
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(MiningLaserBlockEntityRenderer.PIVOT),
                poseStack, bufferSource, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay
        );
        poseStack.translate(0.0D, -1.0D, 0.75D);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(MiningLaserBlockEntityRenderer.LASER),
                poseStack, bufferSource, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay
        );
        poseStack.popPose();
    }
}
