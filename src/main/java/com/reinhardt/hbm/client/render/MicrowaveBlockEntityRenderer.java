package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.MicrowaveBlock;
import com.reinhardt.hbm.blockentity.MicrowaveBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class MicrowaveBlockEntityRenderer implements BlockEntityRenderer<MicrowaveBlockEntity> {
    static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_microwave_body");
    static final ModelResourceLocation WINDOW = MachineModelRenderer.standalone("block/machine_microwave_window");
    static final ModelResourceLocation PLATE = MachineModelRenderer.standalone("block/machine_microwave_plate");

    public MicrowaveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(WINDOW);
        event.register(PLATE);
    }

    @Override
    public void render(MicrowaveBlockEntity microwave, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = microwave.getBlockState();
        Direction facing = state.hasProperty(MicrowaveBlock.FACING)
                ? state.getValue(MicrowaveBlock.FACING) : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, -0.785D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(legacyYaw(facing)), 0.0F, 1.0F, 0.0F)));
        poseStack.translate(-0.5D, 0.0D, 0.65D);

        renderPart(BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(WINDOW, poseStack, bufferSource, state, packedLight, packedOverlay);

        if (microwave.time() > 0) {
            double rotation = (System.currentTimeMillis() * microwave.speed() / 10.0D) % 360.0D;
            poseStack.translate(0.575D, 0.0D, -0.45D);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                    (float) Math.toRadians(rotation), 0.0F, 1.0F, 0.0F)));
            poseStack.translate(-0.575D, 0.0D, 0.45D);
        }
        renderPart(PLATE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MicrowaveBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D);
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, BlockState state,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
