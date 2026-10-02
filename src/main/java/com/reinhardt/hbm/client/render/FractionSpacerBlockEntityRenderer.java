package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FractionSpacerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class FractionSpacerBlockEntityRenderer implements LongRangeBlockEntityRenderer<FractionSpacerBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/fraction_spacer");
    private static final AABB LOCAL_BOUNDS = buildFractionSpacerBounds();

    public FractionSpacerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(FractionSpacerBlockEntity spacer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = spacer.getBlockState();
        poseStack.pushPose();
        applyFractionSpacerTransform(poseStack);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FractionSpacerBlockEntity blockEntity) {
        return LOCAL_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static AABB buildFractionSpacerBounds() {
        PoseStack poseStack = new PoseStack();
        applyFractionSpacerTransform(poseStack);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -2.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F);
    }

    private static void applyFractionSpacerTransform(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }
}
