package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyChopperEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Direct modern transform of RenderHunterChopper, including the original Techne geometry and rotor animation. */
public final class LegacyChopperEntityRenderer extends EntityRenderer<LegacyChopperEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/chopper.png");
    private final LegacyChopperModel model;
    private final double visualRadius;

    public LegacyChopperEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new LegacyChopperModel(context.bakeLayer(LegacyChopperModel.LAYER));
        visualRadius = rotationSafeRadius(LegacyChopperModel.authoredMaximumRootRadius() * 0.25D);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyChopperEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 2.75D, 0.0D);
        poseStack.scale(4.0F, 4.0F, 4.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        // RenderHunterChopper interpolated both angles before applying its
        // authored Y/Z rotations. Keep the same linear interpolation here.
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        // The legacy renderer retained back-face culling. Each zero-thickness
        // rotor box already supplies its two outward-facing textured faces;
        // rendering their back faces too overlays both UVs on the same plane.
        model.render(poseStack, bufferSource.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyChopperEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY() + 2.75D, entity.getZ(), visualRadius);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyChopperEntity entity) {
        return TEXTURE;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
