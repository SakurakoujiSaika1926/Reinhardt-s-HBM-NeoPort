package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.ZirnoxDebrisEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class ZirnoxDebrisEntityRenderer extends EntityRenderer<ZirnoxDebrisEntity> {
    private static final float DIAGONAL_AXIS = 0.5773502691896258F;

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
    public ResourceLocation getTextureLocation(ZirnoxDebrisEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
