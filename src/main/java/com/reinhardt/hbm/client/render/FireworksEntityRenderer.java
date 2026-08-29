package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.FireworksEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Billboard letter matching the old ParticleLetter growth and fade curve. */
public final class FireworksEntityRenderer extends EntityRenderer<FireworksEntity> {
    public FireworksEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            FireworksEntity fireworks,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (!fireworks.exploded()) {
            return;
        }

        float age = fireworks.letterAge(partialTick);
        float progress = Math.min(age / FireworksEntity.LETTER_TICKS, 1.0F);
        float time = age * 4.0F / FireworksEntity.LETTER_TICKS;
        float scale = (float) (1.0D - Math.exp(-time));
        float alpha = Math.max(10.0F / 255.0F, 1.0F - progress);
        int argb = (int) (alpha * 255.0F) << 24 | fireworks.color();

        Font font = Minecraft.getInstance().font;
        String text = String.valueOf(fireworks.character());
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(-font.width(text) * 0.5F, -font.lineHeight * 0.5F, 0.0F);
        font.drawInBatch(text, 0.0F, 0.0F, argb, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FireworksEntity fireworks) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
