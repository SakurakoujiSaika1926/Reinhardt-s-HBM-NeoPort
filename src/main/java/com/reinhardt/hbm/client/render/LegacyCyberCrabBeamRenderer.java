package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/** Exact solid RANDOM beam pass used by the 1.7.10 crab renderers. */
final class LegacyCyberCrabBeamRenderer {
    private LegacyCyberCrabBeamRenderer() {
    }

    static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       List<Vec3> targets, Vec3 origin, double emitterHeight, long gameTime) {
        if (targets.isEmpty()) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        int phase = (int) (gameTime % 1000L) + 1;
        poseStack.pushPose();
        poseStack.translate(0.0D, emitterHeight, 0.0D);
        for (Vec3 target : targets) {
            Vec3 skeleton = target.subtract(origin);
            double length = skeleton.length();
            if (length <= 1.0E-5D) {
                continue;
            }

            double horizontal = Math.sqrt(skeleton.x * skeleton.x + skeleton.z * skeleton.z);
            float yaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180.0D / Math.PI);
            float pitch = (float) (Math.atan2(skeleton.y, horizontal) * 180.0D / Math.PI);
            poseStack.pushPose();
            // BeamPronter starts along +Y and applies these rotations in this order.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));
            renderRandomSolidBeam(poseStack.last(), consumer, skeleton, phase,
                    Math.max(1, (int) (length * 5.0D)));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderRandomSolidBeam(PoseStack.Pose pose, VertexConsumer consumer,
                                              Vec3 skeleton, int start, int segments) {
        Random random = new Random(start);
        double segmentLength = skeleton.length() / segments;
        double lastX = 0.0D;
        double lastY = 0.0D;
        double lastZ = 0.0D;

        for (int index = 0; index <= segments; index++) {
            double angle = Math.PI * 2.0D * random.nextFloat();
            angle += Math.PI * 2.0D * random.nextFloat();
            double pointX = 0.125D * Math.cos(angle);
            double pointZ = -0.125D * Math.sin(angle);
            double pointY = segmentLength * index;

            if (index > 0) {
                for (int layer = 1; layer <= 2; layer++) {
                    double radius = 0.03125D / 2.0D * layer;
                    beamQuad(consumer, pose,
                            lastX + radius, lastY, lastZ + radius,
                            lastX + radius, lastY, lastZ - radius,
                            pointX + radius, pointY, pointZ - radius,
                            pointX + radius, pointY, pointZ + radius);
                    beamQuad(consumer, pose,
                            lastX - radius, lastY, lastZ + radius,
                            lastX - radius, lastY, lastZ - radius,
                            pointX - radius, pointY, pointZ - radius,
                            pointX - radius, pointY, pointZ + radius);
                    beamQuad(consumer, pose,
                            lastX + radius, lastY, lastZ + radius,
                            lastX - radius, lastY, lastZ + radius,
                            pointX - radius, pointY, pointZ + radius,
                            pointX + radius, pointY, pointZ + radius);
                    beamQuad(consumer, pose,
                            lastX + radius, lastY, lastZ - radius,
                            lastX - radius, lastY, lastZ - radius,
                            pointX - radius, pointY, pointZ - radius,
                            pointX + radius, pointY, pointZ - radius);
                }
            }

            lastX = pointX;
            lastY = pointY;
            lastZ = pointZ;
        }
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double ax, double ay, double az,
                                 double bx, double by, double bz,
                                 double cx, double cy, double cz,
                                 double dx, double dy, double dz) {
        vertex(consumer, pose, ax, ay, az);
        vertex(consumer, pose, bx, by, bz);
        vertex(consumer, pose, cx, cy, cz);
        vertex(consumer, pose, dx, dy, dz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(0x40, 0x40, 0x40, 0xFF)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
