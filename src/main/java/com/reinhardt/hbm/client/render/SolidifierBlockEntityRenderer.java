package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.SolidifierBlock;
import com.reinhardt.hbm.blockentity.SolidifierBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SolidifierBlockEntityRenderer implements LongRangeBlockEntityRenderer<SolidifierBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_solidifier");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_solidifier_fluid");
    private static final ModelResourceLocation GLASS = MachineModelRenderer.standalone("block/machine_solidifier_glass");
    private static final int GLASS_COLOR = 0x26BFFFFF;

    public SolidifierBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(FLUID);
        event.register(GLASS);
    }

    @Override
    public void render(SolidifierBlockEntity solidifier, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = solidifier.getBlockState();
        Direction facing = solidifierFacing(state);

        poseStack.pushPose();
        applySolidifierPose(poseStack, facing);
        STATIC_MESHES.render(MAIN, poseStack, state, packedLight, packedOverlay);

        if (solidifier.tank().amount() > 0 && !solidifier.tank().type().isNone()) {
            double height = (double) solidifier.tank().amount() / (double) solidifier.tank().capacity();
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.25D, 0.0D);
            poseStack.scale(1.0F, (float) height, 1.0F);
            poseStack.translate(0.0D, -1.25D, 0.0D);
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(FLUID),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay,
                    0xFF000000 | solidifier.tank().type().color()
            );
            poseStack.popPose();
        }

        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(GLASS), poseStack, bufferSource, state, packedLight, packedOverlay, GLASS_COLOR);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolidifierBlockEntity blockEntity) {
        // Main OBJ is +/-1.5 by 0..4; glass and every permitted fluid scale are contained.
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 4.0D, 2.0D);
    }

    /** Exact RenderSolidifier metadata mapping. */
    private static void applySolidifierPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Solidifier facing must be horizontal: " + facing);
        }
    }

    private static Direction solidifierFacing(BlockState state) {
        if (!state.hasProperty(SolidifierBlock.FACING)) {
            throw new IllegalStateException("Solidifier block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(SolidifierBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Solidifier block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
