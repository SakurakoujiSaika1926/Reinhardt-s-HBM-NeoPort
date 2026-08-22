package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.PoweredSteamCondenserBlockEntity;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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

public class PoweredSteamCondenserBlockEntityRenderer implements BlockEntityRenderer<PoweredSteamCondenserBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_condenser_powered_body");
    private static final ModelResourceLocation FAN1 = MachineModelRenderer.standalone("block/machine_condenser_powered_fan1");
    private static final ModelResourceLocation FAN2 = MachineModelRenderer.standalone("block/machine_condenser_powered_fan2");

    public PoweredSteamCondenserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(FAN1);
        event.register(FAN2);
    }

    @Override
    public void render(PoweredSteamCondenserBlockEntity condenser, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = condenser.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(LegacyMachineGeometry.legacyWavefrontYaw(facing, 270.0F)));

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        float spin = condenser.spin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(spin), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FAN1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(spin), -1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FAN2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PoweredSteamCondenserBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D, 4.0D, 5.0D);
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
