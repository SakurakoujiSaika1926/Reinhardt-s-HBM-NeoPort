package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class LandmineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation AP = model("block/mine_ap_grass_world");
    private static final ModelResourceLocation HE = model("block/mine_he_world");
    private static final ModelResourceLocation SHRAPNEL = model("block/mine_shrap_world");
    private static final ModelResourceLocation NUCLEAR = model("block/mine_fat_world");
    private static final ModelResourceLocation NAVAL = model("block/mine_naval_world");

    public LandmineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        poseStack.pushPose();
        applyLegacyBaseTransform(context, poseStack);
        if (block == HbmBlocks.MINE_AP.get()) {
            applyApTransform(context, poseStack);
            render(AP, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (block == HbmBlocks.MINE_HE.get()) {
            applyHeTransform(context, poseStack);
            render(HE, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (block == HbmBlocks.MINE_SHRAP.get()) {
            applyApTransform(context, poseStack);
            render(SHRAPNEL, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (block == HbmBlocks.MINE_NAVAL.get()) {
            applyNavalTransform(context, poseStack);
            render(NAVAL, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (block == HbmBlocks.MINE_FAT.get()) {
            applyNuclearTransform(context, poseStack);
            render(NUCLEAR, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void applyLegacyBaseTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5D, 0.625D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(-1.0F / 16.0F, -1.0F / 16.0F, -1.0F / 16.0F);
            return;
        }
        if (context == ItemDisplayContext.GROUND) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        } else {
            poseStack.translate(0.5D, 0.25D, 0.0D);
        }
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void applyApTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.scale(8.0F, 8.0F, 8.0F);
        }
        poseStack.scale(1.25F, 1.25F, 1.25F);
    }

    private static void applyHeTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.scale(6.0F, 6.0F, 6.0F);
        } else {
            poseStack.translate(0.25D, 0.625D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0F));
        }
        poseStack.scale(4.0F, 4.0F, 4.0F);
    }

    private static void applyNavalTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0D, 2.0D, -1.0D);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
    }

    private static void applyNuclearTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0D, -1.0D, 0.0D);
            poseStack.scale(7.0F, 7.0F, 7.0F);
        }
        poseStack.translate(0.25D, 0.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                               BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                state, packedLight, packedOverlay);
    }
}
