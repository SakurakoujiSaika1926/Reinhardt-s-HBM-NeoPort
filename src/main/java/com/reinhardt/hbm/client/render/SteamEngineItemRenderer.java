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

public final class SteamEngineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_Y = 1.28125F;
    private static final float MODEL_FIT_SCALE = 1.35F / 7.0F;

    public SteamEngineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_STEAM_ENGINE.get().defaultBlockState();
        float rot = (System.currentTimeMillis() % 3600L) * 0.1F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0F), 0.0F, 1.0F, 0.0F)));
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(0.0F, -MODEL_CENTER_Y, 0.0F);
        SteamEngineBlockEntityRenderer.renderParts(rot, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
