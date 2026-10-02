package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RotaryFurnaceBlock;
import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class RotaryFurnaceBlockEntityRenderer implements LongRangeBlockEntityRenderer<RotaryFurnaceBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation FURNACE_MODEL = MachineModelRenderer.standalone("block/machine_rotary_furnace_furnace");
    private static final ModelResourceLocation PISTON_MODEL = MachineModelRenderer.standalone("block/machine_rotary_furnace_piston");
    private static final AABB NORTH_BOUNDS = createRotaryFurnaceLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_BOUNDS = createRotaryFurnaceLocalBounds(Direction.SOUTH);
    private static final AABB WEST_BOUNDS = createRotaryFurnaceLocalBounds(Direction.WEST);
    private static final AABB EAST_BOUNDS = createRotaryFurnaceLocalBounds(Direction.EAST);

    public RotaryFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FURNACE_MODEL);
        event.register(PISTON_MODEL);
    }

    @Override
    public void render(RotaryFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        poseStack.pushPose();
        applyRotaryFurnacePose(poseStack, rotaryFurnaceFacing(state));
        STATIC_MESHES.render(FURNACE_MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, furnace.pistonOffset(partialTick), 0.0D);
        STATIC_MESHES.render(PISTON_MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RotaryFurnaceBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = rotaryFurnaceFacing(state);
        // The piston is [-.5,2,1]..[.5,3.25,2] and its complete authored
        // -1..0 Y travel remains strictly inside the furnace's face-used
        // baked-float envelope below, so the union equals the furnace box.
        AABB local = switch (facing) {
            case NORTH -> NORTH_BOUNDS;
            case SOUTH -> SOUTH_BOUNDS;
            case WEST -> WEST_BOUNDS;
            case EAST -> EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Rotary furnace facing must be horizontal: " + facing);
        };
        return local.move(blockEntity.getBlockPos());
    }

    private static void applyRotaryFurnacePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Rotary furnace facing must be horizontal: " + facing);
        }
    }

    private static AABB createRotaryFurnaceLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyRotaryFurnacePose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -2.5F, 1.5F, 5.0F, 2.5F);
    }

    private static Direction rotaryFurnaceFacing(BlockState state) {
        if (!state.hasProperty(RotaryFurnaceBlock.FACING)) {
            throw new IllegalStateException("Rotary furnace block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RotaryFurnaceBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Rotary furnace block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
