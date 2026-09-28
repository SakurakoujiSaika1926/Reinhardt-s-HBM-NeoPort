package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.VendingMachineBlock;
import com.reinhardt.hbm.blockentity.VendingMachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Literal RenderVendingMachine orientation mapping with separate legacy OBJ groups. */
public final class VendingMachineBlockEntityRenderer implements LongRangeBlockEntityRenderer<VendingMachineBlockEntity> {
    private static final ModelResourceLocation SODA = MachineModelRenderer.standalone("block/vending_machine_soda");
    private static final ModelResourceLocation SNACKS = MachineModelRenderer.standalone("block/vending_machine_snacks");
    private static final AABB NORTH_RENDER_BOUNDS = createVendingMachineLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createVendingMachineLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createVendingMachineLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createVendingMachineLocalBounds(Direction.EAST);

    public VendingMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SODA);
        event.register(SNACKS);
    }

    @Override
    public void render(VendingMachineBlockEntity vending, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = vending.getBlockState();
        poseStack.pushPose();
        applyVendingMachinePose(poseStack, vendingMachineFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(vending.snacks() ? SNACKS : SODA), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(VendingMachineBlockEntity vending) {
        BlockState state = vending.getBlockState();
        Direction facing = vendingMachineFacing(state);
        return vendingMachineRenderBounds(vending.getBlockPos(), facing);
    }

    private static void applyVendingMachinePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Vending machine facing must be horizontal: " + facing);
        }
    }

    private static AABB vendingMachineRenderBounds(BlockPos pos, Direction facing) {
        AABB local = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Vending machine facing must be horizontal: " + facing);
        };
        return local.move(pos);
    }

    private static AABB createVendingMachineLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyVendingMachinePose(poseStack, facing);
        // Both OBJ groups share these 44 face-referenced vertices.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -0.5F, 0.0F, -0.5F, 0.5F, 2.0F, 0.5F);
    }

    private static Direction vendingMachineFacing(BlockState state) {
        if (!state.hasProperty(VendingMachineBlock.FACING)) {
            throw new IllegalStateException("Vending machine block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(VendingMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Vending machine block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
