package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.DfcCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DfcCoreBlockEntityRenderer implements LongRangeBlockEntityRenderer<DfcCoreBlockEntity> {
    private static final ModelResourceLocation SPHERE_UV = MachineModelRenderer.standalone("block/dfc_core_sphere_uv");
    private static final ModelResourceLocation SPHERE_RUV = MachineModelRenderer.standalone("block/dfc_core_sphere_ruv");
    private static final double MAX_PULSE = ((Math.asin(0.8D) / 0.8D) + 1.0D) / 2.0D;
    private static final double MAX_ORB_SCALE = 5.0D * (5.0D + 0.5D * MAX_PULSE);
    // Round the analytical supremum outwards to cover the renderer's intermediate float arithmetic.
    private static final double MAX_ORB_X_RADIUS = Math.ceil(0.475529D * MAX_ORB_SCALE * 1_000.0D) / 1_000.0D;
    private static final double MAX_ORB_YZ_RADIUS = Math.ceil(0.5D * MAX_ORB_SCALE * 1_000.0D) / 1_000.0D;

    public DfcCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SPHERE_UV);
        event.register(SPHERE_RUV);
    }

    @Override
    public void render(DfcCoreBlockEntity core, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = core.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        if (core.heat() <= 0) {
            renderStandby(poseStack, bufferSource, state, packedOverlay);
        } else if (core.meltdownTick()) {
            renderMeltdown(core, poseStack, bufferSource, state, packedOverlay);
        } else {
            renderOrb(core, poseStack, bufferSource, state, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DfcCoreBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        return new AABB(
                centerX - MAX_ORB_X_RADIUS, centerY - MAX_ORB_YZ_RADIUS, centerZ - MAX_ORB_YZ_RADIUS,
                centerX + MAX_ORB_X_RADIUS, centerY + MAX_ORB_YZ_RADIUS, centerZ + MAX_ORB_YZ_RADIUS
        );
    }

    /** Direct state/scale port of RenderCore#renderStandby. */
    private static void renderStandby(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(SPHERE_UV), poseStack,
                bufferSource, state, packedOverlay, 0xFF808080);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(SPHERE_UV), poseStack,
                bufferSource, state, packedOverlay, 0x661A1A1A);
        poseStack.popPose();
    }

    private static void renderOrb(DfcCoreBlockEntity core, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        int total = core.tank(0).capacity() + core.tank(1).capacity();
        int fill = core.tank(0).amount() + core.tank(1).amount();
        float scale = total <= 0 ? 0.5F : 4.5F * fill / total + 0.5F;
        int color = core.color() == 0 ? 0xFF000000 : 0xFF000000 | core.color();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(SPHERE_RUV), poseStack,
                bufferSource, state, packedOverlay, color);
        long time = core.getLevel() == null ? 0L : core.getLevel().getGameTime();
        double phase = (time * 0.1D) % (Math.PI * 2.0D);
        double t = 0.8D;
        float pulse = (float) (((Math.atan((t * Math.sin(phase)) / (1.0D - t * Math.cos(phase))) / t) + 1.0D) / 2.0D);
        for (int layer = 0; layer <= 16; layer++) {
            poseStack.pushPose();
            float layerScale = 1.0F + 0.25F * layer + pulse * (20.0F - layer) * 0.125F;
            poseStack.scale(layerScale, layerScale, layerScale);
            MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(SPHERE_RUV), poseStack,
                    bufferSource, state, packedOverlay, color);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderMeltdown(DfcCoreBlockEntity core, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        int color = core.color() == 0 ? 0xFFFFAA00 : 0xFF000000 | core.color();
        poseStack.pushPose();
        poseStack.scale(1.25F, 1.25F, 1.25F);
        MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(SPHERE_RUV), poseStack,
                bufferSource, state, packedOverlay, color);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(SPHERE_RUV), poseStack,
                bufferSource, state, packedOverlay, 0x66FFFFFF);
        poseStack.popPose();
    }
}
