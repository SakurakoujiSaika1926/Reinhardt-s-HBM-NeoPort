package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.CoolingTowerBlock;
import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CoolingTowerBlockEntityRenderer implements BlockEntityRenderer<CoolingTowerBlockEntity> {
    private static final ModelResourceLocation SMALL_WORLD = MachineModelRenderer.standalone("block/machine_tower_small_world");
    private static final ModelResourceLocation LARGE_WORLD = MachineModelRenderer.standalone("block/machine_tower_large_world");

    public CoolingTowerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SMALL_WORLD);
        event.register(LARGE_WORLD);
    }

    @Override
    public void render(CoolingTowerBlockEntity tower, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tower.getBlockState();
        ModelResourceLocation model = tower.kind() == CoolingTowerBlock.Kind.LARGE ? LARGE_WORLD : SMALL_WORLD;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CoolingTowerBlockEntity blockEntity) {
        if (blockEntity.kind() == CoolingTowerBlock.Kind.LARGE) {
            return new AABB(
                    blockEntity.getBlockPos().getX() - 4.0D,
                    blockEntity.getBlockPos().getY(),
                    blockEntity.getBlockPos().getZ() - 4.0D,
                    blockEntity.getBlockPos().getX() + 5.0D,
                    blockEntity.getBlockPos().getY() + 13.0D,
                    blockEntity.getBlockPos().getZ() + 5.0D
            );
        }
        return new AABB(
                blockEntity.getBlockPos().getX() - 2.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 2.0D,
                blockEntity.getBlockPos().getX() + 3.0D,
                blockEntity.getBlockPos().getY() + 20.0D,
                blockEntity.getBlockPos().getZ() + 3.0D
        );
    }
}
