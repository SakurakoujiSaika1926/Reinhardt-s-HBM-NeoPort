package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.DieselGeneratorBlock;
import com.reinhardt.hbm.blockentity.DieselGeneratorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DieselGeneratorBlockEntityRenderer implements LongRangeBlockEntityRenderer<DieselGeneratorBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_diesel_world");
    private static final AABB[] LOCAL_BOUNDS = buildDieselGeneratorBounds();

    public DieselGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(DieselGeneratorBlockEntity diesel, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = diesel.getBlockState();
        Direction facing = dieselGeneratorFacing(state);
        poseStack.pushPose();
        applyDieselGeneratorTransform(poseStack, facing);
        if (diesel.hasAcceptableFuel() && diesel.fuelTank().amount() > 0) {
            poseStack.translate(
                    Math.sin((diesel.getLevel() == null ? 0L : diesel.getLevel().getGameTime()) / 2.5D) * 0.005D,
                    0.0D,
                    Math.sin((diesel.getLevel() == null ? 0L : diesel.getLevel().getGameTime()) / 5.0D) * 0.005D
            );
        }
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DieselGeneratorBlockEntity blockEntity) {
        // The cardinally rotated face-used model is exactly one block. The
        // running animation adds independent sinusoidal x/z translations with
        // amplitude 0.005; use its exact closed all-state/time envelope so a
        // fuel transition cannot change render-section membership unnoticed.
        Direction facing = dieselGeneratorFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildDieselGeneratorBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyDieselGeneratorTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.505F, 0.0F, -0.505F, 0.505F, 1.0F, 0.505F);
        }
        return bounds;
    }

    private static void applyDieselGeneratorTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Diesel generator facing must be horizontal: " + facing);
        }
    }

    private static Direction dieselGeneratorFacing(BlockState state) {
        if (!state.hasProperty(DieselGeneratorBlock.FACING)) {
            throw new IllegalStateException("Diesel generator state is missing its owned facing property: " + state);
        }
        return state.getValue(DieselGeneratorBlock.FACING);
    }
}
