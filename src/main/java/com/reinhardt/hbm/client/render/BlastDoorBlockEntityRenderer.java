package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.BlastDoorBlock;
import com.reinhardt.hbm.blockentity.BlastDoorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

public class BlastDoorBlockEntityRenderer implements BlockEntityRenderer<BlastDoorBlockEntity> {
    private static final ModelResourceLocation BASE = model("blast_door_base_part");
    private static final ModelResourceLocation BLOCK = model("blast_door_block_part");
    private static final ModelResourceLocation SLIDER = model("blast_door_slider_part");
    private static final ModelResourceLocation TOOTH = model("blast_door_tooth_part");

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
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, BlastDoorBlock.isAxisZ(state) ? 270.0F : 180.0F);
        renderMovingParts(door.visualTimer(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BlastDoorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0.0D, 7.0D, 0.0D).inflate(1.5D);
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
