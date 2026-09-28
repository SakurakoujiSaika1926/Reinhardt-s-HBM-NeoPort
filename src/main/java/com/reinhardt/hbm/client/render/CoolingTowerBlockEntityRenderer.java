package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.CoolingTowerBlock;
import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CoolingTowerBlockEntityRenderer implements LongRangeBlockEntityRenderer<CoolingTowerBlockEntity> {
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
        if (tower.kind() == CoolingTowerBlock.Kind.LARGE) {
            renderLargeTower(state, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderSmallTower(state, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(CoolingTowerBlockEntity blockEntity) {
        if (blockEntity.kind() == CoolingTowerBlock.Kind.LARGE) {
            return largeTowerRenderBounds(blockEntity.getBlockPos());
        }
        return smallTowerRenderBounds(blockEntity.getBlockPos());
    }

    private static void renderSmallTower(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySmallTowerWorldPose(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SMALL_WORLD), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLargeTower(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyLargeTowerWorldPose(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LARGE_WORLD), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applySmallTowerWorldPose(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
    }

    private static void applyLargeTowerWorldPose(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
    }

    private static AABB smallTowerRenderBounds(BlockPos pos) {
        PoseStack poseStack = new PoseStack();
        applySmallTowerWorldPose(poseStack);
        // Exact face-used model extrema before the small tower's dedicated translation.
        return RenderBounds.transformedBox(pos, poseStack,
                -2.5F, 0.0F, -2.5F, 2.5F, 19.5F, 2.5F);
    }

    private static AABB largeTowerRenderBounds(BlockPos pos) {
        PoseStack poseStack = new PoseStack();
        applyLargeTowerWorldPose(poseStack);
        // Exact face-used model extrema before the large tower's dedicated translation.
        return RenderBounds.transformedBox(pos, poseStack,
                -4.5F, 0.0F, -4.5F, 4.5F, 13.0F, 4.5F);
    }
}
