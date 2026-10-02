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
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB LEVEL_BOUNDS = buildGasFlareBounds(false);
    private static final AABB TILTED_BOUNDS = buildGasFlareBounds(true);

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
        applyGasFlareTransform(poseStack, flare.tilted());
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GasFlareBlockEntity flare) {
        return (flare.tilted() ? TILTED_BOUNDS : LEVEL_BOUNDS).move(flare.getBlockPos());
    }

    private static AABB buildGasFlareBounds(boolean tilted) {
        PoseStack poseStack = new PoseStack();
        applyGasFlareTransform(poseStack, tilted);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.875F, 12.0F, 1.875F);
    }

    /** Exact old gas-flare pose; deliberately not shared with other machines. */
    private static void applyGasFlareTransform(PoseStack poseStack, boolean tilted) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        if (tilted) {
            poseStack.translate(0.0D, -0.25D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(10.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(5.0F));
        }
    }
}
