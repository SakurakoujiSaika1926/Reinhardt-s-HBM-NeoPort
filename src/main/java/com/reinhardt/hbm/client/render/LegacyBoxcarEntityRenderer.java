package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBoxcarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Literal EntityBoxcar branch of RenderBoxcar, including its authored offset. */
public final class LegacyBoxcarEntityRenderer extends EntityRenderer<LegacyBoxcarEntity> {
    private static final ResourceLocation MODEL_LOCATION = ReinhardtsHBM.id("models/entity/legacy_boxcar.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/legacy_boxcar.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(MODEL_LOCATION);

    public LegacyBoxcarEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyBoxcarEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -1.5D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE)),
                packedLight, 0, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBoxcarEntity entity) {
        return TEXTURE;
    }
}
