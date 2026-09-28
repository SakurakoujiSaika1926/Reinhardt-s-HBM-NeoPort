package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.RefineryBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class RefineryBlockEntityRenderer implements LongRangeBlockEntityRenderer<RefineryBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_refinery");
    private static final AABB RENDER_BOUNDS = createRefineryLocalBounds();

    public RefineryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(RefineryBlockEntity refinery, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = refinery.getBlockState();
        poseStack.pushPose();
        applyRefineryPose(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RefineryBlockEntity blockEntity) {
        // Exact face-used refinery.obj envelope under the fixed 180-degree render pose.
        return RENDER_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static void applyRefineryPose(PoseStack poseStack) {
        // Exact fixed transform from the old refinery renderer; it is not a
        // reusable facing convention.
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static AABB createRefineryLocalBounds() {
        PoseStack poseStack = new PoseStack();
        applyRefineryPose(poseStack);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 9.5F, 1.5F);
    }
}
