package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Direct layout port of GUIScreenBobble. */
public final class BobbleheadScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private final BobbleheadType type;
    private int left;
    private int top;

    private BobbleheadScreen(BobbleheadType type) {
        super(Component.literal("Nuclear Tech Commemorative Bobblehead"));
        this.type = type;
    }

    public static void open(BobbleheadType type) {
        Minecraft.getInstance().setScreen(new BobbleheadScreen(type));
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
        drawCentered(graphics, "Nuclear Tech Commemorative Bobblehead", y, 0xFF00FF00);
        y += 10;
        drawCentered(graphics, type == BobbleheadType.MELLOW ? anagrammedMellow() : type.title(), y, 0xFF009900);
        y += 20;
        if (type.contribution() != null) {
            drawCentered(graphics, "Has contributed", y, 0xFF00FF00);
            y += 10;
            for (String line : type.contribution().split("\\$")) {
                drawCentered(graphics, line, y, 0xFF009900);
                y += 10;
            }
            y += 10;
        }
        if (type.inscription() != null) {
            drawCentered(graphics, "On the bottom is the following inscription:", y, 0xFF00FF00);
            y += 10;
            for (String line : type.inscription().split("\\$")) {
                drawCentered(graphics, line, y, 0xFF009900);
                y += 10;
            }
        }
    }

    private void drawCentered(GuiGraphics graphics, String text, int y, int color) {
        FormattedCharSequence sequence = Component.literal(text).getVisualOrderText();
        graphics.drawString(font, sequence, left + (WIDTH - font.width(sequence)) / 2, y, color, true);
    }

    private String anagrammedMellow() {
        String from = type.title();
        String to = "GEORGEWILLIAMPATON";
        double progress = Math.sin(System.currentTimeMillis() / 1500.0D) * 0.75D + 0.5D;
        char[] source = from.toCharArray();
        char[] target = to.toCharArray();
        boolean[] pairedTarget = new boolean[source.length];
        List<LetterTarget> letters = new ArrayList<>();

        for (int sourceIndex = 0; sourceIndex < source.length; sourceIndex++) {
            for (int targetIndex = 0; targetIndex < target.length; targetIndex++) {
                if (source[sourceIndex] == target[targetIndex] && !pairedTarget[targetIndex]) {
                    letters.add(new LetterTarget(lerp(sourceIndex, targetIndex, progress), source[sourceIndex]));
                    pairedTarget[targetIndex] = true;
                    break;
                }
            }
        }

        letters.sort(Comparator.comparingDouble(LetterTarget::position));
        StringBuilder result = new StringBuilder(letters.size());
        for (LetterTarget letter : letters) {
            result.append(letter.character());
        }
        return result.toString();
    }

    private static double lerp(double from, double to, double progress) {
        progress = Math.max(Math.min(progress, 1.0D), 0.0D);
        return from * (1.0D - progress) + to * progress;
    }

    private record LetterTarget(double position, char character) {
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
