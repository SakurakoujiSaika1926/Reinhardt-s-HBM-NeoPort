package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PurexBlock;
import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class PurexBlockEntityRenderer implements LongRangeBlockEntityRenderer<PurexBlockEntity> {
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_purex_base");
    static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_purex_frame");
    static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/machine_purex_fan");
    static final ModelResourceLocation PUMP = MachineModelRenderer.standalone("block/machine_purex_pump");
    private static final AABB NORTH_BOUNDS = createPurexLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_BOUNDS = createPurexLocalBounds(Direction.SOUTH);
    private static final AABB WEST_BOUNDS = createPurexLocalBounds(Direction.WEST);
    private static final AABB EAST_BOUNDS = createPurexLocalBounds(Direction.EAST);

    public PurexBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(FAN);
        event.register(PUMP);
    }

    @Override
    public void render(PurexBlockEntity purex, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        purex.updateClientAnimation();
        BlockState state = purex.getBlockState();
        Direction facing = purexFacing(state);
        double anim = purex.clientAnim(partialTick);

        poseStack.pushPose();
        applyPurexPose(poseStack, facing);
        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (purex.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.translate(1.5D, 1.25D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (anim * 45.0D)));
        poseStack.translate(-1.5D, -1.25D, 0.0D);
        renderPart(FAN, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(Math.PI * 0.5D * Math.cos(anim * 0.25D)) * 0.5D, 0.0D, 0.0D);
        renderPart(PUMP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PurexBlockEntity purex) {
        BlockState state = purex.getBlockState();
        Direction facing = purexFacing(state);
        // Base+optional frame are exactly [-2.5,0,-2.5]..[2.5,5,2.5].
        // The fan's full Z rotation and pump's complete +/-0.5 X stroke are
        // strictly contained, so no animation-derived expansion is needed.
        AABB local = switch (facing) {
            case NORTH -> NORTH_BOUNDS;
            case SOUTH -> SOUTH_BOUNDS;
            case WEST -> WEST_BOUNDS;
            case EAST -> EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("PUREX facing must be horizontal: " + facing);
        };
        return local.move(purex.getBlockPos());
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void applyPurexPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("PUREX facing must be horizontal: " + facing);
        }
    }

    private static AABB createPurexLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyPurexPose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -2.5F, 0.0F, -2.5F, 2.5F, 5.0F, 2.5F);
    }

    private static Direction purexFacing(BlockState state) {
        if (!state.hasProperty(PurexBlock.FACING)) {
            throw new IllegalStateException("PUREX block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(PurexBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PUREX block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
