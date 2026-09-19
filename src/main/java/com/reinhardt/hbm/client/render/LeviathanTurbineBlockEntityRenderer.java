package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
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

public class LeviathanTurbineBlockEntityRenderer implements LongRangeBlockEntityRenderer<LeviathanTurbineBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_chungus_body");
    private static final ModelResourceLocation LEVER = MachineModelRenderer.standalone("block/machine_chungus_lever");
    private static final ModelResourceLocation BLADES = MachineModelRenderer.standalone("block/machine_chungus_blades");

    public LeviathanTurbineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(LEVER);
        event.register(BLADES);
    }

    @Override
    public void render(LeviathanTurbineBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turbine.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(legacyYaw(facing)));
        poseStack.translate(0.0D, 0.0D, -3.0D);

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 4.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(turbine.leverAngle()), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, 0.0D, -4.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LEVER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 2.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(turbine.rotor(partialTick)), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(0.0D, -2.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADES), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LeviathanTurbineBlockEntity blockEntity) {
        /*
         * The legacy renderer translated the OBJ by -3 blocks along its local
         * Z axis and then rotated it with the turbine's facing.  The OBJ spans
         * roughly 15 blocks along that axis, so the rendered geometry reaches
         * about ten blocks away from the block entity after the transform.
         *
         * The old 1.7.10 tile entity used INFINITE_EXTENT_AABB.  The previous
         * port's -6..+7 box clipped the back half of the model, which made the
         * whole OBJ disappear as soon as the camera moved far enough for the
         * frustum test to reject that box.  Keep this finite but cover the
         * complete rotated model (including the animated blade/lever margin).
         */
        return new AABB(
                blockEntity.getBlockPos().getX() - 11.0D,
                blockEntity.getBlockPos().getY() - 2.0D,
                blockEntity.getBlockPos().getZ() - 11.0D,
                blockEntity.getBlockPos().getX() + 12.0D,
                blockEntity.getBlockPos().getY() + 10.0D,
                blockEntity.getBlockPos().getZ() + 12.0D
        );
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
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
