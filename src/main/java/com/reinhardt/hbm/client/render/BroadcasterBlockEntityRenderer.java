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
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Direct port of the corrupted-broadcaster branch in 1.7.10 RenderDecoBlock. */
public final class BroadcasterBlockEntityRenderer implements LongRangeBlockEntityRenderer<BroadcasterBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/block/broadcaster_pc.png");
    private static final AABB[] LOCAL_BOUNDS = buildLocalBounds();

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
        applyBroadcasterPose(poseStack, state.getValue(BroadcasterBlock.FACING));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BroadcasterBlockEntity broadcaster) {
        Direction facing = broadcaster.getBlockState().getValue(BroadcasterBlock.FACING);
        return LOCAL_BOUNDS[facing.ordinal()].move(broadcaster.getBlockPos());
    }

    private static AABB[] buildLocalBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            bounds[facing.ordinal()] = buildLocalBounds(facing);
        }
        return bounds;
    }

    private static AABB buildLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyBroadcasterPose(poseStack, facing);

        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(BlockPos.ZERO);
        includePart(bounds, poseStack, -7.0F, 14.0F, -4.0F, 14.0F, 10.0F, 8.0F);
        includePart(bounds, poseStack, -5.0F, 11.0F, -1.0F, 2.0F, 3.0F, 2.0F);
        includePart(bounds, poseStack, -4.5F, 0.0F, -0.5F, 1.0F, 11.0F, 1.0F);
        includePart(bounds, poseStack, 2.0F, 12.0F, -0.5F, 3.0F, 2.0F, 1.0F);
        return bounds.build();
    }

    /** Mirrors ModelPart child translation followed by its baked /16 cube vertices. */
    private static void includePart(RenderBounds.FloatBuilder bounds, PoseStack poseStack,
                                    float pivotX, float pivotY, float pivotZ,
                                    float width, float height, float depth) {
        poseStack.pushPose();
        poseStack.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);
        bounds.includeBox(poseStack, 0.0F, 0.0F, 0.0F,
                width / 16.0F, height / 16.0F, depth / 16.0F);
        poseStack.popPose();
    }

    private static void applyBroadcasterPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        switch (facing) {
            case NORTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Broadcaster facing must be horizontal: " + facing);
        }
    }
}
