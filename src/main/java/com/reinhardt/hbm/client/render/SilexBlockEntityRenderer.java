package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.SilexBlock;
import com.reinhardt.hbm.blockentity.SilexBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SilexBlockEntityRenderer implements LongRangeBlockEntityRenderer<SilexBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_silex");

    public SilexBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(SilexBlockEntity silex, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = silex.getBlockState();
        Direction facing = silexFacing(state);
        poseStack.pushPose();
        applySilexPose(poseStack, facing);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SilexBlockEntity blockEntity) {
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 3.0D, 2.0D);
    }

    /** Exact RenderSILEX metadata mapping: NORTH=2, SOUTH=3, WEST=4, EAST=5. */
    private static void applySilexPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("SILEX facing must be horizontal: " + facing);
        }
    }

    private static Direction silexFacing(BlockState state) {
        if (!state.hasProperty(SilexBlock.FACING)) {
            throw new IllegalStateException("SILEX block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(SilexBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("SILEX block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
