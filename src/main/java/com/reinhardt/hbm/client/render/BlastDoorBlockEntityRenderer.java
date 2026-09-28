package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.BlastDoorBlock;
import com.reinhardt.hbm.blockentity.BlastDoorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

public class BlastDoorBlockEntityRenderer implements LongRangeBlockEntityRenderer<BlastDoorBlockEntity> {
    private static final ModelResourceLocation BASE = model("blast_door_base_part");
    private static final ModelResourceLocation BLOCK = model("blast_door_block_part");
    private static final ModelResourceLocation SLIDER = model("blast_door_slider_part");
    private static final ModelResourceLocation TOOTH = model("blast_door_tooth_part");
    private static final AABB AXIS_X_BOUNDS = buildBlastDoorBounds(false);
    private static final AABB AXIS_Z_BOUNDS = buildBlastDoorBounds(true);

    public BlastDoorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : List.of(BASE, BLOCK, SLIDER, TOOTH)) {
            event.register(model);
        }
    }

    @Override
    public void render(BlastDoorBlockEntity door, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = door.getBlockState();
        if (!(state.getBlock() instanceof BlastDoorBlock)) {
            return;
        }

        poseStack.pushPose();
        applyBlastDoorTransform(poseStack, BlastDoorBlock.isAxisZ(state));
        renderMovingParts(door.visualTimer(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BlastDoorBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        boolean axisZ = state.getBlock() instanceof BlastDoorBlock && BlastDoorBlock.isAxisZ(state);
        /* Exact union for visualTimer 0..5: Base supplies Y=0, the translated
         * Block supplies Y=7, and Tooth/Slider motion remains between them.
         * Face-used X/Z extrema across all four component OBJs are +/-0.5
         * and +/-0.75 respectively. */
        return (axisZ ? AXIS_Z_BOUNDS : AXIS_X_BOUNDS).move(blockEntity.getBlockPos());
    }

    private static AABB buildBlastDoorBounds(boolean axisZ) {
        PoseStack poseStack = new PoseStack();
        applyBlastDoorTransform(poseStack, axisZ);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -0.5F, 0.0F, -0.75F, 0.5F, 7.0F, 0.75F);
    }

    private static void applyBlastDoorTransform(PoseStack poseStack, boolean axisZ) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // RenderBlastDoor applies 180 degrees, plus another 90 only to metadata 2.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(axisZ ? 270.0F : 180.0F));
    }

    static void renderClosedParts(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        renderMovingParts(5.0D, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderMovingParts(double timer, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        render(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 3.0D, 0.0D);
        render(BLOCK, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, -timer, 0.0D);
        poseStack.translate(0.0D, 2.0D, 0.0D);
        render(TOOTH, poseStack, bufferSource, state, packedLight, packedOverlay);

        if (timer > 1.0D) {
            render(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (timer > 2.0D) {
            poseStack.translate(0.0D, 1.0D, 0.0D);
            render(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (timer > 3.0D) {
            poseStack.translate(0.0D, 1.0D, 0.0D);
            render(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (timer > 4.0D) {
            poseStack.translate(0.0D, 1.0D, 0.0D);
            render(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/doors/" + name);
    }
}
