package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyTomEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/** Literal TomPronter/RenderTom port, including the authored 100x model space. */
public final class LegacyTomEntityRenderer extends EntityRenderer<LegacyTomEntity> {
    private static final ResourceLocation MAIN_MODEL_LOCATION = ReinhardtsHBM.id("models/entity/weapons/legacy_tom_main.obj");
    private static final ResourceLocation FLAME_MODEL_LOCATION = ReinhardtsHBM.id("models/entity/weapons/legacy_tom_flame.obj");
    private static final ResourceLocation MAIN_TEXTURE = ReinhardtsHBM.id("textures/entity/weapons/legacy_tom_main.png");
    private static final ResourceLocation FLAME_TEXTURE = ReinhardtsHBM.id("textures/entity/weapons/legacy_tom_flame.png");
    private static final LegacyEntityObjMesh MAIN_MODEL = LegacyEntityObjMesh.load(MAIN_MODEL_LOCATION);
    private static final LegacyEntityObjMesh FLAME_MODEL = LegacyEntityObjMesh.load(FLAME_MODEL_LOCATION);

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
        Random random = new Random(0L);
        float rotation = (float) ((-System.currentTimeMillis() / 10L) % 360L);
        for (int i = 0; i < 20; i++) {
            int randomAngle = random.nextInt(90);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation + randomAngle));
            FLAME_MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(FLAME_TEXTURE)),
                    LightTexture.FULL_BRIGHT, 0, 0xFFFFFFFF);
            poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
            poseStack.scale(-1.015F, 0.9F, 1.015F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTomEntity entity) {
        return MAIN_TEXTURE;
    }
}
