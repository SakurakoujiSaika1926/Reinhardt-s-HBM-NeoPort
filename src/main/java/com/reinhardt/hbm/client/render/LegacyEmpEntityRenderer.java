package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyEmpEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Literal RenderEMPBlast port. The old ring is unlit, translucent and uncullled. */
public final class LegacyEmpEntityRenderer extends EntityRenderer<LegacyEmpEntity> {
    private static final ResourceLocation MODEL_LOCATION = ReinhardtsHBM.id("models/entity/legacy_emp_ring.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/legacy_emp.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(MODEL_LOCATION);

    public LegacyEmpEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyEmpEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (!entity.persistent()) {
            poseStack.scale(entity.scale(), 1.0F, entity.scale());
            MODEL.renderAll(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                    LightTexture.FULL_BRIGHT, 0, 0xFFFFFFFF);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyEmpEntity entity) {
        return TEXTURE;
    }
}
