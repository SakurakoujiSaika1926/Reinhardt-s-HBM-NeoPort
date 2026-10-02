package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ZirnoxReactorBlock;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ZirnoxReactorBlockEntityRenderer implements LongRangeBlockEntityRenderer<ZirnoxReactorBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_zirnox_world");
    private static final AABB NORTH_BOUNDS = createZirnoxLocalBounds(Direction.NORTH, false);
    private static final AABB SOUTH_BOUNDS = createZirnoxLocalBounds(Direction.SOUTH, false);
    private static final AABB WEST_BOUNDS = createZirnoxLocalBounds(Direction.WEST, false);
    private static final AABB EAST_BOUNDS = createZirnoxLocalBounds(Direction.EAST, false);
    private static final AABB NORTH_TILTED_BOUNDS = createZirnoxLocalBounds(Direction.NORTH, true);
    private static final AABB SOUTH_TILTED_BOUNDS = createZirnoxLocalBounds(Direction.SOUTH, true);
    private static final AABB WEST_TILTED_BOUNDS = createZirnoxLocalBounds(Direction.WEST, true);
    private static final AABB EAST_TILTED_BOUNDS = createZirnoxLocalBounds(Direction.EAST, true);

    public ZirnoxReactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(ZirnoxReactorBlockEntity reactor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = reactor.getBlockState();
        Direction facing = zirnoxReactorFacing(state);
        poseStack.pushPose();
        applyZirnoxReactorPose(poseStack, facing, reactor.tilted());
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ZirnoxReactorBlockEntity blockEntity) {
        Direction facing = zirnoxReactorFacing(blockEntity.getBlockState());
        boolean tilted = blockEntity.tilted();
        AABB local = switch (facing) {
            case NORTH -> tilted ? NORTH_TILTED_BOUNDS : NORTH_BOUNDS;
            case SOUTH -> tilted ? SOUTH_TILTED_BOUNDS : SOUTH_BOUNDS;
            case WEST -> tilted ? WEST_TILTED_BOUNDS : WEST_BOUNDS;
            case EAST -> tilted ? EAST_TILTED_BOUNDS : EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("ZIRNOX reactor facing must be horizontal: " + facing);
        };
        return local.move(blockEntity.getBlockPos());
    }

    /** Exact RenderZirnox metadata mapping. */
    private static void applyZirnoxReactorPose(PoseStack poseStack, Direction facing, boolean tilted) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (tilted) {
            poseStack.translate(0.0D, -0.5D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F));
        }
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("ZIRNOX reactor facing must be horizontal: " + facing);
        }
    }

    private static AABB createZirnoxLocalBounds(Direction facing, boolean tilted) {
        PoseStack poseStack = new PoseStack();
        applyZirnoxReactorPose(poseStack, facing, tilted);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -2.5F, 0.0F, -2.5F, 2.5F, 5.0F, 2.5F);
    }

    private static Direction zirnoxReactorFacing(BlockState state) {
        if (!state.hasProperty(ZirnoxReactorBlock.FACING)) {
            throw new IllegalStateException("ZIRNOX reactor block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(ZirnoxReactorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("ZIRNOX reactor block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
