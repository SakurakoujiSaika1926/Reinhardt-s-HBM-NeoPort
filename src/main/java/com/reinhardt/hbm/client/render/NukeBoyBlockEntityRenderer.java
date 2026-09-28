package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.NukeBoyBlock;
import com.reinhardt.hbm.blockentity.NukeBoyBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class NukeBoyBlockEntityRenderer implements LongRangeBlockEntityRenderer<NukeBoyBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/nuke_boy_world");
    private static final AABB[] LOCAL_BOUNDS = buildNukeBoyBounds();

    public NukeBoyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(NukeBoyBlockEntity nukeBoy, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = nukeBoy.getBlockState();
        Direction facing = nukeBoyFacing(state);
        poseStack.pushPose();
        applyNukeBoyTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(NukeBoyBlockEntity blockEntity) {
        Direction facing = nukeBoyFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildNukeBoyBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyNukeBoyTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.35F, 0.0F, -0.425F, 2.85F, 0.85F, 0.425F);
        }
        return bounds;
    }

    private static void applyNukeBoyTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Nuke Boy facing must be horizontal: " + facing);
        }
        poseStack.translate(-2.0F, 0.0F, 0.0F);
    }

    private static Direction nukeBoyFacing(BlockState state) {
        if (!state.hasProperty(NukeBoyBlock.FACING)) {
            throw new IllegalStateException("Nuke Boy state is missing its owned facing property: " + state);
        }
        return state.getValue(NukeBoyBlock.FACING);
    }
}
