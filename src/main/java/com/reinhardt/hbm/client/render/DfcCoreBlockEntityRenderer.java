package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.DfcCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DfcCoreBlockEntityRenderer implements BlockEntityRenderer<DfcCoreBlockEntity> {
    private static final ModelResourceLocation SPHERE_UV = MachineModelRenderer.standalone("block/dfc_core_sphere_uv");
    private static final ModelResourceLocation SPHERE_RUV = MachineModelRenderer.standalone("block/dfc_core_sphere_ruv");

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
            renderStandby(core, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (core.meltdownTick()) {
            renderMeltdown(core, poseStack, bufferSource, state, packedOverlay);
        } else {
            renderOrb(core, poseStack, bufferSource, state, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DfcCoreBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D);
    }

    private static void renderStandby(DfcCoreBlockEntity core, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(SPHERE_UV), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF808080);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(SPHERE_UV), poseStack, bufferSource, state, packedLight, packedOverlay, 0x661A1A1A);
        poseStack.popPose();
    }

    private static void renderOrb(DfcCoreBlockEntity core, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        int total = core.tank(0).capacity() + core.tank(1).capacity();
        int fill = core.tank(0).amount() + core.tank(1).amount();
        float scale = (total <= 0 ? 0.5F : 4.5F * fill / total + 0.5F) * 0.25F;
        int color = core.color() == 0 ? 0x80FFFFFF : 0xCC000000 | core.color();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(SPHERE_RUV), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, color);
        long time = core.getLevel() == null ? 0L : core.getLevel().getGameTime();
        float pulse = 0.75F + 0.25F * (float) Math.sin(time * 0.2D);
        for (int layer = 1; layer <= 5; layer++) {
            poseStack.pushPose();
            float layerScale = 1.0F + layer * 0.25F + pulse * (6 - layer) * 0.08F;
            poseStack.scale(layerScale, layerScale, layerScale);
            int alpha = Math.max(16, 96 - layer * 12);
            MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(SPHERE_RUV), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, (alpha << 24) | core.color());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderMeltdown(DfcCoreBlockEntity core, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        int color = core.color() == 0 ? 0xFFFFAA00 : 0xFF000000 | core.color();
        poseStack.pushPose();
        poseStack.scale(1.25F, 1.25F, 1.25F);
        MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(SPHERE_RUV), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, color);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(SPHERE_RUV), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, 0x66FFFFFF);
        poseStack.popPose();
    }
}
