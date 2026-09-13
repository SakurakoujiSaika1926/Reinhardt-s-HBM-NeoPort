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
import net.minecraft.world.item.ItemDisplayContext;
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

    public static void renderItem(boolean satellite, ItemDisplayContext context, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                  LegacyPoleTopModel poleTop, LegacyPoleSatelliteReceiverModel satelliteReceiver) {
        poseStack.pushPose();
        // ItemRenderer applies (-0.5, -0.5, -0.5) before entering a BEWLR.
        // Cancel that API-space shift before applying the authored 1.7.10 transforms.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (satellite) {
            applySatelliteItemTransform(context, poseStack);
        } else {
            // The pole-top item is outside this correction; retain its existing path.
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0F, -1.0F, 0.0F);
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

    private static void applySatelliteItemTransform(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GROUND -> {
                // ItemRenderSatelliteReceiver: ENTITY.
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(0.0F, -1.0F, 0.0F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderSatelliteReceiver: EQUIPPED.
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.translate(0.8F, -0.3F, 0.2F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // ItemRenderSatelliteReceiver: EQUIPPED_FIRST_PERSON.
                poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
                poseStack.translate(-0.6F, -0.6F, -0.1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case GUI, NONE, HEAD, FIXED -> throw new IllegalArgumentException(
                    "ItemRenderSatelliteReceiver had no 1.7.10 3D render type for " + context);
        }
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
        // ModelPart already converts the legacy pixel coordinates to model units.
        // Adding another 1/16 here would shrink the old model a second time.
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
