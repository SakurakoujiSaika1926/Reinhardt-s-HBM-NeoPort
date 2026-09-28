package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.LegacyDeathRayBoltEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** BOLT_LACUNAE renderer used by the death ray's MASKMAN_BOLT ring. */
public final class LegacyDeathRayBoltEntityRenderer extends EntityRenderer<LegacyDeathRayBoltEntity> {
    private static final int LACUNAE_COLOR = 0x4000BF;
    private static final double VISUAL_RADIUS = rotationSafeRadius(
            1.5D * Math.sqrt(3.0D * 3.0D + 0.25D * 0.25D + 0.25D * 0.25D));

    public LegacyDeathRayBoltEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyDeathRayBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        LegacyBossProjectileEntityRenderer.renderDart(
                entity, yaw, pitch, LACUNAE_COLOR, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyDeathRayBoltEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), VISUAL_RADIUS);
        return EntityRenderBounds.shouldRender(
                entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDeathRayBoltEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
