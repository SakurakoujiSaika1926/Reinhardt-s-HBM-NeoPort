package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.block.TapeRecorderBlock;
import com.reinhardt.hbm.blockentity.TapeRecorderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class TapeRecorderBlockEntityRenderer implements BlockEntityRenderer<TapeRecorderBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/tape_recorder");

    public TapeRecorderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(TapeRecorderBlockEntity recorder, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = recorder.getBlockState();
        poseStack.pushPose();
        poseStack.translate(.5F, 0.0F, .5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(TapeRecorderBlock.legacyRotation(state.getValue(TapeRecorderBlock.FACING))));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
