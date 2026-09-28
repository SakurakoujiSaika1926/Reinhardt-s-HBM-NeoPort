package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;

/** Dedicated renderer for the original twenty-box Cyber Crab model. */
public final class LegacyCyberCrabEntityRenderer
        extends MobRenderer<LegacyCyberCrabEntity, LegacyCyberCrabModel> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/crab.png");

    public LegacyCyberCrabEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyCyberCrabModel(context.bakeLayer(LegacyCyberCrabModel.LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyCyberCrabEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(LegacyCyberCrabEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        model.setupAnim(entity, entity.walkAnimation.position(partialTick),
                entity.walkAnimation.speed(partialTick), entity.tickCount + partialTick,
                entity.getYRot(), entity.getXRot());
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyCyberCrabEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // LivingEntityRenderer's -1.501 model translation and this model's
        // +1.5 translation differ by exactly .001. All leg animation is a
        // rotation about an authored ModelPart pivot already included by the
        // layer-derived root radius.
        double radius = entity.getScale()
                * (LegacyCyberCrabModel.authoredMaximumRootRadius() + 0.001D);
        if (entity.hasPose(Pose.SLEEPING)) {
            // LivingEntityRenderer applies this bed offset before entity scale.
            radius += Math.abs(entity.getEyeHeight(Pose.STANDING) - 0.1D);
        }
        if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
            radius += entity.getBbHeight() + 0.1D;
        }
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), Math.nextUp(radius));
        if (EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ)) {
            return true;
        }
        Entity holder = entity instanceof Leashable leashable ? leashable.getLeashHolder() : null;
        return holder != null
                && EntityRenderBounds.isWithinRenderDistance(
                        entity, bounds, cameraX, cameraY, cameraZ)
                && frustum.isVisible(holder.getBoundingBoxForCulling());
    }
}
