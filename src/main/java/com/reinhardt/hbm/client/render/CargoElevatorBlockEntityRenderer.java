package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.CargoElevatorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class CargoElevatorBlockEntityRenderer implements BlockEntityRenderer<CargoElevatorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/cargo_elevator_base");
    private static final ModelResourceLocation PLATFORM = MachineModelRenderer.standalone("block/cargo_elevator_platform");
    private static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/cargo_elevator_piston");
    private static final ModelResourceLocation GUIDES = MachineModelRenderer.standalone("block/cargo_elevator_guides");

    public CargoElevatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(PLATFORM);
        event.register(PISTON);
        event.register(GUIDES);
    }

    @Override
    public void render(CargoElevatorBlockEntity elevator, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!elevator.isCore()) return;
        BlockState state = elevator.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (elevator.renderPlatform()) {
            double extension = elevator.extension(partialTick);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.0D, extension, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PLATFORM), poseStack, bufferSource, state, packedLight, packedOverlay);
            for (int i = 0; i < extension + 1.0D; i++) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.translate(0.0D, -1.0D, 0.0D);
            }
            poseStack.popPose();
        }
        poseStack.pushPose();
        for (int i = 0; i <= elevator.height(); i++) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GUIDES), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0D, 1.0D, 0.0D);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CargoElevatorBlockEntity elevator) {
        return new AABB(elevator.getBlockPos().getX() - 1.0D, elevator.getBlockPos().getY(), elevator.getBlockPos().getZ() - 1.0D,
                elevator.getBlockPos().getX() + 2.0D, elevator.getBlockPos().getY() + elevator.height() + 2.0D,
                elevator.getBlockPos().getZ() + 2.0D);
    }
}
