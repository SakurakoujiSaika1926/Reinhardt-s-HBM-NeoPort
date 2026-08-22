package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.FelBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Random;

public class FelBlockEntityRenderer implements BlockEntityRenderer<FelBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_fel");

    public FelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(FelBlockEntity fel, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = fel.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, MachineModelRendererYaw.fel1710Yaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);

        if (fel.beamVisible()) {
            int color = fel.mode().renderedBeamColor() == 0
                    ? fel.mode().guiColor(fel.getLevel() == null ? 0L : fel.getLevel().getGameTime())
                    : fel.mode().renderedBeamColor();
            poseStack.translate(0.0D, 1.5D, -1.5D);
            renderBeam(poseStack, bufferSource, Math.max(1, fel.distance() - 3), color, fel.getLevel() == null ? 0L : fel.getLevel().getGameTime());
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FelBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(24.0D, 6.0D, 24.0D);
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, int length, int color, long gameTime) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        Vec3 end = new Vec3(0.0D, 0.0D, -length - 1.0D);
        renderSpiralBeam(pose, consumer, Vec3.ZERO, end, (int) (gameTime % 1000L), 2, 0.0625D, color);
        renderRandomBeam(pose, consumer, Vec3.ZERO, end, (int) (gameTime % 500L), Math.max(2, length / 2 + 1), 0.0625D, color);
    }

    private static void renderSpiralBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, int phase, int turns, double thickness, int color) {
        Vec3 axisY = end.subtract(start).normalize();
        Vec3 axisX = new Vec3(axisY.z, 0.0D, -axisY.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisX = axisX.normalize();
        Vec3 axisZ = axisX.cross(axisY).normalize();
        int segments = Math.max(8, (int) Math.ceil(end.length() * 2.0D));
        Vec3 previous = null;
        for (int index = 0; index <= segments; index++) {
            double progress = index / (double) segments;
            double angle = Math.toRadians(phase + 360.0D * turns * progress);
            Vec3 spinner = axisX.scale(Math.cos(angle) * thickness).add(axisZ.scale(Math.sin(angle) * thickness));
            Vec3 point = start.add(axisY.scale(end.length() * progress)).add(spinner);
            if (previous != null) {
                beamQuad(consumer, pose, previous, point, thickness * 0.35D, color);
            }
            previous = point;
        }
    }

    private static void renderRandomBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, int seed, int segments, double thickness, int color) {
        Random random = new Random(seed);
        Vec3 previous = start;
        for (int index = 1; index <= segments; index++) {
            double progress = index / (double) segments;
            Vec3 point = start.add(end.scale(progress));
            if (index < segments) {
                point = point.add(
                        (random.nextDouble() - 0.5D) * thickness * 4.0D,
                        (random.nextDouble() - 0.5D) * thickness * 4.0D,
                        (random.nextDouble() - 0.5D) * thickness * 2.0D
                );
            }
            beamQuad(consumer, pose, previous, point, thickness * 0.5D, color);
            previous = point;
        }
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end, double thickness, int color) {
        Vec3 direction = end.subtract(start);
        Vec3 axisX = new Vec3(direction.z, 0.0D, -direction.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisX = axisX.normalize().scale(thickness);
        Vec3 axisY = new Vec3(0.0D, thickness, 0.0D);
        beamVertex(consumer, pose, start.add(axisX).add(axisY), color);
        beamVertex(consumer, pose, start.subtract(axisX).add(axisY), color);
        beamVertex(consumer, pose, end.subtract(axisX).subtract(axisY), color);
        beamVertex(consumer, pose, end.add(axisX).subtract(axisY), color);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int color) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static final class MachineModelRendererYaw {
        private static float fel1710Yaw(Direction facing) {
            // HBM 1.7.10 RenderFEL:
            // meta 2(NORTH)=0, 3(SOUTH)=180, 4(WEST)=90, 5(EAST)=270.
            return switch (facing) {
                case SOUTH -> 180.0F;
                case WEST -> 90.0F;
                case EAST -> 270.0F;
                default -> 0.0F;
            };
        }
    }
}
