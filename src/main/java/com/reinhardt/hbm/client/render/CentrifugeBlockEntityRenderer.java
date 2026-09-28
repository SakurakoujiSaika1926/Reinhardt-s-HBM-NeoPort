package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.CentrifugeBlock;
import com.reinhardt.hbm.blockentity.CentrifugeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CentrifugeBlockEntityRenderer implements LongRangeBlockEntityRenderer<CentrifugeBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_centrifuge");
    private static final AABB[] LOCAL_BOUNDS = buildCentrifugeBounds();

    public CentrifugeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(CentrifugeBlockEntity centrifuge, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = centrifuge.getBlockState();
        Direction facing = centrifugeFacing(state);
        poseStack.pushPose();
        applyCentrifugeTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CentrifugeBlockEntity blockEntity) {
        Direction facing = centrifugeFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildCentrifugeBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCentrifugeTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.5F, 0.0F, -0.5F, 0.5F, 4.0F, 0.5F);
        }
        return bounds;
    }

    private static void applyCentrifugeTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Centrifuge facing must be horizontal: " + facing);
        }
    }

    private static Direction centrifugeFacing(BlockState state) {
        if (!state.hasProperty(CentrifugeBlock.FACING)) {
            throw new IllegalStateException("Centrifuge state is missing its owned facing property: " + state);
        }
        return state.getValue(CentrifugeBlock.FACING);
    }
}
