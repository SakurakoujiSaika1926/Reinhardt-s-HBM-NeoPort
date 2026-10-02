package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LegacyFurnaceBlock;
import com.reinhardt.hbm.blockentity.SteelFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SteelFurnaceBlockEntityRenderer implements LongRangeBlockEntityRenderer<SteelFurnaceBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/furnace_steel");

    public SteelFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(SteelFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        Direction facing = steelFurnaceFacing(state);

        poseStack.pushPose();
        applySteelFurnacePose(poseStack, facing);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SteelFurnaceBlockEntity blockEntity) {
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 2.625D, 2.0D);
    }

    /** Exact RenderFurnaceSteel metadata rotation followed by its fixed -90 degree yaw. */
    private static void applySteelFurnacePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Steel furnace facing must be horizontal: " + facing);
        }
    }

    private static Direction steelFurnaceFacing(BlockState state) {
        if (!state.hasProperty(LegacyFurnaceBlock.FACING)) {
            throw new IllegalStateException("Steel furnace block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(LegacyFurnaceBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Steel furnace block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
