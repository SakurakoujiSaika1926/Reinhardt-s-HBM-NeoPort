package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.MinerRocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Exact latest RenderDropship model and articulated landing-leg transform. */
public final class MinerRocketEntityRenderer extends EntityRenderer<MinerRocketEntity> {
    private static final ResourceLocation MODEL_LOCATION = ReinhardtsHBM.id("models/obj/entity/dropship.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/dropship.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(MODEL_LOCATION);
    private static final AABB LOCAL_VISUAL_BOUNDS = buildLocalVisualBounds();

    public MinerRocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(MinerRocketEntity rocket, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        var consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        poseStack.pushPose();
        MODEL.renderGroup("Pod", poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        float legs = rocket.previousLegs + (rocket.legs - rocket.previousLegs) * partialTick;
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            applyLegTransform(poseStack, index, legs);
            MODEL.renderGroup("Leg", poseStack, consumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(rocket, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(MinerRocketEntity rocket, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(rocket,
                EntityRenderBounds.moveToEntity(LOCAL_VISUAL_BOUNDS, rocket),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(MinerRocketEntity rocket) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static AABB buildLocalVisualBounds() {
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
        MODEL.includeGroupFacePositions("Pod", new PoseStack(), bounds);
        // The landing-leg animation is a monotonic rotation between these two
        // authored endpoints. Include both and the midpoint so the culling box
        // contains the complete swept volume without an arbitrary range.
        for (float legs : new float[] {0.0F, 0.5F, 1.0F}) {
            for (int index = 0; index < 4; index++) {
                PoseStack poseStack = new PoseStack();
                applyLegTransform(poseStack, index, legs);
                MODEL.includeGroupFacePositions("Leg", poseStack, bounds);
            }
        }
        return bounds.buildLocal();
    }

    private static void applyLegTransform(PoseStack poseStack, int index, float legs) {
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + 90.0F * index));
        poseStack.translate(0.5D, 1.75D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(150.0F * (1.0F - legs)));
        poseStack.translate(-0.5D, -1.75D, 0.0D);
    }
}
