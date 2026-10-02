package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RockMillBlock;
import com.reinhardt.hbm.blockentity.RockMillBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class RockMillBlockEntityRenderer implements LongRangeBlockEntityRenderer<RockMillBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_rockmill_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_rockmill_frame");
    private static final ModelResourceLocation WHEEL = MachineModelRenderer.standalone("block/machine_rockmill_wheel");

    public RockMillBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(WHEEL);
    }

    @Override
    public void render(RockMillBlockEntity mill, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = mill.getBlockState();
        Direction facing = rockMillFacing(state);
        poseStack.pushPose();
        applyRockMillTransform(poseStack, facing);
        renderPart(BASE, poseStack, buffers, state, packedLight, packedOverlay);
        if (mill.hasFrame()) {
            renderPart(FRAME, poseStack, buffers, state, packedLight, packedOverlay);
        }
        poseStack.mulPose(Axis.YN.rotationDegrees(mill.rotation(partialTick)));
        renderPart(WHEEL, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RockMillBlockEntity mill) {
        // Exact OBJ union: -2.5..2.5 around the core centre, Y 0..3.
        return new AABB(-2.0D, 0.0D, -2.0D, 3.0D, 3.0D, 3.0D)
                .move(mill.getBlockPos());
    }

    private static void applyRockMillTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Rock mill facing must be horizontal: " + facing);
        }
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource buffers, BlockState state,
                                   int packedLight, int packedOverlay) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static Direction rockMillFacing(BlockState state) {
        if (!state.hasProperty(RockMillBlock.FACING)) {
            throw new IllegalStateException("Rock mill state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RockMillBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Rock mill state has a vertical facing: " + facing);
        }
        return facing;
    }
}
