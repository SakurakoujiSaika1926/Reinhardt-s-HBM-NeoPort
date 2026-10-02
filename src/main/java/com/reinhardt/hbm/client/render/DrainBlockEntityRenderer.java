package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.DrainBlock;
import com.reinhardt.hbm.blockentity.DrainBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DrainBlockEntityRenderer implements LongRangeBlockEntityRenderer<DrainBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_drain_world");
    private static final AABB[] LOCAL_BOUNDS = buildDrainBounds();

    public DrainBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(DrainBlockEntity drain, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = drain.getBlockState();
        Direction facing = drainFacing(state);

        poseStack.pushPose();
        applyDrainTransform(poseStack, facing);
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DrainBlockEntity blockEntity) {
        Direction facing = drainFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildDrainBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyDrainTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -2.5F, 0.0F, -0.5F, 0.5F, 1.0F, 0.5F);
        }
        return bounds;
    }

    private static void applyDrainTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Drain facing must be horizontal: " + facing);
        }
    }

    private static Direction drainFacing(BlockState state) {
        if (!state.hasProperty(DrainBlock.FACING)) {
            throw new IllegalStateException("Drain state is missing its owned facing property: " + state);
        }
        return state.getValue(DrainBlock.FACING);
    }
}
