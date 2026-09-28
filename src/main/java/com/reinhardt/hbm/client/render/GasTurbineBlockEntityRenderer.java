package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.GasTurbineBlock;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class GasTurbineBlockEntityRenderer implements LongRangeBlockEntityRenderer<GasTurbineBlockEntity> {
    static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_turbinegas_world");
    private static final AABB NORTH_RENDER_BOUNDS = createGasTurbineLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createGasTurbineLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createGasTurbineLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createGasTurbineLocalBounds(Direction.EAST);

    public GasTurbineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(GasTurbineBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turbine.getBlockState();
        Direction facing = gasTurbineFacing(state);
        poseStack.pushPose();
        // RenderTurbineGas#renderTileEntityAt translates to the block centre
        // and then applies this machine's own metadata-specific yaw table.
        applyGasTurbinePose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GasTurbineBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = gasTurbineFacing(state);
        return gasTurbineRenderBounds(blockEntity.getBlockPos(), facing);
    }

    /** Exact RenderTurbineGas metadata mapping: NORTH=2, SOUTH=3, WEST=4, EAST=5. */
    private static void applyGasTurbinePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Gas turbine facing must be horizontal: " + facing);
        }
    }

    private static Direction gasTurbineFacing(BlockState state) {
        if (!state.hasProperty(GasTurbineBlock.FACING)) {
            throw new IllegalStateException("Gas turbine state is missing its owned facing property: " + state);
        }
        Direction facing = state.getValue(GasTurbineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Gas turbine state has an invalid vertical facing: " + state);
        }
        return facing;
    }

    /** The exact old renderer pose applied to every face-referenced OBJ float vertex extent. */
    static AABB gasTurbineRenderBounds(BlockPos pos, Direction facing) {
        AABB localBounds = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Gas turbine facing must be horizontal: " + facing);
        };
        return localBounds.move(pos);
    }

    private static AABB createGasTurbineLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyGasTurbinePose(poseStack, facing);
        // Parsed from all 2,296 face-referenced vertices in turbinegas.obj.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, -0.000001F, -5.5F,
                2.281308F, 2.884829F, 4.5F);
    }
}
