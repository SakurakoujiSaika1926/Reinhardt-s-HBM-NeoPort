package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.block.SnowglobeType;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public final class SnowglobeScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private final SnowglobeType type;
    private int left;
    private int top;

    private SnowglobeScreen(SnowglobeType type) {
        super(Component.literal("Nuclear Tech Commemorative Snowglobe"));
        this.type = type;
    }

    public static void open(SnowglobeType type) {
        Minecraft.getInstance().setScreen(new SnowglobeScreen(type));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(HbmSoundEvents.BOBBLE.get(), 1.0F));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xCC003300);
        int y = top + 10;
        drawCentered(graphics, "Nuclear Tech Commemorative Snowglobe", y, 0xFF00FF00);
        y += 10;
        drawCentered(graphics, type.label(), y, 0xFF009900);
        y += 20;
        if (type.inscription() != null) {
            drawCentered(graphics, "On the bottom is the following inscription:", y, 0xFF00FF00);
            y += 10;
            for (FormattedCharSequence line : font.split(Component.literal(type.inscription()), 280)) {
                graphics.drawString(font, line, left + (WIDTH - font.width(line)) / 2, y, 0xFF009900, true);
                y += 10;
            }
        }
    }

    private void drawCentered(GuiGraphics graphics, String text, int y, int color) {
        FormattedCharSequence sequence = Component.literal(text).getVisualOrderText();
        graphics.drawString(font, sequence, left + (WIDTH - font.width(sequence)) / 2, y, color, true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
