package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.entity.FireworksEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Billboard letter matching the old ParticleLetter growth and fade curve. */
public final class FireworksEntityRenderer extends EntityRenderer<FireworksEntity> {
    private static final Map<Integer, Double> GLYPH_RADII = new ConcurrentHashMap<>();
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
    public boolean shouldRender(FireworksEntity fireworks, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        if (!fireworks.exploded()) {
            return false;
        }
        double radius = GLYPH_RADII.computeIfAbsent(
                (int) fireworks.character(), FireworksEntityRenderer::captureGlyphRadius);
        AABB bounds = EntityRenderBounds.radius(
                fireworks.getX(), fireworks.getY(), fireworks.getZ(), radius);
        return EntityRenderBounds.shouldRender(fireworks, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(FireworksEntity fireworks) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    static void invalidateGlyphBounds() {
        GLYPH_RADII.clear();
    }

    private static double captureGlyphRadius(int codePoint) {
        Font font = Minecraft.getInstance().font;
        String text = String.valueOf((char) codePoint);
        PoseStack poseStack = new PoseStack();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(-font.width(text) * 0.5F, -font.lineHeight * 0.5F, 0.0F);
        BoundsVertexConsumer vertices = new BoundsVertexConsumer();
        MultiBufferSource sink = renderType -> vertices;
        font.drawInBatch(text, 0.0F, 0.0F, 0xFFFFFFFF, false,
                poseStack.last().pose(), sink, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
        return vertices.radius();
    }

    private static final class BoundsVertexConsumer implements VertexConsumer {
        private double maximumRadiusSquared;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            maximumRadiusSquared = Math.max(maximumRadiusSquared,
                    (double) x * x + (double) y * y + (double) z * z);
            return this;
        }

        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float normalX, float normalY, float normalZ) { return this; }

        private double radius() {
            return Math.nextUp(Math.sqrt(maximumRadiusSquared));
        }
    }
}
