package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
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

public class HeatBoilerBlockEntityRenderer implements BlockEntityRenderer<HeatBoilerBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/heat_boiler_world");
    private static final ModelResourceLocation BURST = MachineModelRenderer.standalone("block/heat_boiler_burst_world");

    public HeatBoilerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
        event.register(BURST);
    }

    @Override
    public void render(HeatBoilerBlockEntity boiler, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = boiler.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        if (!boiler.exploded()) {
            int capacity = boiler.outputTank().capacity();
            if (capacity > 0 && boiler.outputTank().amount() >= (capacity * 9) / 10) {
                float wobble = 1.0F + (float) (Math.sin((boiler.getLevel() == null ? 0L : boiler.getLevel().getGameTime()) + partialTick) * 0.015D);
                poseStack.scale(wobble, 1.0F, wobble);
            }
        }
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(legacyYaw(facing)), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(boiler.exploded() ? BURST : WORLD),
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HeatBoilerBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 4.0D, 3.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }
}
