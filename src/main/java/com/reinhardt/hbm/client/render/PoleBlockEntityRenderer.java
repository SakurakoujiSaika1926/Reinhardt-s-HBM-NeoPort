package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PoleSatelliteReceiverBlock;
import com.reinhardt.hbm.blockentity.PoleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact world transform used by RenderPoleTop and RenderPoleSatelliteReceiver. */
public final class PoleBlockEntityRenderer implements BlockEntityRenderer<PoleBlockEntity> {
    private static final ResourceLocation POLE_TOP_TEXTURE =
            ReinhardtsHBM.id("textures/models/pole_top.png");
    private static final ResourceLocation SATELLITE_TEXTURE =
            ReinhardtsHBM.id("textures/models/pole_satellite_receiver.png");

    private final LegacyPoleTopModel poleTop;
    private final LegacyPoleSatelliteReceiverModel satelliteReceiver;

    public PoleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.poleTop = new LegacyPoleTopModel(context.bakeLayer(LegacyPoleTopModel.LAYER));
        this.satelliteReceiver = new LegacyPoleSatelliteReceiverModel(
                context.bakeLayer(LegacyPoleSatelliteReceiverModel.LAYER));
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LegacyPoleTopModel.LAYER, LegacyPoleTopModel::createLayer);
        event.registerLayerDefinition(LegacyPoleSatelliteReceiverModel.LAYER,
                LegacyPoleSatelliteReceiverModel::createLayer);
    }

    public static void renderItem(boolean satellite, Direction facing, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                  LegacyPoleTopModel poleTop, LegacyPoleSatelliteReceiverModel satelliteReceiver) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        if (satellite) {
            poseStack.mulPose(Axis.YP.rotationDegrees(satelliteYaw(facing)));
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(
                satellite ? SATELLITE_TEXTURE : POLE_TOP_TEXTURE));
        if (satellite) {
            satelliteReceiver.render(poseStack, consumer, packedLight, packedOverlay);
        } else {
            poleTop.render(poseStack, consumer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public void render(PoleBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        boolean satellite = state.getBlock() instanceof PoleSatelliteReceiverBlock;
        Direction facing = satellite ? state.getValue(PoleSatelliteReceiverBlock.FACING) : Direction.SOUTH;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        if (satellite) {
            poseStack.mulPose(Axis.YP.rotationDegrees(satelliteYaw(facing)));
        }
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(
                satellite ? SATELLITE_TEXTURE : POLE_TOP_TEXTURE));
        if (satellite) {
            satelliteReceiver.render(poseStack, consumer, packedLight, packedOverlay);
        } else {
            poleTop.render(poseStack, consumer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static float satelliteYaw(Direction facing) {
        return switch (facing) {
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }
}
