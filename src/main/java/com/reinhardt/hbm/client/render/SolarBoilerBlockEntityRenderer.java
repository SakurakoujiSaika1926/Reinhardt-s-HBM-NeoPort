package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.SolarBoilerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class SolarBoilerBlockEntityRenderer implements BlockEntityRenderer<SolarBoilerBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_solar_boiler_world");

    public SolarBoilerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(SolarBoilerBlockEntity boiler, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = boiler.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(legacyYaw(facing)), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderMirrorBeams(boiler, poseStack, bufferSource);
    }

    @Override
    public AABB getRenderBoundingBox(SolarBoilerBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 4.0D, 3.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }

    /** Direct vertex translation of RenderSolarBoiler's four additive beam faces. */
    private static void renderMirrorBeams(SolarBoilerBlockEntity boiler, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        BlockPos boilerPos = boiler.getBlockPos();
        int rendered = 0;
        for (BlockPos mirrorPos : boiler.activeMirrors()) {
            if (rendered++ >= 64) {
                break;
            }

            int dx = boilerPos.getX() - mirrorPos.getX();
            int dy = boilerPos.getY() - mirrorPos.getY();
            int dz = boilerPos.getZ() - mirrorPos.getZ();
            double distance = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
            if (distance <= 0.0001D) {
                continue;
            }

            float pitch = (float) (Math.toDegrees(-Math.asin((dy + 0.5D) / distance)) + 90.0D);
            float yaw = (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180.0D);
            poseStack.pushPose();
            poseStack.translate(-dx, -dy, -dz);
            poseStack.translate(0.0D, 1.0D, 0.0D);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(yaw), 0.0F, 1.0F, 0.0F)));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(pitch), 0.0F, 0.0F, 1.0F)));
            poseStack.translate(0.0D, -1.0D, 0.0D);
            renderBeam(consumer, poseStack.last(), (float) distance);
            poseStack.popPose();
        }
    }

    private static void renderBeam(VertexConsumer consumer, PoseStack.Pose pose, float distance) {
        // Legacy alpha gradient: 0.01 at the mirror and 0.005 at the boiler.
        quad(consumer, pose, 0.5F, 1.0625F, 0.5F, 0.5F, 1.0625F, -0.5F, 0.5F, distance, -0.5F, 0.5F, distance, 0.5F, 0.01F, 0.005F);
        quad(consumer, pose, -0.5F, 1.0625F, 0.5F, -0.5F, 1.0625F, -0.5F, -0.5F, distance, -0.5F, -0.5F, distance, 0.5F, 0.01F, 0.005F);
        quad(consumer, pose, 0.5F, 1.0625F, 0.5F, -0.5F, 1.0625F, 0.5F, -0.5F, distance, 0.5F, 0.5F, distance, 0.5F, 0.01F, 0.005F);
        quad(consumer, pose, 0.5F, 1.0625F, -0.5F, -0.5F, 1.0625F, -0.5F, -0.5F, distance, -0.5F, 0.5F, distance, -0.5F, 0.01F, 0.005F);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float startAlpha, float endAlpha) {
        vertex(consumer, pose, ax, ay, az, startAlpha);
        vertex(consumer, pose, bx, by, bz, startAlpha);
        vertex(consumer, pose, cx, cy, cz, endAlpha);
        vertex(consumer, pose, dx, dy, dz, endAlpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor(255, 255, 255, Math.round(alpha * 255.0F));
    }
}
