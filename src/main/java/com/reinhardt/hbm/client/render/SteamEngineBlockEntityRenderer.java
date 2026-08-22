package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.SteamEngineBlockEntity;
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

public class SteamEngineBlockEntityRenderer implements BlockEntityRenderer<SteamEngineBlockEntity> {
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_steam_engine_world");
    static final ModelResourceLocation FLYWHEEL = MachineModelRenderer.standalone("block/machine_steam_engine_flywheel");
    static final ModelResourceLocation SHAFT = MachineModelRenderer.standalone("block/machine_steam_engine_shaft");
    static final ModelResourceLocation TRANSMISSION = MachineModelRenderer.standalone("block/machine_steam_engine_transmission");
    static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_steam_engine_piston");

    public SteamEngineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FLYWHEEL);
        event.register(SHAFT);
        event.register(TRANSMISSION);
        event.register(PISTON);
    }

    @Override
    public void render(SteamEngineBlockEntity engine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = engine.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(yawQuaternion(legacySteamYaw(facing)));
        poseStack.translate(2.0F, 0.0F, 0.0F);
        renderParts(engine.rotor(partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SteamEngineBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(6.0D, 3.0D, 6.0D);
    }

    static void renderParts(float rot, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(2.0D, 1.375D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(-2.0D, -1.375D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FLYWHEEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.375D, -0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot * 2.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.375D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SHAFT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        double sin = Math.sin(rot * Math.PI / 180.0D) * 0.25D - 0.25D;
        double cos = Math.cos(rot * Math.PI / 180.0D) * 0.25D;
        double ang = Math.acos(cos / 1.875D);

        poseStack.pushPose();
        poseStack.translate(sin, cos, 0.0D);
        poseStack.translate(2.25D, 1.375D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) (ang - Math.PI / 2.0D), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(-2.25D, -1.375D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TRANSMISSION), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        double cath = Math.sqrt(3.515625D - (cos * cos) / 2.0D);
        poseStack.translate(1.875D - cath + sin, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static float legacySteamYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> 270.0F;
            case WEST -> 0.0F;
            default -> 90.0F;
        };
    }
}
