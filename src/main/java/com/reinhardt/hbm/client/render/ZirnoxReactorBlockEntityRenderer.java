package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ZirnoxReactorBlock;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ZirnoxReactorBlockEntityRenderer implements LongRangeBlockEntityRenderer<ZirnoxReactorBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_zirnox_world");

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
        applyZirnoxReactorPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ZirnoxReactorBlockEntity blockEntity) {
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -2.0D, 0.0D, -2.0D, 3.0D, 5.0D, 3.0D);
    }

    /** Exact RenderZirnox metadata mapping. */
    private static void applyZirnoxReactorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("ZIRNOX reactor facing must be horizontal: " + facing);
        }
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
