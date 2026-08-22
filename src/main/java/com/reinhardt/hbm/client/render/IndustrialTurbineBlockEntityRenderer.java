package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
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

public class IndustrialTurbineBlockEntityRenderer implements BlockEntityRenderer<IndustrialTurbineBlockEntity> {
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_industrial_turbine_world");
    static final ModelResourceLocation GAUGE = MachineModelRenderer.standalone("block/machine_industrial_turbine_gauge");
    static final ModelResourceLocation FLYWHEEL = MachineModelRenderer.standalone("block/machine_industrial_turbine_flywheel");

    public IndustrialTurbineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(GAUGE);
        event.register(FLYWHEEL);
    }

    @Override
    public void render(
            IndustrialTurbineBlockEntity turbine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = turbine.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(yawQuaternion(industrialYaw(facing)));
        renderParts(
                turbine.rotor(partialTick),
                turbine.gaugeAngle(),
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(IndustrialTurbineBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D, 4.0D, 5.0D);
    }

    static void renderParts(
            float rotor,
            float gauge,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(gauge), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GAUGE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotor), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FLYWHEEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static float industrialYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }
}
