package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.HeatBoilerBlock;
import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class HeatBoilerBlockEntityRenderer implements LongRangeBlockEntityRenderer<HeatBoilerBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/heat_boiler_world");
    private static final ModelResourceLocation BURST = MachineModelRenderer.standalone("block/heat_boiler_burst_world");
    private static final float MAX_WOBBLE = 1.0F + (float) 0.015D;
    private static final AABB[] FULL_LIFECYCLE_BOUNDS = buildFullLifecycleBounds();

    public HeatBoilerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
        event.register(BURST);
    }

    @Override
    public void render(HeatBoilerBlockEntity boiler, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = boiler.getBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        if (!boiler.exploded()) {
            int capacity = boiler.outputTank().capacity();
            if (capacity > 0 && boiler.outputTank().amount() >= (capacity * 9) / 10) {
                float wobble = 1.0F + (float) (Math.sin((boiler.getLevel() == null ? 0L : boiler.getLevel().getGameTime()) + partialTick) * 0.015D);
                poseStack.scale(wobble, 1.0F, wobble);
            }
        }
        applyHeatBoilerOrientation(poseStack, heatBoilerFacing(state));
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
        Direction facing = heatBoilerFacing(blockEntity.getBlockState());
        return FULL_LIFECYCLE_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildFullLifecycleBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            poseStack.translate(0.5D, 0.0D, 0.5D);
            /* sin <= 1 and both casts/additions are monotone, so this is the
             * exact largest float scale the render expression can produce. */
            poseStack.scale(MAX_WOBBLE, 1.0F, MAX_WOBBLE);
            applyHeatBoilerOrientation(poseStack, facing);
            /* Face-used boiler.obj vertices attain every +/-1.5 footprint
             * corner and y=4. Transforming its corners through this actual
             * float PoseStack also contains the smaller unscaled burst OBJ. */
            bounds[facing.ordinal()] = RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -1.5F, 1.5F, 4.0F, 1.5F);
        }
        return bounds;
    }

    private static Direction heatBoilerFacing(BlockState state) {
        if (!state.hasProperty(HeatBoilerBlock.FACING)) {
            throw new IllegalStateException("Heat boiler state is missing its dedicated facing property");
        }
        return state.getValue(HeatBoilerBlock.FACING);
    }

    private static void applyHeatBoilerOrientation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Heat boiler facing must be horizontal: " + facing);
        }
    }
}
