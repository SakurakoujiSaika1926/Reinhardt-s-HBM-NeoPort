package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class GasFlareBlockEntityRenderer implements LongRangeBlockEntityRenderer<GasFlareBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_flare_world");
    private static final AABB LOCAL_BOUNDS = buildGasFlareBounds();

    public GasFlareBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(
            GasFlareBlockEntity flare,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = flare.getBlockState();
        poseStack.pushPose();
        applyGasFlareTransform(poseStack);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(WORLD),
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GasFlareBlockEntity flare) {
        return LOCAL_BOUNDS.move(flare.getBlockPos());
    }

    private static AABB buildGasFlareBounds() {
        PoseStack poseStack = new PoseStack();
        applyGasFlareTransform(poseStack);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.875F, 12.0F, 1.875F);
    }

    private static void applyGasFlareTransform(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
    }
}
