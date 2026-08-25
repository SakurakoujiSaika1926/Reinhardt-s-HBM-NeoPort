package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.item.LegacyHolotapeImageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Terminal-style direct port of GUIScreenHolotape. */
public final class HolotapeImageScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private final LegacyHolotapeImageItem.Tape tape;
    private int left;
    private int top;

    private HolotapeImageScreen(ItemStack stack) {
        super(Component.literal("Holotape"));
        this.tape = LegacyHolotapeImageItem.tape(stack);
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new HolotapeImageScreen(stack));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xCC003300);
        int y = top + 30;
        for (FormattedCharSequence line : font.split(Component.literal(tape.text()), 275)) {
            graphics.drawString(font, line, left + (WIDTH - font.width(line)) / 2, y, 0xFF009900, true);
            y += 10;
        }
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
