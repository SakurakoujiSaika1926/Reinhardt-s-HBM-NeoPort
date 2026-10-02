package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.GroundwaterPumpBlock;
import com.reinhardt.hbm.blockentity.GroundwaterPumpBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class GroundwaterPumpBlockEntityRenderer implements LongRangeBlockEntityRenderer<GroundwaterPumpBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    static final ModelResourceLocation STEAM_BASE = MachineModelRenderer.standalone("block/pump_steam_base");
    static final ModelResourceLocation STEAM_ROTOR = MachineModelRenderer.standalone("block/pump_steam_rotor");
    static final ModelResourceLocation STEAM_ARMS = MachineModelRenderer.standalone("block/pump_steam_arms");
    static final ModelResourceLocation STEAM_PISTON = MachineModelRenderer.standalone("block/pump_steam_piston");
    static final ModelResourceLocation ELECTRIC_BASE = MachineModelRenderer.standalone("block/pump_electric_base");
    static final ModelResourceLocation ELECTRIC_ROTOR = MachineModelRenderer.standalone("block/pump_electric_rotor");
    static final ModelResourceLocation ELECTRIC_ARMS = MachineModelRenderer.standalone("block/pump_electric_arms");
    static final ModelResourceLocation ELECTRIC_PISTON = MachineModelRenderer.standalone("block/pump_electric_piston");
    private static final PartSet STEAM_PARTS = new PartSet(
            STEAM_BASE, STEAM_ROTOR, STEAM_ARMS, STEAM_PISTON);
    private static final PartSet ELECTRIC_PARTS = new PartSet(
            ELECTRIC_BASE, ELECTRIC_ROTOR, ELECTRIC_ARMS, ELECTRIC_PISTON);
    private static final AABB STEAM_NORTH_BOUNDS = buildSteamPumpBounds(Direction.NORTH);
    private static final AABB STEAM_EAST_BOUNDS = buildSteamPumpBounds(Direction.EAST);
    private static final AABB STEAM_SOUTH_BOUNDS = buildSteamPumpBounds(Direction.SOUTH);
    private static final AABB STEAM_WEST_BOUNDS = buildSteamPumpBounds(Direction.WEST);
    private static final AABB ELECTRIC_NORTH_BOUNDS = buildElectricPumpBounds(Direction.NORTH);
    private static final AABB ELECTRIC_EAST_BOUNDS = buildElectricPumpBounds(Direction.EAST);
    private static final AABB ELECTRIC_SOUTH_BOUNDS = buildElectricPumpBounds(Direction.SOUTH);
    private static final AABB ELECTRIC_WEST_BOUNDS = buildElectricPumpBounds(Direction.WEST);

    public GroundwaterPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(STEAM_BASE);
        event.register(STEAM_ROTOR);
        event.register(STEAM_ARMS);
        event.register(STEAM_PISTON);
        event.register(ELECTRIC_BASE);
        event.register(ELECTRIC_ROTOR);
        event.register(ELECTRIC_ARMS);
        event.register(ELECTRIC_PISTON);
    }

    @Override
    public void render(GroundwaterPumpBlockEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = pump.getBlockState();
        Direction facing = groundwaterPumpFacing(state);

        poseStack.pushPose();
        if (pump.kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            applyElectricPumpTransform(poseStack, facing);
            renderParts(ELECTRIC_PARTS,
                    pump.rotor(partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        } else {
            applySteamPumpTransform(poseStack, facing);
            renderParts(STEAM_PARTS,
                    pump.rotor(partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GroundwaterPumpBlockEntity blockEntity) {
        Direction facing = groundwaterPumpFacing(blockEntity.getBlockState());
        AABB local;
        if (blockEntity.kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            local = switch (facing) {
                case NORTH -> ELECTRIC_NORTH_BOUNDS;
                case EAST -> ELECTRIC_EAST_BOUNDS;
                case SOUTH -> ELECTRIC_SOUTH_BOUNDS;
                case WEST -> ELECTRIC_WEST_BOUNDS;
                case UP, DOWN -> throw new IllegalArgumentException("Electric groundwater pump requires a horizontal facing: " + facing);
            };
        } else {
            local = switch (facing) {
                case NORTH -> STEAM_NORTH_BOUNDS;
                case EAST -> STEAM_EAST_BOUNDS;
                case SOUTH -> STEAM_SOUTH_BOUNDS;
                case WEST -> STEAM_WEST_BOUNDS;
                case UP, DOWN -> throw new IllegalArgumentException("Steam groundwater pump requires a horizontal facing: " + facing);
            };
        }
        return local.move(blockEntity.getBlockPos());
    }

    static void renderParts(
            PartSet parts,
            double rot,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        renderPart(parts.base(), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 2.25F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (rot - 90.0D)));
        poseStack.translate(0.0F, -2.25F, 0.0F);
        renderPart(parts.rotor(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        double sin = Math.sin(rot * Math.PI / 180.0D) * 0.5D - 0.5D;
        double cos = Math.cos(rot * Math.PI / 180.0D) * 0.5D;
        double angle = Math.acos(cos / 2.0D);
        double cath = Math.sqrt(1.0D + (cos * cos) / 2.0D);
        double y = 1.0D - cath + sin;

        poseStack.pushPose();
        poseStack.translate(0.0D, y, 0.0D);
        poseStack.translate(0.0F, 4.75F, 0.0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) (angle * 180.0D / Math.PI - 90.0D)));
        poseStack.translate(0.0F, -4.75F, 0.0F);
        renderPart(parts.arms(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, y, 0.0D);
        renderPart(parts.piston(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static void applySteamPumpTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Steam groundwater pump requires a horizontal facing: " + facing);
        }
    }

    private static void applyElectricPumpTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Electric groundwater pump requires a horizontal facing: " + facing);
        }
    }

    private static AABB buildSteamPumpBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySteamPumpTransform(poseStack, facing);
        // pump.obj face vertices span this exact box; the base already contains every legal arm/rotor sweep.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 5.0F, 1.5F);
    }

    private static AABB buildElectricPumpBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyElectricPumpTransform(poseStack, facing);
        // The electric variant uses the same 1.7.10 pump.obj geometry with its own texture/model registration.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 5.0F, 1.5F);
    }

    private static Direction groundwaterPumpFacing(BlockState state) {
        if (!state.hasProperty(GroundwaterPumpBlock.FACING)) {
            throw new IllegalStateException("Groundwater pump state is missing its owned facing property: " + state);
        }
        return state.getValue(GroundwaterPumpBlock.FACING);
    }

    record PartSet(ModelResourceLocation base, ModelResourceLocation rotor, ModelResourceLocation arms, ModelResourceLocation piston) {
    }
}
