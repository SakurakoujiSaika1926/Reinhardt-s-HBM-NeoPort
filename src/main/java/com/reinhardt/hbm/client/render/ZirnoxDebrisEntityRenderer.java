package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.ZirnoxDebrisEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.EnumMap;
import java.util.Map;

public final class ZirnoxDebrisEntityRenderer extends EntityRenderer<ZirnoxDebrisEntity> {
    private static final float DIAGONAL_AXIS = 0.5773502691896258F;
    private static final Map<ZirnoxDebrisEntity.DebrisType, Double> VISUAL_RADII =
            new EnumMap<>(ZirnoxDebrisEntity.DebrisType.class);

    public ZirnoxDebrisEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(ZirnoxDebrisEntity debris, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(debris.getId() % 360), 0.0F, 1.0F, 0.0F)));
        float rot = debris.lastRot + (debris.rot - debris.lastRot) * partialTick;
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(rot), DIAGONAL_AXIS, DIAGONAL_AXIS, DIAGONAL_AXIS)));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                debris.renderStack(), ItemDisplayContext.FIXED, packedLight, 0,
                poseStack, bufferSource, debris.level(), debris.getId());
        poseStack.popPose();
        super.render(debris, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(ZirnoxDebrisEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = visualRadius(entity);
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius),
                frustum, cameraX, cameraY, cameraZ);
    }

    static synchronized void invalidateBounds() {
        VISUAL_RADII.clear();
    }

    private static synchronized double visualRadius(ZirnoxDebrisEntity entity) {
        Double cached = VISUAL_RADII.get(entity.debrisType());
        if (cached != null) return cached;
        BakedItemGeometrySnapshot.Result geometry = BakedItemGeometrySnapshot.build(
                entity.renderStack(), new PoseStack(), net.minecraft.core.BlockPos.ZERO,
                entity.level(), ItemDisplayContext.FIXED);
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        double radius = Math.nextUp((geometry.maximumRadius() + 0.125D) * (1.0D + gamma64));
        VISUAL_RADII.put(entity.debrisType(), radius);
        return radius;
    }

    @Override
    public ResourceLocation getTextureLocation(ZirnoxDebrisEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
