package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PoleSatelliteReceiverBlock;
import com.reinhardt.hbm.blockentity.PoleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Quaternionf;

/** Exact world transform used by RenderPoleTop and RenderPoleSatelliteReceiver. */
public final class PoleBlockEntityRenderer implements LongRangeBlockEntityRenderer<PoleBlockEntity> {
    private static final ResourceLocation POLE_TOP_TEXTURE =
            ReinhardtsHBM.id("textures/models/pole_top.png");
    private static final ResourceLocation SATELLITE_TEXTURE =
            ReinhardtsHBM.id("textures/models/pole_satellite_receiver.png");
    private static final AABB POLE_TOP_LOCAL_BOUNDS = buildPoleTopLocalBounds();
    private static final AABB[] SATELLITE_LOCAL_BOUNDS = buildSatelliteLocalBounds();

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

    public static void renderPoleTopItem(ItemDisplayContext context, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                         LegacyPoleTopModel poleTop) {
        poseStack.pushPose();
        // ItemRenderer applies (-0.5, -0.5, -0.5) before entering a BEWLR.
        // Cancel that API-space shift before applying the authored 1.7.10 transforms.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(POLE_TOP_TEXTURE));
        poleTop.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public static void renderSatelliteReceiverItem(ItemDisplayContext context, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                                   LegacyPoleSatelliteReceiverModel satelliteReceiver) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applySatelliteReceiverItemTransform(context, poseStack);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SATELLITE_TEXTURE));
        satelliteReceiver.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applySatelliteReceiverItemTransform(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GROUND) {
            // ItemRenderSatelliteReceiver: ENTITY.
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, -1.0F, 0.0F);
            return;
        }
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // ItemRenderSatelliteReceiver: EQUIPPED.
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.8F, -0.3F, 0.2F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            return;
        }
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // ItemRenderSatelliteReceiver: EQUIPPED_FIRST_PERSON.
            poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
            poseStack.translate(-0.6F, -0.6F, -0.1F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            return;
        }
        throw new IllegalArgumentException(
                "ItemRenderSatelliteReceiver had no 1.7.10 3D render type for " + context);
    }

    @Override
    public void render(PoleBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (state.getBlock() instanceof PoleSatelliteReceiverBlock) {
            renderSatelliteReceiverWorld(state, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderPoleTopWorld(poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderPoleTopWorld(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyPoleTopWorldPose(poseStack);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(POLE_TOP_TEXTURE));
        poleTop.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderSatelliteReceiverWorld(BlockState state, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySatelliteReceiverWorldPose(poseStack, state.getValue(PoleSatelliteReceiverBlock.FACING));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SATELLITE_TEXTURE));
        satelliteReceiver.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PoleBlockEntity entity) {
        BlockState state = entity.getBlockState();
        AABB localBounds = state.getBlock() instanceof PoleSatelliteReceiverBlock
                ? SATELLITE_LOCAL_BOUNDS[state.getValue(PoleSatelliteReceiverBlock.FACING).ordinal()]
                : POLE_TOP_LOCAL_BOUNDS;
        return localBounds.move(entity.getBlockPos());
    }

    private static AABB[] buildSatelliteLocalBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            bounds[facing.ordinal()] = buildSatelliteLocalBound(facing);
        }
        return bounds;
    }

    private static AABB buildPoleTopLocalBounds() {
        PoseStack poseStack = new PoseStack();
        applyPoleTopWorldPose(poseStack);

        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(BlockPos.ZERO);
        includePoleTop(bounds, poseStack);
        return bounds.build();
    }

    private static AABB buildSatelliteLocalBound(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySatelliteReceiverWorldPose(poseStack, facing);

        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(BlockPos.ZERO);
        includeSatellite(bounds, poseStack);
        return bounds.build();
    }

    private static void applyPoleTopWorldPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }

    private static void applySatelliteReceiverWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        switch (facing) {
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Satellite receiver requires a horizontal facing: " + facing);
        }
    }

    private static void includePoleTop(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
        includePart(bounds, poseStack, -6.0F, 16.0F, -6.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 12.0F, 8.0F, 12.0F);
        includePart(bounds, poseStack, -8.0F, 4.0F, -8.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 4.0F, 16.0F, 4.0F);
        includePart(bounds, poseStack, 4.0F, 4.0F, -8.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 4.0F, 16.0F, 4.0F);
        includePart(bounds, poseStack, -8.0F, 4.0F, 4.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 4.0F, 16.0F, 4.0F);
        includePart(bounds, poseStack, 4.0F, 4.0F, 4.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 4.0F, 16.0F, 4.0F);
        includePart(bounds, poseStack, -2.0F, 14.0F, -2.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 4.0F, 2.0F, 4.0F);
    }

    private static void includeSatellite(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
        includePart(bounds, poseStack, -6.0F, 8.0F, -6.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 12.0F, 16.0F, 12.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                3.0F, 9.0F, -8.0F, 11.0F, 17.0F, -6.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                3.0F, 7.0F, -10.0F, 11.0F, 9.0F, -7.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                1.0F, 9.0F, -10.0F, 3.0F, 17.0F, -7.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                11.0F, 9.0F, -10.0F, 13.0F, 17.0F, -7.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                3.0F, 17.0F, -10.0F, 11.0F, 19.0F, -7.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                6.0F, 12.0F, -11.0F, 8.0F, 14.0F, -8.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                6.5F, 12.5F, -14.0F, 7.5F, 13.5F, -11.0F);
        includePart(bounds, poseStack, -3.0F, 6.0F, 0.0F, -0.2617994F, -0.4363323F, 0.0F,
                6.0F, 12.0F, -16.0F, 8.0F, 14.0F, -14.0F);
    }

    /** Mirrors ModelPart.translateAndRotate and Cube's baked /16 coordinates. */
    private static void includePart(RenderBounds.FloatBuilder bounds, PoseStack poseStack,
                                    float pivotX, float pivotY, float pivotZ,
                                    float xRot, float yRot, float zRot,
                                    float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ) {
        poseStack.pushPose();
        poseStack.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);
        if (xRot != 0.0F || yRot != 0.0F || zRot != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationZYX(zRot, yRot, xRot));
        }
        bounds.includeBox(poseStack,
                minX / 16.0F, minY / 16.0F, minZ / 16.0F,
                maxX / 16.0F, maxY / 16.0F, maxZ / 16.0F);
        poseStack.popPose();
    }

}
