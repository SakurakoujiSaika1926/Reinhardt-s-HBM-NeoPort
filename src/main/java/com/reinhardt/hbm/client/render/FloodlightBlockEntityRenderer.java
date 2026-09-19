package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.FloodlightBlock;
import com.reinhardt.hbm.blockentity.FloodlightBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class FloodlightBlockEntityRenderer implements BlockEntityRenderer<FloodlightBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/floodlight_base");
    private static final ModelResourceLocation LIGHTS = MachineModelRenderer.standalone("block/floodlight_lights");
    private static final ModelResourceLocation LAMPS = MachineModelRenderer.standalone("block/floodlight_lamps");
    private static final int LAMP_GLOW = 0xB0FFF2C0;

    public FloodlightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(LIGHTS);
        event.register(LAMPS);
    }

    @Override
    public void render(FloodlightBlockEntity light, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = light.getBlockState();
        poseStack.pushPose();
        // RenderFloodlight starts at (x + 0.5, y + 0.5, z + 0.5).  The
        // block-entity pose is block-local, so restore that origin before
        // applying the legacy metadata rotations and offsets.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // RenderFloodlight used the complete legacy metadata (facing ordinal,
        // with flipped variants 6/7) rather than a generic direction transform.
        int meta = state.getValue(FloodlightBlock.FACING).ordinal()
                + (state.getValue(FloodlightBlock.FLIPPED) ? 6 : 0);
        switch (meta) {
            case 0, 6 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            case 1, 7 -> { }
            case 2 -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            }
            case 3 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            case 4 -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            }
            case 5 -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(270.0F));
            }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
        if (meta != 0 && meta != 1) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float rotation = light.rotation();
        if (meta == 0 || meta == 6) rotation -= 90.0F;
        if (meta == 1 || meta == 7) rotation += 90.0F;
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(0.0F, -0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LIGHTS), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (light.isOn()) {
            MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(LAMPS), poseStack, bufferSource, state, packedOverlay);
            MachineModelRenderer.renderUnculledTintedUvEyes(MachineModelRenderer.model(LAMPS), poseStack, bufferSource,
                    state, packedOverlay, LAMP_GLOW, 0.0F, 0.0F);
        } else {
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(LAMPS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF404040);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FloodlightBlockEntity light) {
        return new AABB(light.getBlockPos()).inflate(2.0D);
    }
}
