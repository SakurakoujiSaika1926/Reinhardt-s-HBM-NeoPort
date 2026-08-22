package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.MiningLaserBlock;
import com.reinhardt.hbm.blockentity.MiningLaserBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Random;

public class MiningLaserBlockEntityRenderer implements BlockEntityRenderer<MiningLaserBlockEntity> {
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_mining_laser_base");
    static final ModelResourceLocation PIVOT = MachineModelRenderer.standalone("block/machine_mining_laser_pivot");
    static final ModelResourceLocation LASER = MachineModelRenderer.standalone("block/machine_mining_laser_laser");

    public MiningLaserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(PIVOT);
        event.register(LASER);
    }

    @Override
    public void render(MiningLaserBlockEntity laser, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = laser.getBlockState();
        Direction facing = state.hasProperty(MiningLaserBlock.FACING) ? state.getValue(MiningLaserBlock.FACING) : Direction.NORTH;
        Vec3 target = laser.renderTargetRaw(partialTick);
        RenderAngles angles = angles(laser.getBlockPos(), target);

        poseStack.pushPose();
        poseStack.translate(0.5D, -1.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(renderYaw(facing)));
        renderParts(state, angles.yaw(), angles.pitch(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (laser.beamActive()) {
            poseStack.pushPose();
            poseStack.translate(0.5D, -1.0D, 0.5D);
            renderBeam(laser, target, poseStack, bufferSource);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(MiningLaserBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4.0D, 8.0D, 4.0D);
    }

    static void renderParts(BlockState state, double yaw, double pitch, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(yawQuaternion((float) yaw));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PIVOT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(yawQuaternion((float) yaw));
        poseStack.translate(0.0D, -1.0D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(pitch + 90.0D), -1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, 1.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LASER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static RenderAngles angles(net.minecraft.core.BlockPos corePos, Vec3 target) {
        double vx = target.x - corePos.getX();
        double vy = target.y - corePos.getY() + 3.0D;
        double vz = target.z - corePos.getZ();
        Vec3 normal = new Vec3(vx, vy, vz).normalize().scale(1.5D);
        Vec3 beam = new Vec3(vx - normal.x, vy - normal.y, vz - normal.z);
        double yaw = Math.toDegrees(Math.atan2(beam.x, beam.z));
        double horizontal = Math.sqrt(beam.x * beam.x + beam.z * beam.z);
        double pitch = Math.toDegrees(Math.atan2(beam.y, horizontal));
        return new RenderAngles(yaw, pitch, beam, normal);
    }

    static float renderYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static void renderBeam(MiningLaserBlockEntity laser, Vec3 target, PoseStack poseStack, MultiBufferSource bufferSource) {
        RenderAngles angles = angles(laser.getBlockPos(), target);
        Vec3 beam = angles.beam();
        Vec3 normal = angles.normal();
        if (beam.lengthSqr() < 1.0E-5D) {
            return;
        }

        poseStack.translate(normal.x, normal.y - 1.0D, normal.z);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        int phase = (int) ((laser.getLevel() == null ? 0L : laser.getLevel().getGameTime()) * -25L % 360L);
        int range = Math.max(1, (int) Math.ceil(beam.length() * 0.5D));
        int segments = range * 2;
        renderSpiralBeam(pose, consumer, Vec3.ZERO, beam, phase, segments, 0.075D, 3, 0.025D);
        renderSpiralBeam(pose, consumer, Vec3.ZERO, beam, phase + 120, segments, 0.075D, 3, 0.025D);
        renderSpiralBeam(pose, consumer, Vec3.ZERO, beam, phase + 240, segments, 0.075D, 3, 0.025D);
    }

    private static void renderSpiralBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 skeleton, int phase, int segments, double size, int layers, double thickness) {
        Vec3 axisY = skeleton.normalize();
        Vec3 axisX = new Vec3(axisY.z, 0.0D, -axisY.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisX = axisX.normalize();
        Vec3 axisZ = axisX.cross(axisY).normalize();
        double segmentLength = skeleton.length() / Math.max(1, segments);
        Vec3 previous = null;
        Random random = new Random(phase);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(phase + 360.0D * layers * index / Math.max(1, segments));
            angle += random.nextDouble() * 0.035D;
            Vec3 spinner = axisX.scale(Math.cos(angle) * size).add(axisZ.scale(Math.sin(angle) * size));
            Vec3 point = start.add(axisY.scale(segmentLength * index)).add(spinner);
            if (previous != null) {
                renderBeamSegment(pose, consumer, previous, point, axisX, axisZ, thickness);
            }
            previous = point;
        }
    }

    private static void renderBeamSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 previous, Vec3 point, Vec3 axisX, Vec3 axisZ, double thickness) {
        Vec3 x = axisX.scale(thickness);
        Vec3 z = axisZ.scale(thickness);
        beamQuad(consumer, pose, previous.add(x).add(z), previous.add(x).subtract(z), point.add(x).subtract(z), point.add(x).add(z));
        beamQuad(consumer, pose, previous.subtract(x).add(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.subtract(x).add(z));
        beamQuad(consumer, pose, previous.add(x).add(z), previous.subtract(x).add(z), point.subtract(x).add(z), point.add(x).add(z));
        beamQuad(consumer, pose, previous.add(x).subtract(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.add(x).subtract(z));
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        beamVertex(consumer, pose, a);
        beamVertex(consumer, pose, b);
        beamVertex(consumer, pose, c);
        beamVertex(consumer, pose, d);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(0xA0, 0x00, 0x00, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(Mth.wrapDegrees(degrees)), 0.0F, 1.0F, 0.0F));
    }

    record RenderAngles(double yaw, double pitch, Vec3 beam, Vec3 normal) {
    }
}
