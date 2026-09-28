package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.CokerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CokerBlockEntityRenderer implements LongRangeBlockEntityRenderer<CokerBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_coker");
    private static final AABB LOCAL_BOUNDS = buildCokerBounds();

    public CokerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(CokerBlockEntity coker, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = coker.getBlockState();
        poseStack.pushPose();
        applyCokerTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CokerBlockEntity blockEntity) {
        return LOCAL_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static AABB buildCokerBounds() {
        PoseStack poseStack = new PoseStack();
        applyCokerTransform(poseStack);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -2.5F, 0.0F, -2.5F, 2.5F, 23.0F, 2.5F);
    }

    private static void applyCokerTransform(PoseStack poseStack) {
        // RenderCoker has only the block-center translation and no metadata yaw.
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }
}
