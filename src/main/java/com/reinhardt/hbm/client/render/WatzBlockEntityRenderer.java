package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.WatzBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class WatzBlockEntityRenderer implements BlockEntityRenderer<WatzBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/watz_world");

    public WatzBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(WatzBlockEntity watz, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = watz.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WatzBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX() - 3.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 3.0D,
                blockEntity.getBlockPos().getX() + 4.0D,
                blockEntity.getBlockPos().getY() + 3.0D,
                blockEntity.getBlockPos().getZ() + 4.0D
        );
    }
}
