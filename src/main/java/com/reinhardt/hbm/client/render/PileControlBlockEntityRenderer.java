package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PileMk2DeviceBlock;
import com.reinhardt.hbm.blockentity.PileControlBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact current-upstream renderer for the Chicago Pile MK2 control actuator. */
public final class PileControlBlockEntityRenderer implements LongRangeBlockEntityRenderer<PileControlBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/pile_control_base");
    private static final ModelResourceLocation ROD = MachineModelRenderer.standalone("block/pile_control_rod");

    public PileControlBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(ROD);
    }

    @Override
    public void render(PileControlBlockEntity control, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = control.getBlockState();
        poseStack.pushPose();
        applyControlTransform(poseStack, controlFacing(state));
        renderPart(BASE, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, (float) (control.renderedLevel(partialTick) * 0.75D), 0.0F);
        renderPart(ROD, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PileControlBlockEntity control) {
        PoseStack poseStack = new PoseStack();
        applyControlTransform(poseStack, controlFacing(control.getBlockState()));
        // Exact Base mesh plus the Rod's complete authored 0..0.75 block travel.
        return RenderBounds.transformedBox(control.getBlockPos(), poseStack,
                -0.375F, 0.0F, -0.375F,
                0.5F, 1.75F, 0.375F);
    }

    private static void applyControlTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Pile control facing must be horizontal: " + facing);
        }
    }

    private static Direction controlFacing(BlockState state) {
        if (!state.hasProperty(PileMk2DeviceBlock.FACING)) {
            throw new IllegalStateException("Pile control state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(PileMk2DeviceBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Pile control state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource buffers, BlockState state,
                                   int packedLight, int packedOverlay) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }
}
