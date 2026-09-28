package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LambdaRocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public final class LambdaRocketEntityRenderer extends EntityRenderer<LambdaRocketEntity> {
    private static final ResourceLocation OBJ =
            ReinhardtsHBM.id("models/obj/entity/lambda_rocket.obj");
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/lambda_rocket.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(OBJ);
    private static final AABB LOCAL_BOUNDS = bounds();

    public LambdaRocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(LambdaRocketEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public boolean shouldRender(LambdaRocketEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(LOCAL_BOUNDS, entity),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LambdaRocketEntity entity) {
        return TEXTURE;
    }

    private static AABB bounds() {
        EntityRenderBounds.FloatBuilder builder = new EntityRenderBounds.FloatBuilder();
        MODEL.includeAllFacePositions(new PoseStack(), builder);
        return builder.buildLocal();
    }
}
