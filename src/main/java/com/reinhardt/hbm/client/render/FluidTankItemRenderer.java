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
    private static final float DAMAGED_MODEL_CENTER_X = 0.080749F;
    private static final float DAMAGED_MODEL_CENTER_Y = 1.6346425F;
    private static final float DAMAGED_MODEL_CENTER_Z = -0.0256585F;
    private static final float DAMAGED_MODEL_FIT_SCALE = 0.95F / 5.661498F;

    public FluidTankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemTankState state = stateFromStack(stack);
        poseStack.pushPose();
        fitModelToItemCube(poseStack, state.damaged());
        FluidTankBlockEntityRenderer.renderModel(
                state.fluid(), state.damaged(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void fitModelToItemCube(PoseStack poseStack, boolean damaged) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        float scale = damaged ? DAMAGED_MODEL_FIT_SCALE : MODEL_FIT_SCALE;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(
                -(damaged ? DAMAGED_MODEL_CENTER_X : MODEL_CENTER_X),
                -(damaged ? DAMAGED_MODEL_CENTER_Y : MODEL_CENTER_Y),
                -(damaged ? DAMAGED_MODEL_CENTER_Z : MODEL_CENTER_Z));
    }

    private static ItemTankState stateFromStack(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(FluidTankBlockEntity.ITEM_DATA_KEY)) {
            return new ItemTankState(HbmFluids.none(), false);
        }
        CompoundTag data = root.getCompound(FluidTankBlockEntity.ITEM_DATA_KEY);
        boolean damaged = data.getBoolean("Exploded");
        if (!data.contains("Tank")) {
            return new ItemTankState(HbmFluids.none(), damaged);
        }
        HbmFluidTank tank = new HbmFluidTank(FluidTankBlockEntity.CAPACITY);
        tank.load(data.getCompound("Tank"));
        return new ItemTankState(tank.type(), damaged);
    }

    private record ItemTankState(HbmFluidDefinition fluid, boolean damaged) {
    }
}
