package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
/** Exact solid RANDOM beam pass used by the 1.7.10 crab renderers. */
final class LegacyCyberCrabBeamRenderer {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    private LegacyCyberCrabBeamRenderer() {
    }

    static AABB includeBeamBounds(AABB bodyBounds, List<Vec3> targets,
                                  Vec3 origin, double emitterHeight) {
        AABB bounds = bodyBounds;
        for (Vec3 target : targets) {
            bounds = includeBeamBounds(bounds, target, origin, emitterHeight);
        }
        return bounds;
    }

    static AABB includeBeamBounds(AABB bodyBounds, Vec3 target,
                                  Vec3 origin, double emitterHeight) {
        double dx = target.x - origin.x;
        double dy = target.y - origin.y;
        double dz = target.z - origin.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 1.0E-5D) {
            return bodyBounds;
        }
        // Each random centerline point is exactly 0.125 from the beam axis;
        // the widest layer adds another 0.03125. A sphere around the emitted
        // start with length + 0.15625 contains every procedural phase.
        double radius = Math.nextUp(length + 0.15625D);
        return bodyBounds.minmax(EntityRenderBounds.radius(
                origin.x, origin.y + emitterHeight, origin.z, radius));
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
            double skeletonX = target.x - origin.x;
            double skeletonY = target.y - origin.y;
            double skeletonZ = target.z - origin.z;
            double length = Math.sqrt(skeletonX * skeletonX + skeletonY * skeletonY + skeletonZ * skeletonZ);
            if (length <= 1.0E-5D) {
                continue;
            }

            double horizontal = Math.sqrt(skeletonX * skeletonX + skeletonZ * skeletonZ);
            float yaw = (float) (Math.atan2(skeletonX, skeletonZ) * 180.0D / Math.PI);
            float pitch = (float) (Math.atan2(skeletonY, horizontal) * 180.0D / Math.PI);
            poseStack.pushPose();
            // BeamPronter starts along +Y and applies these rotations in this order.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));
            renderRandomSolidBeam(poseStack.last(), consumer, length, phase,
                    Math.max(1, (int) (length * 5.0D)));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       Vec3 target, Vec3 origin, double emitterHeight, long gameTime) {
        double skeletonX = target.x - origin.x;
        double skeletonY = target.y - origin.y;
        double skeletonZ = target.z - origin.z;
        double length = Math.sqrt(skeletonX * skeletonX + skeletonY * skeletonY + skeletonZ * skeletonZ);
        if (length <= 1.0E-5D) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        int phase = (int) (gameTime % 1000L) + 1;
        double horizontal = Math.sqrt(skeletonX * skeletonX + skeletonZ * skeletonZ);
        float yaw = (float) (Math.atan2(skeletonX, skeletonZ) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(skeletonY, horizontal) * 180.0D / Math.PI);
        poseStack.pushPose();
        poseStack.translate(0.0D, emitterHeight, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));
        renderRandomSolidBeam(poseStack.last(), consumer, length, phase,
                Math.max(1, (int) (length * 5.0D)));
        poseStack.popPose();
    }

    private static void renderRandomSolidBeam(PoseStack.Pose pose, VertexConsumer consumer,
                                              double length, int start, int segments) {
        long randomState = (start ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        double segmentLength = length / segments;
        double lastX = 0.0D;
        double lastY = 0.0D;
        double lastZ = 0.0D;

        for (int index = 0; index <= segments; index++) {
            randomState = advanceJavaRandom(randomState);
            double angle = Math.PI * 2.0D * ((float) (randomState >>> 24) * 0x1.0p-24F);
            randomState = advanceJavaRandom(randomState);
            angle += Math.PI * 2.0D * ((float) (randomState >>> 24) * 0x1.0p-24F);
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

    private static long advanceJavaRandom(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
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
