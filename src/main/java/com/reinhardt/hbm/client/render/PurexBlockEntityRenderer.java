package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class PurexBlockEntityRenderer implements BlockEntityRenderer<PurexBlockEntity> {
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_purex_base");
    static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_purex_frame");
    static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/machine_purex_fan");
    static final ModelResourceLocation PUMP = MachineModelRenderer.standalone("block/machine_purex_pump");

    public PurexBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(FAN);
        event.register(PUMP);
    }

    @Override
    public void render(PurexBlockEntity purex, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        purex.updateClientAnimation();
        BlockState state = purex.getBlockState();
        Direction facing = state.getValue(LargeMachineBlock.FACING);
        double anim = purex.clientAnim(partialTick);

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (purex.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.translate(1.5D, 1.25D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (anim * 45.0D)));
        poseStack.translate(-1.5D, -1.25D, 0.0D);
        renderPart(FAN, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(Math.PI * 0.5D * Math.cos(anim * 0.25D)) * 0.5D, 0.0D, 0.0D);
        renderPart(PUMP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PurexBlockEntity purex) {
        return new AABB(
                purex.getBlockPos().getX() - 2.0D,
                purex.getBlockPos().getY(),
                purex.getBlockPos().getZ() - 2.0D,
                purex.getBlockPos().getX() + 3.0D,
                purex.getBlockPos().getY() + 5.0D,
                purex.getBlockPos().getZ() + 3.0D
        );
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
