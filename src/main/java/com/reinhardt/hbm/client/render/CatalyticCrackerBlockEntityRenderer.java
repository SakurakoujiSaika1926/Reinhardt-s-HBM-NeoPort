package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.CatalyticCrackerBlock;
import com.reinhardt.hbm.blockentity.CatalyticCrackerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CatalyticCrackerBlockEntityRenderer implements LongRangeBlockEntityRenderer<CatalyticCrackerBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_catalytic_cracker");
    private static final AABB[] LOCAL_BOUNDS = buildCatalyticCrackerBounds();

    public CatalyticCrackerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(CatalyticCrackerBlockEntity cracker, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = cracker.getBlockState();
        Direction facing = catalyticCrackerFacing(state);

        poseStack.pushPose();
        applyCatalyticCrackerTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CatalyticCrackerBlockEntity blockEntity) {
        Direction facing = catalyticCrackerFacing(blockEntity.getBlockState());
        // Face-used cracking_tower.obj vertices, transformed by the renderer's center translation and cardinal yaw.
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildCatalyticCrackerBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCatalyticCrackerTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -3.5F, 0.0F, -3.5F, 3.500001F, 16.0F, 2.5F);
        }
        return bounds;
    }

    private static void applyCatalyticCrackerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Catalytic cracker facing must be horizontal: " + facing);
        }
    }

    private static Direction catalyticCrackerFacing(BlockState state) {
        if (!state.hasProperty(CatalyticCrackerBlock.FACING)) {
            throw new IllegalStateException("Catalytic cracker state is missing its owned facing property: " + state);
        }
        return state.getValue(CatalyticCrackerBlock.FACING);
    }
}
