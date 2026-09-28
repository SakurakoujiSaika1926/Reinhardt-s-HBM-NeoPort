package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.NukeTorexEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

public class NukeTorexEntityRenderer extends EntityRenderer<NukeTorexEntity> {
    private static final ResourceLocation CLOUDLET = ReinhardtsHBM.id("textures/particle/particle_base.png");
    private static final ResourceLocation FLASH = ReinhardtsHBM.id("textures/particle/flare.png");
    private static final RenderType CLOUDLET_TYPE = RenderType.entityTranslucent(CLOUDLET);
    private static final RenderType FLASH_TYPE = RenderType.entityTranslucentEmissive(FLASH);
    private final List<NukeTorexEntity.Cloudlet> sortedCloudlets = new ArrayList<>();
    private final Comparator<NukeTorexEntity.Cloudlet> cloudletDistanceComparator = this::compareCloudletDistance;
    private final Map<NukeTorexEntity, FlashGeometry> flashGeometry = new WeakHashMap<>();
    private double sortX;
    private double sortY;
    private double sortZ;

    public NukeTorexEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NukeTorexEntity cloud, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        renderCloudlets(cloud, partialTick, poseStack, bufferSource);
        if (cloud.tickCount < 101) {
            renderFlash(cloud, partialTick, poseStack, bufferSource);
        }
        poseStack.popPose();
        super.render(cloud, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(NukeTorexEntity cloud, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(
                cloud, visualBounds(cloud), frustum, cameraX, cameraY, cameraZ);
    }

    private AABB visualBounds(NukeTorexEntity cloud) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        boolean hasVisual = false;
        for (NukeTorexEntity.Cloudlet cloudlet : cloud.cloudlets) {
            if (cloudlet.alpha() <= 0.0F) {
                continue;
            }
            double positionScale = cloudlet.type == NukeTorexEntity.TorexType.SHOCK
                    ? 1.0D : (float) cloud.getScale();
            double previousX = cloud.getX() + (cloudlet.prevPosX - cloud.getX()) * positionScale;
            double previousY = cloud.getY() + (cloudlet.prevPosY - cloud.getY()) * positionScale;
            double previousZ = cloud.getZ() + (cloudlet.prevPosZ - cloud.getZ()) * positionScale;
            double currentX = cloud.getX() + (cloudlet.posX - cloud.getX()) * positionScale;
            double currentY = cloud.getY() + (cloudlet.posY - cloud.getY()) * positionScale;
            double currentZ = cloud.getZ() + (cloudlet.posZ - cloud.getZ()) * positionScale;
            double extent = billboardSweepExtent(cloudlet.renderScale());
            minX = Math.min(minX, Math.min(previousX, currentX) - extent);
            minY = Math.min(minY, Math.min(previousY, currentY) - extent);
            minZ = Math.min(minZ, Math.min(previousZ, currentZ) - extent);
            maxX = Math.max(maxX, Math.max(previousX, currentX) + extent);
            maxY = Math.max(maxY, Math.max(previousY, currentY) + extent);
            maxZ = Math.max(maxZ, Math.max(previousZ, currentZ) + extent);
            hasVisual = true;
        }
        if (cloud.tickCount < 101) {
            FlashGeometry flash = flashGeometry(cloud);
            double extent = billboardSweepExtent((float) (25.0D * cloud.rollerSize));
            for (int offset = 0; offset < flash.positions().length; offset += 3) {
                double x = cloud.getX() + flash.positions()[offset];
                double y = cloud.getY() + flash.positions()[offset + 1];
                double z = cloud.getZ() + flash.positions()[offset + 2];
                minX = Math.min(minX, x - extent);
                minY = Math.min(minY, y - extent);
                minZ = Math.min(minZ, z - extent);
                maxX = Math.max(maxX, x + extent);
                maxY = Math.max(maxY, y + extent);
                maxZ = Math.max(maxZ, z + extent);
            }
            hasVisual = true;
        }
        return hasVisual ? new AABB(minX, minY, minZ, maxX, maxY, maxZ)
                : cloud.getBoundingBoxForCulling();
    }

    private static double billboardSweepExtent(float scale) {
        /* A camera-facing square's four corners have radius scale*sqrt(2).
         * gamma(32) contains the normalized-quaternion coefficient and FMA
         * path used by renderBillboard for every camera orientation. */
        double unitRoundoff = 0x1.0p-24;
        double gamma32 = 32.0D * unitRoundoff / (1.0D - 32.0D * unitRoundoff);
        return Math.nextUp(Math.abs((double) scale) * Math.sqrt(2.0D) * (1.0D + gamma32));
    }

    private void renderCloudlets(NukeTorexEntity cloud, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(CLOUDLET_TYPE);
        PoseStack.Pose pose = poseStack.last();
        this.sortedCloudlets.clear();
        this.sortedCloudlets.addAll(cloud.cloudlets);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            this.sortX = player.getX();
            this.sortY = player.getY();
            this.sortZ = player.getZ();
            this.sortedCloudlets.sort(this.cloudletDistanceComparator);
        }
        for (NukeTorexEntity.Cloudlet cloudlet : this.sortedCloudlets) {
            renderCloudlet(cloud, pose, consumer, cloudlet, partialTick);
        }
    }

    private int compareCloudletDistance(NukeTorexEntity.Cloudlet first, NukeTorexEntity.Cloudlet second) {
        return Double.compare(cloudletDistanceSqr(second), cloudletDistanceSqr(first));
    }

    private double cloudletDistanceSqr(NukeTorexEntity.Cloudlet cloudlet) {
        double x = this.sortX - cloudlet.posX;
        double y = this.sortY - cloudlet.posY;
        double z = this.sortZ - cloudlet.posZ;
        return x * x + y * y + z * z;
    }

    private void renderFlash(NukeTorexEntity cloud, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(FLASH_TYPE);
        PoseStack.Pose pose = poseStack.last();
        double age = Math.min(cloud.tickCount + partialTick, 100.0D);
        float alpha = (float) ((100.0D - age) / 100.0D);
        FlashGeometry geometry = flashGeometry(cloud);
        for (int i = 0; i < 3; i++) {
            int offset = i * 3;
            renderBillboard(pose, consumer,
                    geometry.positions()[offset], geometry.positions()[offset + 1], geometry.positions()[offset + 2],
                    (float) (25.0D * cloud.rollerSize), 1.0F, 1.0F, 1.0F, alpha);
        }
    }

    private void renderCloudlet(NukeTorexEntity cloud, PoseStack.Pose pose, VertexConsumer consumer,
                                NukeTorexEntity.Cloudlet cloudlet, float partialTick) {
        float alpha = cloudlet.alpha();
        if (alpha <= 0.0F) {
            return;
        }
        float scale = cloudlet.renderScale();
        float brightness = cloudlet.type == NukeTorexEntity.TorexType.CONDENSATION ? 0.9F : 0.75F * cloudlet.colorMod;
        double positionScale = cloudlet.type == NukeTorexEntity.TorexType.SHOCK ? 1.0D : (float) cloud.getScale();
        double x = (net.minecraft.util.Mth.lerp(partialTick, cloudlet.prevPosX, cloudlet.posX) - cloud.getX()) * positionScale;
        double y = (net.minecraft.util.Mth.lerp(partialTick, cloudlet.prevPosY, cloudlet.posY) - cloud.getY()) * positionScale;
        double z = (net.minecraft.util.Mth.lerp(partialTick, cloudlet.prevPosZ, cloudlet.posZ) - cloud.getZ()) * positionScale;
        float red;
        float green;
        float blue;
        if (cloudlet.type == NukeTorexEntity.TorexType.CONDENSATION) {
            red = brightness;
            green = brightness;
            blue = brightness;
        } else {
            double greying = cloud.greying() + (cloudlet.type == NukeTorexEntity.TorexType.RING ? 1.0D : 0.0D);
            red = (float) (net.minecraft.util.Mth.lerp(partialTick, cloudlet.prevColor.x, cloudlet.color.x) * greying) * brightness;
            green = (float) (net.minecraft.util.Mth.lerp(partialTick, cloudlet.prevColor.y, cloudlet.color.y) * greying) * brightness;
            blue = (float) (net.minecraft.util.Mth.lerp(partialTick, cloudlet.prevColor.z, cloudlet.color.z) * greying) * brightness;
        }
        renderBillboard(
                pose,
                consumer,
                x, y, z,
                scale,
                red, green, blue,
                alpha
        );
    }

    private FlashGeometry flashGeometry(NukeTorexEntity cloud) {
        FlashGeometry previous = this.flashGeometry.get(cloud);
        if (previous != null
                && Double.doubleToLongBits(previous.rollerSize()) == Double.doubleToLongBits(cloud.rollerSize)
                && Double.doubleToLongBits(previous.coreHeight()) == Double.doubleToLongBits(cloud.coreHeight)) {
            return previous;
        }
        Random random = new Random(cloud.getId());
        double[] positions = new double[9];
        for (int i = 0; i < 3; i++) {
            int offset = i * 3;
            positions[offset] = random.nextGaussian() * 0.5D * cloud.rollerSize;
            positions[offset + 1] = random.nextGaussian() * 0.5D * cloud.rollerSize + cloud.coreHeight;
            positions[offset + 2] = random.nextGaussian() * 0.5D * cloud.rollerSize;
        }
        FlashGeometry next = new FlashGeometry(cloud.rollerSize, cloud.coreHeight, positions);
        this.flashGeometry.put(cloud, next);
        return next;
    }

    private void renderBillboard(PoseStack.Pose pose, VertexConsumer consumer,
                                 double x, double y, double z,
                                 float scale, float red, float green, float blue, float alpha) {
        Quaternionf camera = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        float qx = camera.x();
        float qy = camera.y();
        float qz = camera.z();
        float qw = camera.w();
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float ww = qw * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float xw = qx * qw;
        float zw = qz * qw;
        float yw = qy * qw;
        float inverseNorm = 1.0F / (xx + yy + zz + ww);
        float m00 = (xx - yy - zz + ww) * inverseNorm;
        float m01 = 2.0F * (xy - zw) * inverseNorm;
        float m10 = 2.0F * (xy + zw) * inverseNorm;
        float m11 = (yy - xx - zz + ww) * inverseNorm;
        float m20 = 2.0F * (xz - yw) * inverseNorm;
        float m21 = 2.0F * (yz + xw) * inverseNorm;
        float c0x = Math.fma(m00, -scale, Math.fma(m01, -scale, 0.0F));
        float c0y = Math.fma(m10, -scale, Math.fma(m11, -scale, 0.0F));
        float c0z = Math.fma(m20, -scale, Math.fma(m21, -scale, 0.0F));
        float c1x = Math.fma(m00, -scale, Math.fma(m01, scale, 0.0F));
        float c1y = Math.fma(m10, -scale, Math.fma(m11, scale, 0.0F));
        float c1z = Math.fma(m20, -scale, Math.fma(m21, scale, 0.0F));
        float c2x = Math.fma(m00, scale, Math.fma(m01, scale, 0.0F));
        float c2y = Math.fma(m10, scale, Math.fma(m11, scale, 0.0F));
        float c2z = Math.fma(m20, scale, Math.fma(m21, scale, 0.0F));
        float c3x = Math.fma(m00, scale, Math.fma(m01, -scale, 0.0F));
        float c3y = Math.fma(m10, scale, Math.fma(m11, -scale, 0.0F));
        float c3z = Math.fma(m20, scale, Math.fma(m21, -scale, 0.0F));
        vertex(consumer, pose, x + c0x, y + c0y, z + c0z,
                red, green, blue, alpha, 1.0F, 1.0F);
        vertex(consumer, pose, x + c1x, y + c1y, z + c1z,
                red, green, blue, alpha, 1.0F, 0.0F);
        vertex(consumer, pose, x + c2x, y + c2y, z + c2z,
                red, green, blue, alpha, 0.0F, 0.0F);
        vertex(consumer, pose, x + c3x, y + c3y, z + c3z,
                red, green, blue, alpha, 0.0F, 1.0F);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                        double x, double y, double z,
                        float red, float green, float blue, float alpha, float u, float v) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor(clampColor(red), clampColor(green), clampColor(blue), clampColor(alpha))
                .setUv(u, v)
                .setOverlay(0)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private int clampColor(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    @Override
    public ResourceLocation getTextureLocation(NukeTorexEntity entity) {
        return CLOUDLET;
    }

    private record FlashGeometry(double rollerSize, double coreHeight, double[] positions) {
    }
}
