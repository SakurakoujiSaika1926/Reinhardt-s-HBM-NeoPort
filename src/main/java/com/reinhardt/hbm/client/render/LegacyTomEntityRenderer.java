package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyTomEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Random;

/** Literal TomPronter/RenderTom port, including the authored 100x model space. */
public final class LegacyTomEntityRenderer extends EntityRenderer<LegacyTomEntity> {
    private static final ResourceLocation MAIN_MODEL_LOCATION = ReinhardtsHBM.id("models/entity/weapons/legacy_tom_main.obj");
    private static final ResourceLocation FLAME_MODEL_LOCATION = ReinhardtsHBM.id("models/entity/weapons/legacy_tom_flame.obj");
    private static final ResourceLocation MAIN_TEXTURE = ReinhardtsHBM.id("textures/entity/weapons/legacy_tom_main.png");
    private static final ResourceLocation FLAME_TEXTURE = ReinhardtsHBM.id("textures/entity/weapons/legacy_tom_flame.png");
    private static final LegacyEntityObjMesh MAIN_MODEL = LegacyEntityObjMesh.load(MAIN_MODEL_LOCATION);
    private static final LegacyEntityObjMesh FLAME_MODEL = LegacyEntityObjMesh.load(FLAME_MODEL_LOCATION);
    private static final int[] FLAME_RANDOM_ANGLES = buildFlameRandomAngles();
    private static final AABB LOCAL_VISUAL_BOUNDS = buildLocalVisualBounds();

    public LegacyTomEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyTomEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -50.0D, 0.0D);
        poseStack.scale(100.0F, 100.0F, 100.0F);
        MAIN_MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(MAIN_TEXTURE)),
                LightTexture.FULL_BRIGHT, 0, 0xFFFFFFFF);

        // TomPronter uses a deterministic random sequence and twenty additive
        // flame passes; preserve those exact per-pass rotations and scales.
        poseStack.scale(0.8F, 5.0F, 0.8F);
        float rotation = (float) ((-System.currentTimeMillis() / 10L) % 360L);
        for (int i = 0; i < 20; i++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation + FLAME_RANDOM_ANGLES[i]));
            FLAME_MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(FLAME_TEXTURE)),
                    LightTexture.FULL_BRIGHT, 0, 0xFFFFFFFF);
            poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
            poseStack.scale(-1.015F, 0.9F, 1.015F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyTomEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(LOCAL_VISUAL_BOUNDS, entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTomEntity entity) {
        return MAIN_TEXTURE;
    }

    private static int[] buildFlameRandomAngles() {
        Random random = new Random(0L);
        int[] angles = new int[20];
        for (int index = 0; index < angles.length; index++) {
            angles[index] = random.nextInt(90);
        }
        return angles;
    }

    private static AABB buildLocalVisualBounds() {
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();

        PoseStack mainPose = new PoseStack();
        mainPose.translate(0.0D, -50.0D, 0.0D);
        mainPose.scale(100.0F, 100.0F, 100.0F);
        MAIN_MODEL.includeAllFacePositions(mainPose, bounds);

        // render() derives an integer degree from currentTimeMillis/10.  Cover
        // every reachable negative modulo phase and replay the exact cumulative
        // PoseStack sequence used by all twenty flame passes.
        for (int phase = 0; phase < 360; phase++) {
            float rotation = -phase;
            PoseStack flamePose = new PoseStack();
            flamePose.translate(0.0D, -50.0D, 0.0D);
            flamePose.scale(100.0F, 100.0F, 100.0F);
            flamePose.scale(0.8F, 5.0F, 0.8F);
            for (int index = 0; index < FLAME_RANDOM_ANGLES.length; index++) {
                flamePose.mulPose(Axis.YP.rotationDegrees(rotation + FLAME_RANDOM_ANGLES[index]));
                FLAME_MODEL.includeAllFacePositions(flamePose, bounds);
                flamePose.mulPose(Axis.YP.rotationDegrees(-rotation));
                flamePose.scale(-1.015F, 0.9F, 1.015F);
            }
        }
        return bounds.buildLocal();
    }
}
