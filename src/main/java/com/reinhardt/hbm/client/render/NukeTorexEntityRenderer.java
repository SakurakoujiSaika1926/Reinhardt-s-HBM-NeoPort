package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.NukeTorexEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class NukeTorexEntityRenderer extends EntityRenderer<NukeTorexEntity> {
    private static final ResourceLocation CLOUDLET = ReinhardtsHBM.id("textures/particle/particle_base.png");
    private static final ResourceLocation FLASH = ReinhardtsHBM.id("textures/particle/flare.png");
    private static final RenderType CLOUDLET_TYPE = RenderType.entityTranslucent(CLOUDLET);
    private static final RenderType FLASH_TYPE = RenderType.entityTranslucentEmissive(FLASH);

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

    private void renderCloudlets(NukeTorexEntity cloud, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(CLOUDLET_TYPE);
        PoseStack.Pose pose = poseStack.last();
        List<NukeTorexEntity.Cloudlet> cloudlets = new ArrayList<>(cloud.cloudlets);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            cloudlets.sort(Comparator.comparingDouble((NukeTorexEntity.Cloudlet c) -> player.distanceToSqr(c.posX, c.posY, c.posZ)).reversed());
        }
        for (NukeTorexEntity.Cloudlet cloudlet : cloudlets) {
            Vec3 vec = cloudlet.interpolatedPos(partialTick);
            Vec3 local = vec.subtract(cloud.position());
            renderCloudlet(pose, consumer, local, cloudlet, partialTick);
        }
    }

    private void renderFlash(NukeTorexEntity cloud, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(FLASH_TYPE);
        PoseStack.Pose pose = poseStack.last();
        double age = Math.min(cloud.tickCount + partialTick, 100.0D);
        float alpha = (float) ((100.0D - age) / 100.0D);
        Random random = new Random(cloud.getId());
        for (int i = 0; i < 3; i++) {
            double x = random.nextGaussian() * 0.5D * cloud.rollerSize;
            double y = random.nextGaussian() * 0.5D * cloud.rollerSize + cloud.coreHeight;
            double z = random.nextGaussian() * 0.5D * cloud.rollerSize;
            renderBillboard(pose, consumer, new Vec3(x, y, z), (float) (25.0D * cloud.rollerSize), 1.0F, 1.0F, 1.0F, alpha);
        }
    }

    private void renderCloudlet(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, NukeTorexEntity.Cloudlet cloudlet, float partialTick) {
        float alpha = cloudlet.alpha();
        if (alpha <= 0.0F) {
            return;
        }
        float scale = cloudlet.renderScale();
        float brightness = cloudlet.type == NukeTorexEntity.TorexType.CONDENSATION ? 0.9F : 0.75F * cloudlet.colorMod;
        Vec3 color = cloudlet.interpolatedColor(partialTick);
        renderBillboard(
                pose,
                consumer,
                pos,
                scale,
                (float) color.x * brightness,
                (float) color.y * brightness,
                (float) color.z * brightness,
                alpha
        );
    }

    private void renderBillboard(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, float scale, float red, float green, float blue, float alpha) {
        Quaternionf camera = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        Vec3[] corners = {
                rotate(new Vec3(-scale, -scale, 0.0D), camera).add(pos),
                rotate(new Vec3(-scale, scale, 0.0D), camera).add(pos),
                rotate(new Vec3(scale, scale, 0.0D), camera).add(pos),
                rotate(new Vec3(scale, -scale, 0.0D), camera).add(pos)
        };
        vertex(consumer, pose, corners[0], red, green, blue, alpha, 1.0F, 1.0F);
        vertex(consumer, pose, corners[1], red, green, blue, alpha, 1.0F, 0.0F);
        vertex(consumer, pose, corners[2], red, green, blue, alpha, 0.0F, 0.0F);
        vertex(consumer, pose, corners[3], red, green, blue, alpha, 0.0F, 1.0F);
    }

    private Vec3 rotate(Vec3 vec, Quaternionf rotation) {
        org.joml.Vector3f joml = new org.joml.Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
        joml.rotate(rotation);
        return new Vec3(joml.x, joml.y, joml.z);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float red, float green, float blue, float alpha, float u, float v) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
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
}
