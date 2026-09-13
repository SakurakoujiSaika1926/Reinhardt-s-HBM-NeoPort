package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.ClusterSubmunitionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Exact LegoClient.RENDER_BOMB geometry used by cluster submunitions. */
public final class ClusterSubmunitionEntityRenderer extends EntityRenderer<ClusterSubmunitionEntity> {
    private static final ResourceLocation MODEL_LOCATION = ReinhardtsHBM.id("models/entity/legacy_fatman.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/legacy_cluster_submunition.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(MODEL_LOCATION);

    public ClusterSubmunitionEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ClusterSubmunitionEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // RenderBulletMK4's common orientation, followed by the exact
        // RENDER_BOMB transform (no shared auto-fit or fallback model).
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot() + 180.0F));
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, 1.0D);
        MODEL.renderGroup("MiniNuke", poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE)),
                packedLight, 0, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ClusterSubmunitionEntity entity) {
        return TEXTURE;
    }
}
