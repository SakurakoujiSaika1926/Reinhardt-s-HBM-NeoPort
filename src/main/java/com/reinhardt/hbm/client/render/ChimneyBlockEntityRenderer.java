package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ChimneyBlockEntityRenderer implements BlockEntityRenderer<ChimneyBlockEntity> {
    private static final ModelResourceLocation BRICK = MachineModelRenderer.standalone("block/chimney_brick");
    private static final ModelResourceLocation INDUSTRIAL = MachineModelRenderer.standalone("block/chimney_industrial");

    public ChimneyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BRICK);
        event.register(INDUSTRIAL);
    }

    @Override
    public void render(ChimneyBlockEntity chimney, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = chimney.getBlockState();
        ModelResourceLocation model = state.is(HbmBlocks.CHIMNEY_INDUSTRIAL.get()) ? INDUSTRIAL : BRICK;

        poseStack.pushPose();
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ChimneyBlockEntity blockEntity) {
        if (blockEntity.getBlockState().is(HbmBlocks.CHIMNEY_INDUSTRIAL.get())) {
            return new AABB(
                    blockEntity.getBlockPos().getX() - 1.0D,
                    blockEntity.getBlockPos().getY(),
                    blockEntity.getBlockPos().getZ() - 1.0D,
                    blockEntity.getBlockPos().getX() + 2.0D,
                    blockEntity.getBlockPos().getY() + 23.0D,
                    blockEntity.getBlockPos().getZ() + 2.0D
            );
        }
        return new AABB(
                blockEntity.getBlockPos().getX() - 1.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 1.0D,
                blockEntity.getBlockPos().getX() + 2.0D,
                blockEntity.getBlockPos().getY() + 13.0D,
                blockEntity.getBlockPos().getZ() + 2.0D
        );
    }
}
