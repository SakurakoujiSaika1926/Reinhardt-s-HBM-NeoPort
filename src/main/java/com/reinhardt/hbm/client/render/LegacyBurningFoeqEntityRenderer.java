package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBurningFoeqEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.Random;

/** Literal transform and flame-stack port of 1.7.10 RenderFOEQ. */
public final class LegacyBurningFoeqEntityRenderer extends EntityRenderer<LegacyBurningFoeqEntity> {
    private static final ResourceLocation BODY_MODEL_LOCATION =
            ReinhardtsHBM.id("models/entity/sat_foeq_burning.obj");
    private static final ResourceLocation FIRE_MODEL_LOCATION =
            ReinhardtsHBM.id("models/entity/sat_foeq_fire.obj");
    private static final ResourceLocation BODY_TEXTURE =
            ReinhardtsHBM.id("textures/entity/sat_foeq_burning.png");
    private static final LegacyEntityObjMesh BODY_MODEL = LegacyEntityObjMesh.load(BODY_MODEL_LOCATION);
    private static final LegacyEntityObjMesh FIRE_MODEL = LegacyEntityObjMesh.load(FIRE_MODEL_LOCATION);
    private static final RenderType FIRE_ADDITIVE = RenderType.create(
            "reinhardtshbm_burning_foeq_fire",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            4096,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .createCompositeState(false));
    private static final double VISUAL_RADIUS = calculateVisualRadius();

    public LegacyBurningFoeqEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyBurningFoeqEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -75.0D, 0.0D);
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F;
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        BODY_MODEL.renderAll(poseStack,
                bufferSource.getBuffer(RenderType.entityCutout(BODY_TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        VertexConsumer fire = bufferSource.getBuffer(FIRE_ADDITIVE);
        Random random = new Random(System.currentTimeMillis() / 50L);
        poseStack.scale(1.15F, 0.75F, 1.15F);
        poseStack.translate(0.0D, -0.5D, 0.3D);
        for (int index = 0; index < 10; index++) {
            renderFirePass(poseStack, fire, random, 0xFFFFBF3F);
            poseStack.translate(0.0D, 2.0D, 0.0D);
            renderFirePass(poseStack, fire, random, 0xFFFF7F00);
            poseStack.translate(0.0D, 2.0D, 0.0D);
            renderFirePass(poseStack, fire, random, 0xFFFF3F00);
            poseStack.translate(0.0D, 2.0D, 0.0D);
            renderFirePass(poseStack, fire, random, 0xFFFF2600);
            poseStack.translate(0.0D, -3.8D, 0.0D);
            poseStack.scale(0.95F, 1.2F, 0.95F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderFirePass(PoseStack poseStack, VertexConsumer consumer,
                                       Random random, int argb) {
        poseStack.mulPose(Axis.YP.rotationDegrees(random.nextInt(360)));
        FIRE_MODEL.renderAllPositionColorTriangles(poseStack, consumer, argb);
    }

    @Override
    public boolean shouldRender(LegacyBurningFoeqEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), VISUAL_RADIUS);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBurningFoeqEntity entity) {
        return BODY_TEXTURE;
    }

    /**
     * A derived upper bound for every authored vertex and every legal dynamic
     * flame transform. Rotations preserve length; translations and anisotropic
     * scales are accumulated using their exact maximum singular values.
     */
    private static double calculateVisualRadius() {
        double body = 75.0D + BODY_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D);
        double linearNorm = 1.15D;
        double translationNorm = linearNorm * Math.sqrt(0.5D * 0.5D + 0.3D * 0.3D);
        double fire = translationNorm + linearNorm * FIRE_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D);
        for (int loop = 0; loop < 10; loop++) {
            for (int pass = 0; pass < 3; pass++) {
                translationNorm += linearNorm * 2.0D;
                fire = Math.max(fire,
                        translationNorm + linearNorm * FIRE_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D));
            }
            translationNorm += linearNorm * 3.8D;
            linearNorm *= 1.2D;
        }
        return Math.nextUp(75.0D + Math.max(body - 75.0D, fire));
    }
}
