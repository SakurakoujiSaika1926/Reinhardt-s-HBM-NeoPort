package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.WoodBurnerBlock;
import com.reinhardt.hbm.blockentity.WoodBurnerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class WoodBurnerBlockEntityRenderer implements LongRangeBlockEntityRenderer<WoodBurnerBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_wood_burner_world");
    private static final AABB NORTH_RENDER_BOUNDS = createWoodBurnerLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createWoodBurnerLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createWoodBurnerLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createWoodBurnerLocalBounds(Direction.EAST);

    public WoodBurnerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(WoodBurnerBlockEntity burner, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = burner.getBlockState();
        Direction facing = woodBurnerFacing(state);

        poseStack.pushPose();
        applyWoodBurnerPose(poseStack, facing);
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WoodBurnerBlockEntity blockEntity) {
        Direction facing = woodBurnerFacing(blockEntity.getBlockState());
        return woodBurnerRenderBounds(blockEntity.getBlockPos(), facing);
    }

    private static Direction woodBurnerFacing(BlockState state) {
        if (!state.hasProperty(WoodBurnerBlock.FACING)) {
            throw new IllegalStateException("Wood burner state is missing its owned facing property: " + state);
        }
        Direction facing = state.getValue(WoodBurnerBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Wood burner state has a vertical facing: " + facing);
        }
        return facing;
    }

    /** Exact RenderWoodBurner metadata mapping. */
    private static void applyWoodBurnerPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Wood burner facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static AABB woodBurnerRenderBounds(BlockPos pos, Direction facing) {
        AABB local = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Wood burner facing must be horizontal: " + facing);
        };
        return local.move(pos);
    }

    private static AABB createWoodBurnerLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyWoodBurnerPose(poseStack, facing);
        // All 240 face-referenced wood_burner.obj vertices.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -0.724444F, 0.0F, -1.0F,
                0.724444F, 3.9375F, 1.0F);
    }
}
