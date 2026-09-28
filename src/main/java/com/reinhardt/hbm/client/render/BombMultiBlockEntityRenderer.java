package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.BombMultiBlock;
import com.reinhardt.hbm.blockentity.BombMultiBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact RenderBombMulti transform owned entirely by this bomb renderer. */
public final class BombMultiBlockEntityRenderer implements LongRangeBlockEntityRenderer<BombMultiBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/bomb_multi_world");

    public BombMultiBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(BombMultiBlockEntity bomb, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = bomb.getBlockState();
        poseStack.pushPose();
        applyBombMultiPose(poseStack, bombMultiFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BombMultiBlockEntity blockEntity) {
        Direction facing = bombMultiFacing(blockEntity.getBlockState());
        // Face-used OBJ bounds after translate(.5,.5,.5) * rotateX(180)
        // and the renderer's four cardinal legacy yaws.
        return switch (facing) {
            case SOUTH -> RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                    -0.375D, 0.0D, 0.25D, 1.025D, 0.5D, 0.75D);
            case WEST -> RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                    0.25D, 0.0D, -0.375D, 0.75D, 0.5D, 1.025D);
            case NORTH -> RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                    -0.025D, 0.0D, 0.25D, 1.375D, 0.5D, 0.75D);
            case EAST -> RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                    0.25D, 0.0D, -0.025D, 0.75D, 0.5D, 1.375D);
            case UP, DOWN -> throw new IllegalArgumentException("Bomb Multi facing must be horizontal: " + facing);
        };
    }

    private static Direction bombMultiFacing(BlockState state) {
        if (!state.hasProperty(BombMultiBlock.FACING)) {
            throw new IllegalStateException("Bomb Multi state is missing its dedicated facing property");
        }
        return state.getValue(BombMultiBlock.FACING);
    }

    private static void applyBombMultiPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Bomb Multi facing must be horizontal: " + facing);
        }
    }
}
