package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ResearchReactorBlockEntityRenderer implements BlockEntityRenderer<ResearchReactorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_reactor_small_base");
    private static final ModelResourceLocation RODS = MachineModelRenderer.standalone("block/machine_reactor_small_rods");

    public ResearchReactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(RODS);
    }

    @Override
    public void render(ResearchReactorBlockEntity reactor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = reactor.getBlockState();
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, 180.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, reactor.controlLevel(partialTick), 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RODS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ResearchReactorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 4.0D, 1.0D);
    }
}
