package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FurnaceCombinationBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class FurnaceCombinationBlockEntityRenderer implements LongRangeBlockEntityRenderer<FurnaceCombinationBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/furnace_combination");
    private static final AABB LOCAL_BOUNDS = buildFurnaceCombinationBounds();

    public FurnaceCombinationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(FurnaceCombinationBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        poseStack.pushPose();
        applyFurnaceCombinationTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FurnaceCombinationBlockEntity blockEntity) {
        return LOCAL_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static AABB buildFurnaceCombinationBounds() {
        PoseStack poseStack = new PoseStack();
        applyFurnaceCombinationTransform(poseStack);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 2.125F, 1.5F);
    }

    private static void applyFurnaceCombinationTransform(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }
}
