package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class HeaterBlockEntityRenderer implements BlockEntityRenderer<HeaterBlockEntity> {
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
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        // The old TESRs lit the shell and door with the world's normal light.
        // Keep full-bright exclusively for the burning inner mesh.
        int worldLight = heater.getLevel() == null
                ? packedLight
                : LevelRenderer.getLightColor(heater.getLevel(), heater.getBlockPos());

        switch (heater.kind()) {
            case FIREBOX -> {
                poseStack.pushPose();
                MachineModelRenderer.orientLegacyHeaterParts(poseStack, facing);
                renderStatic(FIREBOX_WORLD, poseStack, bufferSource, state, worldLight, packedOverlay);
                renderFirebox(heater, partialTick, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case OVEN -> {
                poseStack.pushPose();
                MachineModelRenderer.orientLegacyHeaterParts(poseStack, facing);
                renderStatic(OVEN_WORLD, poseStack, bufferSource, state, worldLight, packedOverlay);
                renderOven(heater, partialTick, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case ELECTRIC -> {
                poseStack.pushPose();
                MachineModelRenderer.orientLegacyHeaterParts(poseStack, facing);
                // The legacy TESR used normal world lighting for the electric heater.
                // This model needs the block renderer path so ambient occlusion is retained.
                renderLitStatic(ELECTRIC, poseStack, bufferSource, state, worldLight, packedOverlay);
                poseStack.popPose();
            }
            case HEATEX -> {
                poseStack.pushPose();
                MachineModelRenderer.orientLegacyHeaterParts(poseStack, facing);
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
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 2.0D, 3.0D);
    }

    private static void renderStatic(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
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
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FIREBOX_DOOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        BakedModel inner = MachineModelRenderer.model(heater.isVisuallyActive() ? FIREBOX_INNER_BURNING : FIREBOX_INNER_EMPTY);
        if (heater.isVisuallyActive()) {
            MachineModelRenderer.renderUnculledFullBright(inner, poseStack, bufferSource, state, packedOverlay);
        } else {
            MachineModelRenderer.renderUnculled(inner, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderOven(HeaterBlockEntity heater, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float door = heater.doorAngle(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, door * 0.75F / 135.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(OVEN_DOOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        BakedModel inner = MachineModelRenderer.model(heater.isVisuallyActive() ? OVEN_INNER_BURNING : OVEN_INNER);
        if (heater.isVisuallyActive()) {
            MachineModelRenderer.renderUnculledFullBright(inner, poseStack, bufferSource, state, packedOverlay);
        } else {
            MachineModelRenderer.renderUnculled(inner, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }
}
