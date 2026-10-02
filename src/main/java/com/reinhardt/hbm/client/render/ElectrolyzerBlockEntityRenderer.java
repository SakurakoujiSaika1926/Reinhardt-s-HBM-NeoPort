package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ElectrolyzerBlock;
import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ElectrolyzerBlockEntityRenderer implements LongRangeBlockEntityRenderer<ElectrolyzerBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_electrolyser_world");
    private static final AABB NORTH_RENDER_BOUNDS = createElectrolyzerLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createElectrolyzerLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createElectrolyzerLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createElectrolyzerLocalBounds(Direction.EAST);

    public ElectrolyzerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(ElectrolyzerBlockEntity electrolyzer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = electrolyzer.getBlockState();
        Direction facing = electrolyzerFacing(state);
        poseStack.pushPose();
        applyElectrolyzerPose(poseStack, facing);
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ElectrolyzerBlockEntity blockEntity) {
        Direction facing = electrolyzerFacing(blockEntity.getBlockState());
        return electrolyzerRenderBounds(blockEntity.getBlockPos(), facing);
    }

    /** Exact RenderElectrolyser metadata rotation followed by its explicit extra 180-degree rotation. */
    private static void applyElectrolyzerPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Electrolyzer facing must be horizontal: " + facing);
        }
    }

    private static Direction electrolyzerFacing(BlockState state) {
        if (!state.hasProperty(ElectrolyzerBlock.FACING)) {
            throw new IllegalStateException("Electrolyzer state is missing its owned facing property: " + state);
        }
        Direction facing = state.getValue(ElectrolyzerBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Electrolyzer state has an invalid vertical facing: " + state);
        }
        return facing;
    }

    static AABB electrolyzerRenderBounds(BlockPos pos, Direction facing) {
        AABB localBounds = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Electrolyzer facing must be horizontal: " + facing);
        };
        return localBounds.move(pos);
    }

    private static AABB createElectrolyzerLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyElectrolyzerPose(poseStack, facing);
        // Exact extent of all 1,482 face-used electrolyser.obj vertices.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.555902F, 0.0F, -5.7509F,
                3.5F, 4.207107F, 5.7509F);
    }
}
