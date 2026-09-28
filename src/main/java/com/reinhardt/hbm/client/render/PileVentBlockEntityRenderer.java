package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PileMk2DeviceBlock;
import com.reinhardt.hbm.blockentity.PileVentBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact current-upstream renderer for the Chicago Pile MK2 air vent. */
public final class PileVentBlockEntityRenderer implements LongRangeBlockEntityRenderer<PileVentBlockEntity> {
    private static final ModelResourceLocation PIPE = MachineModelRenderer.standalone("block/pile_vent_pipe");
    private static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/pile_vent_fan");

    public PileVentBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PIPE);
        event.register(FAN);
    }

    @Override
    public void render(PileVentBlockEntity vent, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = vent.getBlockState();
        poseStack.pushPose();
        applyVentTransform(poseStack, ventFacing(state));
        renderPart(PIPE, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(vent.renderedFan(partialTick)));
        renderPart(FAN, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PileVentBlockEntity vent) {
        PoseStack poseStack = new PoseStack();
        applyVentTransform(poseStack, ventFacing(vent.getBlockState()));
        // The pipe envelope contains the fan's complete Y-axis rotation.
        return RenderBounds.transformedBox(vent.getBlockPos(), poseStack,
                -0.5F, 0.125F, -0.375F,
                0.5F, 0.875F, 0.375F);
    }

    private static void applyVentTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Pile vent facing must be horizontal: " + facing);
        }
    }

    private static Direction ventFacing(BlockState state) {
        if (!state.hasProperty(PileMk2DeviceBlock.FACING)) {
            throw new IllegalStateException("Pile vent state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(PileMk2DeviceBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Pile vent state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource buffers, BlockState state,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model), poseStack, buffers, state, packedLight, packedOverlay);
    }
}
