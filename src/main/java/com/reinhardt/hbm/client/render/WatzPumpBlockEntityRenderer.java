package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.WatzPumpBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class WatzPumpBlockEntityRenderer implements BlockEntityRenderer<WatzPumpBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/watz_pump_world");

    public WatzPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(WatzPumpBlockEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = pump.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WatzPumpBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 2.0D, 1.0D);
    }
}
