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
        switch (state.getValue(FloodlightBlock.FACING)) {
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            case NORTH -> { poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F)); poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F)); }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            case WEST -> { poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F)); poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F)); }
            case EAST -> { poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F)); poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F)); }
            case UP -> { }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
        if (state.getValue(FloodlightBlock.FACING) != net.minecraft.core.Direction.UP
                && state.getValue(FloodlightBlock.FACING) != net.minecraft.core.Direction.DOWN) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float rotation = light.rotation();
        if (state.getValue(FloodlightBlock.FACING) == net.minecraft.core.Direction.DOWN) rotation -= 90.0F;
        if (state.getValue(FloodlightBlock.FACING) == net.minecraft.core.Direction.UP) rotation += 90.0F;
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(0.0F, -0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LIGHTS), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (light.isOn()) {
            MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(LAMPS), poseStack, bufferSource, state, packedOverlay);
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
