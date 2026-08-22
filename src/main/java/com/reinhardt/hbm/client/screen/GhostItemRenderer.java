package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class GhostItemRenderer {
    private GhostItemRenderer() {
    }

    static void render(GuiGraphics guiGraphics, ItemStack stack, int x, int y, float alpha) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x + 8.0F, y + 8.0F, 150.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            pose.scale(16.0F, -16.0F, 16.0F);
            boolean flatLighting = !model.usesBlockLight();
            if (flatLighting) {
                Lighting.setupForFlatItems();
            }

            minecraft.getItemRenderer().render(
                    stack,
                    ItemDisplayContext.GUI,
                    false,
                    pose,
                    new AlphaBufferSource(guiGraphics.bufferSource(), alpha),
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    model
            );
            guiGraphics.flush();

            if (flatLighting) {
                Lighting.setupFor3DItems();
            }
        } finally {
            RenderSystem.disableBlend();
            pose.popPose();
        }
    }

    private record AlphaBufferSource(MultiBufferSource source, float alpha) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new AlphaVertexConsumer(this.source.getBuffer(renderType), this.alpha);
        }
    }

    private record AlphaVertexConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(red, green, blue, Math.round(alpha * this.alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            this.delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
