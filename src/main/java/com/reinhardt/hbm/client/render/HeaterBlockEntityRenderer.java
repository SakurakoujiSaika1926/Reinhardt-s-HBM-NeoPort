package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.HeaterBlock;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class HeaterBlockEntityRenderer implements LongRangeBlockEntityRenderer<HeaterBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation FIREBOX_WORLD = MachineModelRenderer.standalone("block/heater_firebox_world");
    private static final ModelResourceLocation FIREBOX_DOOR = MachineModelRenderer.standalone("block/heater_firebox_door");
    private static final ModelResourceLocation FIREBOX_INNER_EMPTY = MachineModelRenderer.standalone("block/heater_firebox_inner_empty");
    private static final ModelResourceLocation FIREBOX_INNER_BURNING = MachineModelRenderer.standalone("block/heater_firebox_inner_burning");
    private static final ModelResourceLocation OVEN_WORLD = MachineModelRenderer.standalone("block/heater_oven_world");
    private static final ModelResourceLocation OVEN_DOOR = MachineModelRenderer.standalone("block/heater_oven_door");
    private static final ModelResourceLocation OVEN_INNER = MachineModelRenderer.standalone("block/heater_oven_inner");
    private static final ModelResourceLocation OVEN_INNER_BURNING = MachineModelRenderer.standalone("block/heater_oven_inner_burning");
    private static final ModelResourceLocation OILBURNER = MachineModelRenderer.standalone("block/heater_oilburner");
    private static final ModelResourceLocation ELECTRIC = MachineModelRenderer.standalone("block/heater_electric");
    private static final ModelResourceLocation HEATEX = MachineModelRenderer.standalone("block/heater_heatex");
    private static final AABB[] FIREBOX_BOUNDS = buildFireboxBounds();
    private static final AABB[] OVEN_BOUNDS = buildOvenBounds();
    private static final AABB[] ELECTRIC_BOUNDS = buildElectricHeaterBounds();
    private static final AABB[] HEATEX_BOUNDS = buildHeatExchangerBounds();

    public HeaterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FIREBOX_WORLD);
        event.register(FIREBOX_DOOR);
        event.register(FIREBOX_INNER_EMPTY);
        event.register(FIREBOX_INNER_BURNING);
        event.register(OVEN_WORLD);
        event.register(OVEN_DOOR);
        event.register(OVEN_INNER);
        event.register(OVEN_INNER_BURNING);
        event.register(OILBURNER);
        event.register(ELECTRIC);
        event.register(HEATEX);
    }

    @Override
    public void render(HeaterBlockEntity heater, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = heater.getBlockState();
        // The old TESRs lit the shell and door with the world's normal light.
        // Keep full-bright exclusively for the burning inner mesh.
        int worldLight = heater.getLevel() == null
                ? packedLight
                : LevelRenderer.getLightColor(heater.getLevel(), heater.getBlockPos());

        switch (heater.kind()) {
            case FIREBOX -> {
                poseStack.pushPose();
                applyFireboxTransform(poseStack, fireboxFacing(state));
                renderStatic(FIREBOX_WORLD, poseStack, bufferSource, state, worldLight, packedOverlay);
                renderFirebox(heater, partialTick, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case OVEN -> {
                poseStack.pushPose();
                applyOvenTransform(poseStack, ovenFacing(state));
                renderStatic(OVEN_WORLD, poseStack, bufferSource, state, worldLight, packedOverlay);
                renderOven(heater, partialTick, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case ELECTRIC -> {
                poseStack.pushPose();
                applyElectricHeaterTransform(poseStack, electricHeaterFacing(state));
                // The legacy TESR used normal world lighting for the electric heater.
                // This model needs the block renderer path so ambient occlusion is retained.
                renderLitStatic(ELECTRIC, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case HEATEX -> {
                poseStack.pushPose();
                applyHeatExchangerTransform(poseStack, heatExchangerFacing(state));
                renderStatic(HEATEX, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case OILBURNER -> {
                poseStack.pushPose();
                renderStatic(OILBURNER, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(HeaterBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        return switch (blockEntity.kind()) {
            case FIREBOX -> FIREBOX_BOUNDS[fireboxFacing(state).ordinal()].move(blockEntity.getBlockPos());
            case OVEN -> OVEN_BOUNDS[ovenFacing(state).ordinal()].move(blockEntity.getBlockPos());
            case ELECTRIC -> ELECTRIC_BOUNDS[electricHeaterFacing(state).ordinal()].move(blockEntity.getBlockPos());
            case HEATEX -> HEATEX_BOUNDS[heatExchangerFacing(state).ordinal()].move(blockEntity.getBlockPos());
            // The oil burner is the sole heater variant rendered without an orientation transform.
            case OILBURNER -> RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                    -1.0D, 0.0D, -1.0D, 2.0D, 2.0D, 2.000001D);
        };
    }

    private static AABB[] buildFireboxBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyFireboxTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F, (float) 2.6275996611745187D, 1.0F, 2.0F);
        }
        return bounds;
    }

    private static AABB[] buildOvenBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyOvenTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F, 2.1875F, 1.0F, 2.0F);
        }
        return bounds;
    }

    private static AABB[] buildElectricHeaterBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyElectricHeaterTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.125F, 0.0F, -1.125F, 3.0F, 1.0F, 2.125F);
        }
        return bounds;
    }

    private static AABB[] buildHeatExchangerBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyHeatExchangerTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F);
        }
        return bounds;
    }

    private static Direction fireboxFacing(BlockState state) {
        if (!state.hasProperty(HeaterBlock.FACING)) {
            throw new IllegalStateException("Firebox heater state is missing its dedicated facing property");
        }
        return state.getValue(HeaterBlock.FACING);
    }

    private static Direction ovenFacing(BlockState state) {
        if (!state.hasProperty(HeaterBlock.FACING)) {
            throw new IllegalStateException("Oven heater state is missing its dedicated facing property");
        }
        return state.getValue(HeaterBlock.FACING);
    }

    private static Direction electricHeaterFacing(BlockState state) {
        if (!state.hasProperty(HeaterBlock.FACING)) {
            throw new IllegalStateException("Electric heater state is missing its dedicated facing property");
        }
        return state.getValue(HeaterBlock.FACING);
    }

    private static Direction heatExchangerFacing(BlockState state) {
        if (!state.hasProperty(HeaterBlock.FACING)) {
            throw new IllegalStateException("Heat exchanger state is missing its dedicated facing property");
        }
        return state.getValue(HeaterBlock.FACING);
    }

    private static void applyFireboxTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Firebox heater facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static void applyOvenTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Oven heater facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static void applyElectricHeaterTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Electric heater facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static void applyHeatExchangerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Heat exchanger facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static void renderStatic(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static void renderLitStatic(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.render(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderFirebox(HeaterBlockEntity heater, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        float door = heater.doorAngle(partialTick);
        poseStack.translate(1.875F, 0.0F, 0.875F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-door), 0.0F, 1.0F, 0.0F)));
        poseStack.translate(-1.875F, 0.0F, -0.875F);
        STATIC_MESHES.render(FIREBOX_DOOR, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        ModelResourceLocation inner = heater.isVisuallyActive() ? FIREBOX_INNER_BURNING : FIREBOX_INNER_EMPTY;
        if (heater.isVisuallyActive()) {
            STATIC_MESHES.render(inner, poseStack, state,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay);
        } else {
            STATIC_MESHES.render(inner, poseStack, state, packedLight, packedOverlay);
        }
    }

    private static void renderOven(HeaterBlockEntity heater, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float door = heater.doorAngle(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, door * 0.75F / 135.0F);
        STATIC_MESHES.render(OVEN_DOOR, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        ModelResourceLocation inner = heater.isVisuallyActive() ? OVEN_INNER_BURNING : OVEN_INNER;
        if (heater.isVisuallyActive()) {
            STATIC_MESHES.render(inner, poseStack, state,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay);
        } else {
            STATIC_MESHES.render(inner, poseStack, state, packedLight, packedOverlay);
        }
    }
}
