package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

final class DfcScreenUtil {
    private DfcScreenUtil() {
    }

    static void drawFluid(Minecraft minecraft, GuiGraphics graphics, int leftPos, int topPos, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (minecraft == null || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(leftPos + x, topPos + bottomY - height, leftPos + x + width, topPos + bottomY, color);
            graphics.fill(leftPos + x, topPos + bottomY - height, leftPos + x + width, topPos + bottomY - height + 1, 0x66FFFFFF);
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            for (int tileY = 0; tileY < height; tileY += 16) {
                int tileHeight = Math.min(16, height - tileY);
                graphics.blit(texture, leftPos + x + tileX, topPos + top + tileY, 0, 16 - tileHeight, tileWidth, tileHeight, 16, 16);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static String shortNumber(long number) {
        if (number < 1_000L) {
            return Long.toString(number);
        }
        if (number < 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fk", number / 1_000.0D);
        }
        if (number < 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", number / 1_000_000.0D);
        }
        if (number < 1_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fG", number / 1_000_000_000.0D);
        }
        return String.format(Locale.ROOT, "%.1fT", number / 1_000_000_000_000.0D);
    }
}
