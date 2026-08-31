package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.BroadcasterBlock;
import com.reinhardt.hbm.blockentity.BroadcasterBlockEntity;
import com.reinhardt.hbm.client.sound.BroadcasterClientSounds;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Direct port of the corrupted-broadcaster branch in 1.7.10 RenderDecoBlock. */
public final class BroadcasterBlockEntityRenderer implements BlockEntityRenderer<BroadcasterBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/block/broadcaster_pc.png");

    private final LegacyBroadcasterModel model;

    public BroadcasterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LegacyBroadcasterModel(context.bakeLayer(LegacyBroadcasterModel.LAYER));
    }

    @Override
    public void render(BroadcasterBlockEntity broadcaster, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BroadcasterClientSounds.tick(broadcaster);
        BlockState state = broadcaster.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw(state.getValue(BroadcasterBlock.FACING))));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BroadcasterBlockEntity broadcaster) {
        return true;
    }

    private static float yaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
