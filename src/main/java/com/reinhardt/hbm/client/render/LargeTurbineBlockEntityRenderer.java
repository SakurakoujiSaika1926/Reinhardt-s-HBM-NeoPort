package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeTurbineBlock;
import com.reinhardt.hbm.blockentity.LargeTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class LargeTurbineBlockEntityRenderer implements LongRangeBlockEntityRenderer<LargeTurbineBlockEntity> {
    static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_large_turbine_body");
    static final ModelResourceLocation BLADES = MachineModelRenderer.standalone("block/machine_large_turbine_blades");
    private static final AABB[] LOCAL_BOUNDS = buildLargeTurbineBounds();

    public LargeTurbineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(BLADES);
    }

    @Override
    public void render(LargeTurbineBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turbine.getBlockState();
        Direction facing = largeTurbineFacing(state);

        poseStack.pushPose();
        applyLargeTurbineTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(turbine.rotor(partialTick)), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(0.0D, -1.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADES), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LargeTurbineBlockEntity blockEntity) {
        Direction facing = largeTurbineFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildLargeTurbineBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyLargeTurbineTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.625F, 0.0F, -2.5F, 1.625F, 2.5F, 2.5F);
        }
        return bounds;
    }

    private static void applyLargeTurbineTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Large turbine facing must be horizontal: " + facing);
        }
        poseStack.translate(0.0F, 0.0F, -1.0F);
    }

    private static Direction largeTurbineFacing(BlockState state) {
        if (!state.hasProperty(LargeTurbineBlock.FACING)) {
            throw new IllegalStateException("Large turbine state is missing its owned facing property: " + state);
        }
        return state.getValue(LargeTurbineBlock.FACING);
    }
}
