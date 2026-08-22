package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class IndustrialTurbineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_Y = 1.5F;
    private static final float MODEL_FIT_SCALE = 1.50F / 7.0F;

    public IndustrialTurbineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_INDUSTRIAL_TURBINE.get().defaultBlockState();
        float rot = (System.currentTimeMillis() / 5L) % 360L;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0F), 0.0F, 1.0F, 0.0F)));
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(0.0F, -MODEL_CENTER_Y, 0.0F);
        IndustrialTurbineBlockEntityRenderer.renderParts(rot, 135.0F, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
