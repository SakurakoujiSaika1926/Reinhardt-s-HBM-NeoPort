package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class GroundwaterPumpItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_X = 0.0F;
    private static final float MODEL_CENTER_Y = 2.5F;
    private static final float MODEL_CENTER_Z = 0.0F;
    private static final float MODEL_FIT_SCALE = 0.95F / 5.0F;

    public GroundwaterPumpItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (isElectricPump(stack)) {
            renderElectricPumpItem(poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderSteamPumpItem(poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderSteamPumpItem(PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.PUMP_STEAM.get().defaultBlockState();
        double rotor = (System.currentTimeMillis() % 3600L) * 0.1D;
        poseStack.pushPose();
        fitSteamPumpToItemCube(poseStack);
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        GroundwaterPumpBlockEntityRenderer.renderParts(
                new GroundwaterPumpBlockEntityRenderer.PartSet(
                        GroundwaterPumpBlockEntityRenderer.STEAM_BASE,
                        GroundwaterPumpBlockEntityRenderer.STEAM_ROTOR,
                        GroundwaterPumpBlockEntityRenderer.STEAM_ARMS,
                        GroundwaterPumpBlockEntityRenderer.STEAM_PISTON),
                rotor,
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static void renderElectricPumpItem(PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.PUMP_ELECTRIC.get().defaultBlockState();
        double rotor = (System.currentTimeMillis() % 3600L) * 0.1D;
        poseStack.pushPose();
        fitElectricPumpToItemCube(poseStack);
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        GroundwaterPumpBlockEntityRenderer.renderParts(
                new GroundwaterPumpBlockEntityRenderer.PartSet(
                        GroundwaterPumpBlockEntityRenderer.ELECTRIC_BASE,
                        GroundwaterPumpBlockEntityRenderer.ELECTRIC_ROTOR,
                        GroundwaterPumpBlockEntityRenderer.ELECTRIC_ARMS,
                        GroundwaterPumpBlockEntityRenderer.ELECTRIC_PISTON),
                rotor,
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static boolean isElectricPump(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == HbmBlocks.PUMP_ELECTRIC.get();
    }

    private static void fitSteamPumpToItemCube(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
    }

    private static void fitElectricPumpToItemCube(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
    }
}
