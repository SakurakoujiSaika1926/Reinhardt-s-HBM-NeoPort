package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.GroundwaterPumpBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.GroundwaterPumpBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
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

public class GroundwaterPumpBlockEntityRenderer implements BlockEntityRenderer<GroundwaterPumpBlockEntity> {
    static final ModelResourceLocation STEAM_BASE = MachineModelRenderer.standalone("block/pump_steam_base");
    static final ModelResourceLocation STEAM_ROTOR = MachineModelRenderer.standalone("block/pump_steam_rotor");
    static final ModelResourceLocation STEAM_ARMS = MachineModelRenderer.standalone("block/pump_steam_arms");
    static final ModelResourceLocation STEAM_PISTON = MachineModelRenderer.standalone("block/pump_steam_piston");
    static final ModelResourceLocation ELECTRIC_BASE = MachineModelRenderer.standalone("block/pump_electric_base");
    static final ModelResourceLocation ELECTRIC_ROTOR = MachineModelRenderer.standalone("block/pump_electric_rotor");
    static final ModelResourceLocation ELECTRIC_ARMS = MachineModelRenderer.standalone("block/pump_electric_arms");
    static final ModelResourceLocation ELECTRIC_PISTON = MachineModelRenderer.standalone("block/pump_electric_piston");

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
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        PartSet parts = PartSet.forState(state);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        rotateY(poseStack, legacyYaw(facing));
        renderParts(parts, pump.rotor(partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GroundwaterPumpBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 5.0D, 3.0D);
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
        rotateZ(poseStack, (float) (rot - 90.0D));
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
        rotateNegativeZ(poseStack, (float) (angle * 180.0D / Math.PI - 90.0D));
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
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 180.0F;
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            default -> 270.0F;
        };
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }

    private static void rotateZ(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F)));
    }

    private static void rotateNegativeZ(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, -1.0F)));
    }

    record PartSet(ModelResourceLocation base, ModelResourceLocation rotor, ModelResourceLocation arms, ModelResourceLocation piston) {
        static PartSet forState(BlockState state) {
            if (state.is(HbmBlocks.PUMP_ELECTRIC.get())) {
                return new PartSet(ELECTRIC_BASE, ELECTRIC_ROTOR, ELECTRIC_ARMS, ELECTRIC_PISTON);
            }
            return new PartSet(STEAM_BASE, STEAM_ROTOR, STEAM_ARMS, STEAM_PISTON);
        }

        static PartSet forKind(GroundwaterPumpBlock.Kind kind) {
            if (kind == GroundwaterPumpBlock.Kind.ELECTRIC) {
                return new PartSet(ELECTRIC_BASE, ELECTRIC_ROTOR, ELECTRIC_ARMS, ELECTRIC_PISTON);
            }
            return new PartSet(STEAM_BASE, STEAM_ROTOR, STEAM_ARMS, STEAM_PISTON);
        }
    }
}
