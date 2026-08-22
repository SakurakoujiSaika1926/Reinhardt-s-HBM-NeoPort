package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class FluidTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_X = 0.0F;
    private static final float MODEL_CENTER_Y = 1.640625F;
    private static final float MODEL_CENTER_Z = 0.0F;
    private static final float MODEL_FIT_SCALE = 0.95F / 5.5F;

    public FluidTankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        fitModelToItemCube(poseStack);
        FluidTankBlockEntityRenderer.renderModel(fluidFromStack(stack), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void fitModelToItemCube(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
    }

    private static HbmFluidDefinition fluidFromStack(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(FluidTankBlockEntity.ITEM_DATA_KEY)) {
            return HbmFluids.none();
        }
        CompoundTag data = root.getCompound(FluidTankBlockEntity.ITEM_DATA_KEY);
        if (!data.contains("Tank")) {
            return HbmFluids.none();
        }
        HbmFluidTank tank = new HbmFluidTank(FluidTankBlockEntity.CAPACITY);
        tank.load(data.getCompound("Tank"));
        return tank.type();
    }
}
