package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.IndustrialTurbineBlock;
import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class IndustrialTurbineBlockEntityRenderer implements LongRangeBlockEntityRenderer<IndustrialTurbineBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_industrial_turbine_world");
    static final ModelResourceLocation GAUGE = MachineModelRenderer.standalone("block/machine_industrial_turbine_gauge");
    static final ModelResourceLocation FLYWHEEL = MachineModelRenderer.standalone("block/machine_industrial_turbine_flywheel");
    private static final AABB[] LOCAL_BOUNDS = buildIndustrialTurbineBounds();

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
        Direction facing = industrialTurbineFacing(state);

        poseStack.pushPose();
        applyIndustrialTurbineTransform(poseStack, facing);
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
        BlockState state = blockEntity.getBlockState();
        Direction facing = industrialTurbineFacing(state);
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
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
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(gauge), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(GAUGE, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotor), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(FLYWHEEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static AABB[] buildIndustrialTurbineBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyIndustrialTurbineTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -3.5F, 1.5F, 3.0F, 3.5F);
        }
        return bounds;
    }

    private static void applyIndustrialTurbineTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Industrial turbine facing must be horizontal: " + facing);
        }
    }

    private static Direction industrialTurbineFacing(BlockState state) {
        if (!state.hasProperty(IndustrialTurbineBlock.FACING)) {
            throw new IllegalStateException("Industrial turbine state is missing its owned facing property: " + state);
        }
        return state.getValue(IndustrialTurbineBlock.FACING);
    }
}
