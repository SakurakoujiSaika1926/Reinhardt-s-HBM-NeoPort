package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FurnaceCombinationBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class FurnaceCombinationBlockEntityRenderer implements BlockEntityRenderer<FurnaceCombinationBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/furnace_combination");

    public FurnaceCombinationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(FurnaceCombinationBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FurnaceCombinationBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX() - 1.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 1.0D,
                blockEntity.getBlockPos().getX() + 2.0D,
                blockEntity.getBlockPos().getY() + 2.125D,
                blockEntity.getBlockPos().getZ() + 2.0D
        );
    }
}
