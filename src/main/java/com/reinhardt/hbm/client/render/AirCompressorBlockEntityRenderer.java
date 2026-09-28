package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.AirCompressorBlock;
import com.reinhardt.hbm.blockentity.AirCompressorBlockEntity;
import com.reinhardt.hbm.client.sound.AirCompressorClientSounds;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class AirCompressorBlockEntityRenderer implements LongRangeBlockEntityRenderer<AirCompressorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_intake_base");
    private static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/machine_intake_fan");
    private static final AABB[] LOCAL_BOUNDS = buildLocalBounds();

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
        Direction facing = airCompressorFacing(state);

        poseStack.pushPose();
        applyAirCompressorTransform(poseStack, facing);
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
        Direction facing = airCompressorFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildLocalBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            /*
             * intake.obj Base faces are [-1,0,-1]..[1,1,1].  The renderer's
             * final local translate changes that to the box below.  Fan's
             * full Y sweep radius is 0.5867034095664759, strictly inside it.
             */
            PoseStack poseStack = new PoseStack();
            applyAirCompressorTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F);
        }
        return bounds;
    }

    private static void applyAirCompressorTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Air compressor facing must be horizontal: " + facing);
        }
        // RenderIntake applies this model-space offset after its metadata yaw.
        poseStack.translate(-0.5F, 0.0F, 0.5F);
    }

    private static Direction airCompressorFacing(BlockState state) {
        if (!state.hasProperty(AirCompressorBlock.FACING)) {
            throw new IllegalStateException("Air compressor state is missing its owned facing property: " + state);
        }
        return state.getValue(AirCompressorBlock.FACING);
    }
}
