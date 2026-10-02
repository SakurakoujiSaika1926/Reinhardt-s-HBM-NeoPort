package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.MachineBlastFurnaceBlock;
import com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class MachineBlastFurnaceBlockEntityRenderer implements LongRangeBlockEntityRenderer<MachineBlastFurnaceBlockEntity> {
    private static final ModelResourceLocation OFF = MachineModelRenderer.standalone("block/machine_blast_furnace");
    private static final ModelResourceLocation ON = MachineModelRenderer.standalone("block/machine_blast_furnace");
    private static final AABB NORTH_BOUNDS = createBlastFurnaceLocalBounds(Direction.NORTH, false);
    private static final AABB SOUTH_BOUNDS = createBlastFurnaceLocalBounds(Direction.SOUTH, false);
    private static final AABB WEST_BOUNDS = createBlastFurnaceLocalBounds(Direction.WEST, false);
    private static final AABB EAST_BOUNDS = createBlastFurnaceLocalBounds(Direction.EAST, false);
    private static final AABB NORTH_TILTED_BOUNDS = createBlastFurnaceLocalBounds(Direction.NORTH, true);
    private static final AABB SOUTH_TILTED_BOUNDS = createBlastFurnaceLocalBounds(Direction.SOUTH, true);
    private static final AABB WEST_TILTED_BOUNDS = createBlastFurnaceLocalBounds(Direction.WEST, true);
    private static final AABB EAST_TILTED_BOUNDS = createBlastFurnaceLocalBounds(Direction.EAST, true);

    public MachineBlastFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(OFF);
        event.register(ON);
    }

    @Override
    public void render(MachineBlastFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        Direction facing = blastFurnaceFacing(state);
        boolean lit = state.hasProperty(MachineBlastFurnaceBlock.LIT) && state.getValue(MachineBlastFurnaceBlock.LIT);

        poseStack.pushPose();
        applyBlastFurnacePose(poseStack, facing, furnace.tilted());
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(lit ? ON : OFF), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MachineBlastFurnaceBlockEntity blockEntity) {
        Direction facing = blastFurnaceFacing(blockEntity.getBlockState());
        // Exact face-used blast_furnace_1710.obj envelope for each authored pose.
        boolean tilted = blockEntity.tilted();
        AABB local = switch (facing) {
            case NORTH -> tilted ? NORTH_TILTED_BOUNDS : NORTH_BOUNDS;
            case SOUTH -> tilted ? SOUTH_TILTED_BOUNDS : SOUTH_BOUNDS;
            case WEST -> tilted ? WEST_TILTED_BOUNDS : WEST_BOUNDS;
            case EAST -> tilted ? EAST_TILTED_BOUNDS : EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Blast furnace facing must be horizontal: " + facing);
        };
        return local.move(blockEntity.getBlockPos());
    }

    /** Exact old blast-furnace pose; the tilt occurs after this machine's authored yaw. */
    private static void applyBlastFurnacePose(PoseStack poseStack, Direction facing, boolean tilted) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Blast furnace facing must be horizontal: " + facing);
        }
        if (tilted) {
            poseStack.translate(0.0D, -0.25D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F));
        }
    }

    private static Direction blastFurnaceFacing(BlockState state) {
        if (!state.hasProperty(MachineBlastFurnaceBlock.FACING)) {
            throw new IllegalStateException("Blast furnace state is missing its owned facing property: " + state);
        }
        return state.getValue(MachineBlastFurnaceBlock.FACING);
    }

    private static AABB createBlastFurnaceLocalBounds(Direction facing, boolean tilted) {
        PoseStack poseStack = new PoseStack();
        applyBlastFurnacePose(poseStack, facing, tilted);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 7.0F, 1.5F);
    }
}
