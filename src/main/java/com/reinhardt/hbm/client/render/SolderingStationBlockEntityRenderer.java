package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.SolderingStationBlock;
import com.reinhardt.hbm.blockentity.SolderingStationBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SolderingStationBlockEntityRenderer implements LongRangeBlockEntityRenderer<SolderingStationBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_soldering_station_world");
    private static final AABB NORTH_RENDER_BOUNDS = createSolderingStationLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createSolderingStationLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createSolderingStationLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createSolderingStationLocalBounds(Direction.EAST);

    public SolderingStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(SolderingStationBlockEntity solderingStation, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = solderingStation.getBlockState();
        Direction facing = solderingStationFacing(state);

        poseStack.pushPose();
        applySolderingStationPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolderingStationBlockEntity blockEntity) {
        Direction facing = solderingStationFacing(blockEntity.getBlockState());
        return solderingStationRenderBounds(blockEntity.getBlockPos(), facing);
    }

    private static Direction solderingStationFacing(BlockState state) {
        if (!state.hasProperty(SolderingStationBlock.FACING)) {
            throw new IllegalStateException("Soldering station state is missing its owned facing property: " + state);
        }
        Direction facing = state.getValue(SolderingStationBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Soldering station state has a vertical facing: " + facing);
        }
        return facing;
    }

    /** Exact RenderSolderingStation metadata mapping. */
    private static void applySolderingStationPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Soldering station facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, 0.5F);
    }

    private static AABB solderingStationRenderBounds(BlockPos pos, Direction facing) {
        AABB local = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Soldering station facing must be horizontal: " + facing);
        };
        return local.move(pos);
    }

    private static AABB createSolderingStationLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySolderingStationPose(poseStack, facing);
        // All 217 face-referenced soldering_station.obj vertices.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.0F, 0.0F, -1.0F, 1.0F, 1.5F, 1.0F);
    }
}
