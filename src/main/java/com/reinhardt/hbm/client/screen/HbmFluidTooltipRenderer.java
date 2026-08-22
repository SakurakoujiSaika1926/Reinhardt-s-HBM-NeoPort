package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Modern rendering of the 1.7.10 GUIElements.drawHoveringTextFluid tooltip.
 */
public final class HbmFluidTooltipRenderer {
    private static final int HEADER_OFFSET = 6;
    private static final int LEGACY_LINE_DISTANCE = 10;
    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int MIN_SCREEN_MARGIN = 4;

    private HbmFluidTooltipRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, List<Component> lines,
                              HbmFluidDefinition fluid, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }

        int lineDistance = Math.max(LEGACY_LINE_DISTANCE, font.lineHeight + 2);
        int width = lines.stream().mapToInt(font::width).max().orElse(0);
        int height = 6 + HEADER_OFFSET;
        if (lines.size() > 1) {
            height += 2 + (lines.size() - 1) * lineDistance;
        }

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width + 4 > graphics.guiWidth()) {
            x -= 28 + width;
        }
        if (y + height + 6 > graphics.guiHeight()) {
            y = graphics.guiHeight() - height - 6;
        }
        x = Math.max(x, MIN_SCREEN_MARGIN);
        y = Math.max(y, MIN_SCREEN_MARGIN);

        int borderTop = fluidBorderColor(fluid);
        int borderBottom = darkerFluidBorderColor(borderTop);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        graphics.fill(x - 3, y - 4, x + width + 3, y - 3, BACKGROUND_COLOR);
        graphics.fill(x - 3, y + height + 3, x + width + 3, y + height + 4, BACKGROUND_COLOR);
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, BACKGROUND_COLOR);
        graphics.fill(x - 4, y - 3, x - 3, y + height + 3, BACKGROUND_COLOR);
        graphics.fill(x + width + 3, y - 3, x + width + 4, y + height + 3, BACKGROUND_COLOR);

        graphics.fillGradient(x - 3, y - 2, x - 2, y + height + 2, borderTop, borderBottom);
        graphics.fillGradient(x + width + 2, y - 2, x + width + 3, y + height + 2, borderTop, borderBottom);
        graphics.fill(x - 3, y - 3, x + width + 3, y - 2, borderTop);
        graphics.fill(x - 3, y + height + 2, x + width + 3, y + height + 3, borderBottom);

        int lineY = y;
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x, lineY, 0xFFFFFFFF, true);
            if (index == 0) {
                lineY += HEADER_OFFSET;
            }
            lineY += lineDistance;
        }
        graphics.pose().popPose();
    }

    private static int fluidBorderColor(HbmFluidDefinition fluid) {
        int rgb = fluid == null ? 0xFFFFFF : fluid.color() & 0xFFFFFF;
        return 0xFF000000 | rgb;
    }

    private static int darkerFluidBorderColor(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int adjustment = (red + green + blue) / 3 > 0x80 ? -0x40 : 0x40;
        red = clampColor(red + adjustment);
        green = clampColor(green + adjustment);
        blue = clampColor(blue + adjustment);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(0xFF, value));
    }
}
