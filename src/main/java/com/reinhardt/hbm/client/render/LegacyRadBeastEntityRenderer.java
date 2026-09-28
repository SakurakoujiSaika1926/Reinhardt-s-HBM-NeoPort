package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyRadBeastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Radiation Beast uses the old Blaze body and its dedicated texture. */
public final class LegacyRadBeastEntityRenderer
        extends MobRenderer<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/radbeast.png");
    private static final ResourceLocation MASK_TEXTURE = ReinhardtsHBM.id("textures/models/model_m65_blaze.png");
    private static final double BLAZE_MODEL_RADIUS = blazeModelRadius();
    private final double modelRadius;

    public LegacyRadBeastEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
        addLayer(new MaskLayer(this, new LegacyM65BlazeModel(context.bakeLayer(LegacyM65BlazeModel.LAYER))));
        // LivingEntityRenderer applies its fixed -1.501 model translation
        // outside the mask's dedicated 18/16 * 1.01 scale.  Head pitch/yaw
        // preserve the model-derived root radius.
        double maskScale = 18.0D / 16.0D * 1.01D;
        double maskRadius = 1.501D
                + LegacyM65BlazeModel.authoredMaximumRootRadius() * maskScale;
        this.modelRadius = Math.nextUp(Math.max(BLAZE_MODEL_RADIUS, maskRadius));
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyRadBeastEntity entity) {
        return TEXTURE;
    }

    /** EntityRADBeast#getBrightnessForRender always returned full-bright. */
    @Override
    protected int getBlockLightLevel(LegacyRadBeastEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(LegacyRadBeastEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        Vec3 target = beamTarget(entity);
        if (target == null) return;
        Vec3 origin = entity.position();
        LegacyCyberCrabBeamRenderer.render(poseStack, bufferSource, target,
                origin, 1.25D, entity.level().getGameTime());
    }

    @Override
    public boolean shouldRender(LegacyRadBeastEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = modelRadius * entity.getScale();
        if (entity.hasPose(Pose.SLEEPING)) {
            // LivingEntityRenderer applies the bed offset before entity scale,
            // so the translation itself remains in world units.
            radius += Math.abs(entity.getEyeHeight(Pose.STANDING) - 0.1D);
        }
        if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
            radius += entity.getBbHeight() + 0.1D;
        }
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), Math.nextUp(radius));
        Vec3 target = beamTarget(entity);
        if (target != null) {
            bounds = LegacyCyberCrabBeamRenderer.includeBeamBounds(
                    bounds, target, entity.position(), 1.25D);
        }
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

    private static Vec3 beamTarget(LegacyRadBeastEntity entity) {
        Entity victim = entity.getUnfortunateSoul();
        if (victim == null || entity.getY() <= 0.1D) {
            return null;
        }
        Vec3 target = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
        if (victim instanceof Player) {
            target = target.subtract(0.0D, 1.5D, 0.0D);
        }
        return target.distanceTo(entity.position()) < 200.0D ? target : null;
    }

    /**
     * Proved envelope of Minecraft 1.21.1 BlazeModel#createBodyLayer and
     * setupAnim after LivingEntityRenderer's fixed -1.501 translation.
     */
    private static double blazeModelRadius() {
        double headHorizontal = Math.sqrt(2.0D) * 0.25D;
        double head = Math.hypot(headHorizontal, 1.501D + 0.25D);

        // Each rod has a 2x8x2-pixel cube whose X/Z corner offset is at most
        // sqrt(2)*2/16. setupAnim moves the three rings on exact radii 9/16,
        // 7/16 and 5/16. The listed Y values are the extrema after the
        // renderer's fixed translation and Y mirror.
        double cornerHorizontal = Math.sqrt(2.0D) * (2.0D / 16.0D);
        double upper = Math.hypot(9.0D / 16.0D + cornerHorizontal,
                1.501D + 3.0D / 16.0D);
        double middle = Math.hypot(7.0D / 16.0D + cornerHorizontal,
                1.501D - 1.0D / 16.0D);
        double lower = Math.hypot(5.0D / 16.0D + cornerHorizontal,
                1.501D - 10.0D / 16.0D);
        return Math.nextUp(Math.max(Math.max(head, upper), Math.max(middle, lower)));
    }

    private static final class MaskLayer extends RenderLayer<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> {
        private final LegacyM65BlazeModel maskModel;

        private MaskLayer(RenderLayerParent<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> parent,
                          LegacyM65BlazeModel maskModel) {
            super(parent);
            this.maskModel = maskModel;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                           LegacyRadBeastEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            maskModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            poseStack.pushPose();
            // ModelM65Blaze explicitly scales the authored mask by 18/16 and then 1.01.
            float legacyScale = 18.0F / 16.0F * 1.01F;
            poseStack.scale(legacyScale, legacyScale, legacyScale);
            maskModel.renderToBuffer(poseStack,
                    bufferSource.getBuffer(RenderType.entityCutoutNoCull(MASK_TEXTURE)),
                    packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
            poseStack.popPose();
        }
    }
}
