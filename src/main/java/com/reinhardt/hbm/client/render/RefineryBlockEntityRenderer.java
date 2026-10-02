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
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB LEVEL_BOUNDS = createRefineryLocalBounds(false);
    private static final AABB TILTED_BOUNDS = createRefineryLocalBounds(true);

    public RefineryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(RefineryBlockEntity refinery, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = refinery.getBlockState();
        poseStack.pushPose();
        applyRefineryPose(poseStack, refinery.tilted());
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RefineryBlockEntity blockEntity) {
        // Exact face-used refinery.obj envelope under the fixed 180-degree render pose.
        return (blockEntity.tilted() ? TILTED_BOUNDS : LEVEL_BOUNDS).move(blockEntity.getBlockPos());
    }

    private static void applyRefineryPose(PoseStack poseStack, boolean tilted) {
        // Exact fixed transform from the old refinery renderer; it is not a
        // reusable facing convention.
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        if (tilted) {
            poseStack.translate(0.0D, -0.25D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F));
        }
    }

    private static AABB createRefineryLocalBounds(boolean tilted) {
        PoseStack poseStack = new PoseStack();
        applyRefineryPose(poseStack, tilted);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 9.5F, 1.5F);
    }
}
