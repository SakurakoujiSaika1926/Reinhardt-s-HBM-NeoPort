package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBossProjectileEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Random;

/** Legacy RenderBullet styles used by the chopper, BOTPrime and UFO bosses. */
public final class LegacyBossProjectileEntityRenderer extends EntityRenderer<LegacyBossProjectileEntity> {
    private static final ModelResourceLocation ROCKET = MachineModelRenderer.standalone("entity/legacy_rocket");
    private static final BlockState RENDER_STATE = Blocks.STONE.defaultBlockState();
    private static final ResourceLocation CHOPPER_TEXTURE = ReinhardtsHBM.id("models/emplacer");

    public LegacyBossProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ROCKET);
    }

    @Override
    public void render(LegacyBossProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        if (entity.projectileType() == LegacyBossProjectileEntity.Type.UFO_ROCKET) {
            renderRocket(yaw, pitch, poseStack, bufferSource, packedLight);
        } else if (entity.projectileType() == LegacyBossProjectileEntity.Type.CHOPPER_BULLET) {
            renderChopperBullet(entity, yaw, pitch, poseStack, bufferSource, packedLight);
        } else {
            int color = switch (entity.projectileType()) {
                case WORM_LASER -> 0xFF0000;
                case WORM_BOLT -> 0x00FF00;
                default -> 0xFFFFFF;
            };
            renderDart(entity, yaw, pitch, color, poseStack, bufferSource);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderRocket(float yaw, float pitch, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ROCKET), poseStack, bufferSource,
                RENDER_STATE, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderDart(LegacyBossProjectileEntity entity, float yaw, float pitch, int color,
                                   PoseStack poseStack,
                                   MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(new Random(entity.getId()).nextInt(90) - 45.0F));
        poseStack.scale(0.25F, 0.125F, 0.125F);
        poseStack.scale(-1.0F, 1.0F, 1.0F);
        poseStack.scale(2.0F, 2.0F, 2.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        renderDartGeometry(consumer, poseStack.last(), color);
        poseStack.popPose();
    }

    private static void renderDartGeometry(VertexConsumer consumer, PoseStack.Pose pose, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;

        dartTriangle(consumer, pose, 6, 0, 0, 3, -1, -1, 3, 1, -1, red, green, blue, 255, 0, 0);
        dartTriangle(consumer, pose, 3, -1, 1, 6, 0, 0, 3, 1, 1, red, green, blue, 0, 255, 0);
        dartTriangle(consumer, pose, 3, -1, -1, 6, 0, 0, 3, -1, 1, red, green, blue, 0, 255, 0);
        dartTriangle(consumer, pose, 6, 0, 0, 3, 1, -1, 3, 1, 1, red, green, blue, 255, 0, 0);

        dartTriangle(consumer, pose, 6, 0, 0, 4, -0.5, -0.5, 4, 0.5, -0.5, red, green, blue, 255, 255, 255);
        dartTriangle(consumer, pose, 4, -0.5, 0.5, 6, 0, 0, 4, 0.5, 0.5, red, green, blue, 255, 255, 255);
        dartTriangle(consumer, pose, 4, -0.5, -0.5, 6, 0, 0, 4, -0.5, 0.5, red, green, blue, 255, 255, 255);
        dartTriangle(consumer, pose, 6, 0, 0, 4, 0.5, -0.5, 4, 0.5, 0.5, red, green, blue, 255, 255, 255);

        dartQuad(consumer, pose, 4, 0.5, -0.5, 4, 0.5, 0.5, 0, 0.5, 0.5, 0, 0.5, -0.5, red, green, blue);
        dartQuad(consumer, pose, 4, -0.5, -0.5, 4, -0.5, 0.5, 0, -0.5, 0.5, 0, -0.5, -0.5, red, green, blue);
        dartQuad(consumer, pose, 4, -0.5, 0.5, 4, 0.5, 0.5, 0, 0.5, 0.5, 0, -0.5, 0.5, red, green, blue);
        dartQuad(consumer, pose, 4, -0.5, -0.5, 4, 0.5, -0.5, 0, 0.5, -0.5, 0, -0.5, -0.5, red, green, blue);
    }

    private static void dartTriangle(VertexConsumer consumer, PoseStack.Pose pose,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     double x3, double y3, double z3,
                                     int red, int green, int blue,
                                     int alpha1, int alpha2, int alpha3) {
        vertex(consumer, pose, x1, y1, z1, red, green, blue, alpha1);
        vertex(consumer, pose, x2, y2, z2, red, green, blue, alpha2);
        vertex(consumer, pose, x3, y3, z3, red, green, blue, alpha3);
    }

    private static void dartQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3,
                                 double x4, double y4, double z4,
                                 int red, int green, int blue) {
        vertex(consumer, pose, x1, y1, z1, red, green, blue, 255);
        vertex(consumer, pose, x2, y2, z2, red, green, blue, 255);
        vertex(consumer, pose, x3, y3, z3, red, green, blue, 0);
        vertex(consumer, pose, x4, y4, z4, red, green, blue, 0);
    }

    private static void renderChopperBullet(LegacyBossProjectileEntity entity, float yaw, float pitch,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(new Random(entity.getId()).nextInt(360)));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(CHOPPER_TEXTURE));
        renderChopperCube(consumer, poseStack.last(), packedLight);
        poseStack.popPose();
    }

    private static void renderChopperCube(VertexConsumer consumer, PoseStack.Pose pose, int packedLight) {
        float minX = 1.0F / 16.0F;
        float maxX = 3.0F / 16.0F;
        float min = -0.5F / 16.0F;
        float max = 0.5F / 16.0F;
        chopperQuad(consumer, pose, minX, min, min, maxX, min, max, maxX, max, max, minX, max, min, packedLight, 0, 0, 1, 1);
        chopperQuad(consumer, pose, maxX, min, min, minX, min, max, minX, max, max, maxX, max, min, packedLight, 0, 0, 1, 1);
        chopperQuad(consumer, pose, minX, min, min, maxX, min, min, maxX, max, min, minX, max, min, packedLight, 0, 0, 1, 1);
        chopperQuad(consumer, pose, minX, min, max, maxX, min, max, maxX, max, max, minX, max, max, packedLight, 0, 0, 1, 1);
        chopperQuad(consumer, pose, minX, min, max, minX, min, min, minX, max, min, minX, max, max, packedLight, 0, 0, 1, 1);
        chopperQuad(consumer, pose, maxX, min, min, maxX, min, max, maxX, max, max, maxX, max, min, packedLight, 0, 0, 1, 1);
    }

    private static void chopperQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                    float x1, float y1, float z1, float x2, float y2, float z2,
                                    float x3, float y3, float z3, float x4, float y4, float z4,
                                    int packedLight, float u1, float v1, float u2, float v2) {
        chopperVertex(consumer, pose, x1, y1, z1, u1, v1, packedLight);
        chopperVertex(consumer, pose, x2, y2, z2, u2, v1, packedLight);
        chopperVertex(consumer, pose, x3, y3, z3, u2, v2, packedLight);
        chopperVertex(consumer, pose, x4, y4, z4, u1, v2, packedLight);
    }

    private static void chopperVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                      float x, float y, float z, float u, float v, int packedLight) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
                                int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBossProjectileEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
