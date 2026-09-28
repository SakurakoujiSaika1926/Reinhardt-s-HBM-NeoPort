package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.MicrowaveBlock;
import com.reinhardt.hbm.blockentity.MicrowaveBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class MicrowaveBlockEntityRenderer implements LongRangeBlockEntityRenderer<MicrowaveBlockEntity> {
    static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_microwave_body");
    static final ModelResourceLocation WINDOW = MachineModelRenderer.standalone("block/machine_microwave_window");
    static final ModelResourceLocation PLATE = MachineModelRenderer.standalone("block/machine_microwave_plate");
    private static final AABB[] LOCAL_BOUNDS = buildMicrowaveBounds();

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
        Direction facing = microwaveFacing(state);

        poseStack.pushPose();
        applyMicrowaveWorldTransform(poseStack, facing);
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
    public AABB getRenderBoundingBox(MicrowaveBlockEntity microwave) {
        Direction facing = microwaveFacing(microwave.getBlockState());
        /*
         * Body/window face vertices plus the plate's complete rotation about
         * (.575, -.45).  Its farthest referenced vertex has radius
         * 0.3823824846838568, and the resulting disk only extends the body's
         * adjusted local max-Z to 0.5823824846838568.  Keeping the full disk
         * makes section membership independent of the running animation.
         */
        return LOCAL_BOUNDS[facing.ordinal()].move(microwave.getBlockPos());
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, BlockState state,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static AABB[] buildMicrowaveBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyMicrowaveWorldTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.512995F, 0.787154F, -0.339384F,
                    0.487836F, 1.315426F, (float) 0.5823824846838568D);
        }
        return bounds;
    }

    private static void applyMicrowaveWorldTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, -0.785F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Microwave facing must be horizontal: " + facing);
        }
    }

    private static Direction microwaveFacing(BlockState state) {
        if (!state.hasProperty(MicrowaveBlock.FACING)) {
            throw new IllegalStateException("Microwave state is missing its owned facing property: " + state);
        }
        return state.getValue(MicrowaveBlock.FACING);
    }
}
