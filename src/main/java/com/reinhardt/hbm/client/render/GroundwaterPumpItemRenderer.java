package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.GroundwaterPumpBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
        GroundwaterPumpBlock.Kind kind = kind(stack);
        Block block = kind == GroundwaterPumpBlock.Kind.ELECTRIC ? HbmBlocks.PUMP_ELECTRIC.get() : HbmBlocks.PUMP_STEAM.get();
        BlockState state = block.defaultBlockState();
        double rot = (System.currentTimeMillis() % 3600L) * 0.1D;

        poseStack.pushPose();
        fitModelToItemCube(poseStack);
        MachineModelRenderer.orientYaw(poseStack, 45.0F);
        GroundwaterPumpBlockEntityRenderer.renderParts(
                GroundwaterPumpBlockEntityRenderer.PartSet.forKind(kind),
                rot,
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static GroundwaterPumpBlock.Kind kind(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == HbmBlocks.PUMP_ELECTRIC.get()) {
            return GroundwaterPumpBlock.Kind.ELECTRIC;
        }
        return GroundwaterPumpBlock.Kind.STEAM;
    }

    private static void fitModelToItemCube(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
    }
}
