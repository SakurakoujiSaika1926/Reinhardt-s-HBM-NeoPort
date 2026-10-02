package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FractionTowerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class FractionTowerBlockEntityRenderer implements LongRangeBlockEntityRenderer<FractionTowerBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_fraction_tower");
    private static final AABB LOCAL_BOUNDS = buildFractionTowerBounds();

    public FractionTowerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(FractionTowerBlockEntity tower, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tower.getBlockState();
        poseStack.pushPose();
        applyFractionTowerTransform(poseStack);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FractionTowerBlockEntity blockEntity) {
        // Exact face-used fraction_tower.obj envelope under the renderer's origin pose.
        return LOCAL_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static AABB buildFractionTowerBounds() {
        PoseStack poseStack = new PoseStack();
        applyFractionTowerTransform(poseStack);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 3.0F, 1.5F);
    }

    private static void applyFractionTowerTransform(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }
}
