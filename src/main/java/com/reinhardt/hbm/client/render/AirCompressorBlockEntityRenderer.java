package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.AirCompressorBlockEntity;
import com.reinhardt.hbm.client.sound.AirCompressorClientSounds;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class AirCompressorBlockEntityRenderer implements BlockEntityRenderer<AirCompressorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_intake_base");
    private static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/machine_intake_fan");

    public AirCompressorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FAN);
    }

    @Override
    public void render(AirCompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        AirCompressorClientSounds.tick(compressor);
        BlockState state = compressor.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(yawQuaternion(legacyIntakeYaw(facing)));
        poseStack.translate(-0.5F, 0.0F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        float rot = -compressor.fan(partialTick);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FAN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AirCompressorBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 1.0D,
                pos.getY(),
                pos.getZ() - 1.0D,
                pos.getX() + 2.0D,
                pos.getY() + 1.0D,
                pos.getZ() + 2.0D
        );
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static float legacyIntakeYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
