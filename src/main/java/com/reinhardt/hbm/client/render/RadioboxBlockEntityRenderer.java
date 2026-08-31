package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RadioboxBlock;
import com.reinhardt.hbm.blockentity.RadioboxBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Direct port of the radiobox branch in 1.7.10 RenderDecoBlock. */
public final class RadioboxBlockEntityRenderer implements BlockEntityRenderer<RadioboxBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/block/radiobox.png");

    private final LegacyRadioboxModel model;

    public RadioboxBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LegacyRadioboxModel(context.bakeLayer(LegacyRadioboxModel.LAYER));
    }

    @Override
    public void render(RadioboxBlockEntity radiobox, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = radiobox.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw(state.getValue(RadioboxBlock.FACING))));
        poseStack.translate(0.0F, 0.0F, 1.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.render(poseStack, consumer, packedLight, packedOverlay,
                state.getValue(RadioboxBlock.ACTIVE));
        poseStack.popPose();
    }

    private static float yaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }
}
