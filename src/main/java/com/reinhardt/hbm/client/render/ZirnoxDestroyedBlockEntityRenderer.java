package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ZirnoxDestroyedBlock;
import com.reinhardt.hbm.blockentity.ZirnoxDestroyedBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ZirnoxDestroyedBlockEntityRenderer implements LongRangeBlockEntityRenderer<ZirnoxDestroyedBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/zirnox_destroyed_world");
    private static final AABB NORTH_RENDER_BOUNDS = createDestroyedZirnoxLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createDestroyedZirnoxLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createDestroyedZirnoxLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createDestroyedZirnoxLocalBounds(Direction.EAST);

    public ZirnoxDestroyedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(ZirnoxDestroyedBlockEntity reactor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = reactor.getBlockState();
        Direction facing = destroyedZirnoxFacing(state);
        poseStack.pushPose();
        applyDestroyedZirnoxPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ZirnoxDestroyedBlockEntity blockEntity) {
        Direction facing = destroyedZirnoxFacing(blockEntity.getBlockState());
        return destroyedZirnoxRenderBounds(blockEntity.getBlockPos(), facing);
    }

    private static void applyDestroyedZirnoxPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Destroyed ZIRNOX facing must be horizontal: " + facing);
        }
    }

    private static AABB destroyedZirnoxRenderBounds(BlockPos pos, Direction facing) {
        AABB local = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Destroyed ZIRNOX facing must be horizontal: " + facing);
        };
        return local.move(pos);
    }

    private static AABB createDestroyedZirnoxLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyDestroyedZirnoxPose(poseStack, facing);
        // All 963 face-referenced zirnox_destroyed.obj vertices.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -3.52335F, 0.0F, -2.911979F,
                3.150906F, 3.098903F, 3.242841F);
    }

    private static Direction destroyedZirnoxFacing(BlockState state) {
        if (!state.hasProperty(ZirnoxDestroyedBlock.FACING)) {
            throw new IllegalStateException("Destroyed ZIRNOX block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(ZirnoxDestroyedBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Destroyed ZIRNOX block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
