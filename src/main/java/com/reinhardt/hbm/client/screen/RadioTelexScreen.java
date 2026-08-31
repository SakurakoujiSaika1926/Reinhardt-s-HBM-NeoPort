package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RadioTelexBlockEntity;
import com.reinhardt.hbm.menu.RadioTelexMenu;
import com.reinhardt.hbm.network.RadioTelexControlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Random;

/** Pixel-layout port of GUIScreenRadioTelex and its five-line terminal editor. */
public final class RadioTelexScreen extends AbstractContainerScreen<RadioTelexMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_telex.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private final String[] transmit = new String[RadioTelexBlockEntity.LINE_COUNT];
    private EditBox transmitChannel;
    private EditBox receiveChannel;
    private boolean textFocus;
    private int cursorLine;

    public RadioTelexScreen(RadioTelexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 244;
        Arrays.fill(transmit, "");
    }

    @Override
    protected void init() {
        super.init();
        RadioTelexBlockEntity telex = telex();
        if (telex != null) {
            String[] serverLines = telex.txBuffer();
            System.arraycopy(serverLines, 0, transmit, 0, transmit.length);
            for (int index = transmit.length - 1; index > 0; index--) {
                if (!transmit[index].isEmpty()) {
                    cursorLine = index;
                    break;
                }
            }
        }

        transmitChannel = channelBox(leftPos + 29, topPos + 110,
                telex == null ? "" : telex.txChannel());
        receiveChannel = channelBox(leftPos + 29, topPos + 224,
                telex == null ? "" : telex.rxChannel());
        addRenderableWidget(transmitChannel);
        addRenderableWidget(receiveChannel);
    }

    private EditBox channelBox(int x, int y, String value) {
        EditBox box = new EditBox(font, x, y, 90, 14, Component.empty());
        box.setTextColor(0x00FF00);
        box.setTextColorUneditable(0x00FF00);
        box.setBordered(false);
        box.setMaxLength(RadioTelexBlockEntity.CHANNEL_WIDTH);
        box.setValue(value);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int line = 0; line < transmit.length; line++) {
            drawTerminalLine(graphics, transmit[line], 11, 11 + 14 * line, true);
            if (textFocus && cursorLine == line && System.currentTimeMillis() % 1000L < 500L) {
                graphics.drawString(font, "|", 11 + 7 * transmit[line].length(), 11 + 14 * line,
                        0x00FF00, false);
            }
        }

        RadioTelexBlockEntity telex = telex();
        if (telex != null) {
            String[] receive = telex.rxBuffer();
            for (int line = 0; line < receive.length; line++) {
                drawTerminalLine(graphics, receive[line], 11, 145 + 14 * line, false);
            }
            drawWaveform(graphics, telex.sendingChar());
        }
    }

    private void drawTerminalLine(GuiGraphics graphics, String text, int startX, int y, boolean showControls) {
        String format = "\u00a7r";
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\u00a7' && index + 1 < text.length()) {
                format = "\u00a7" + text.charAt(++index);
                continue;
            }
            String glyph = Character.toString(character);
            int color = 0x00FF00;
            if (showControls) {
                if (character == RadioTelexBlockEntity.BELL) glyph = "B";
                else if (character == RadioTelexBlockEntity.PRINT) glyph = "P";
                else if (character == RadioTelexBlockEntity.CLEAR) glyph = "<";
                else if (character == RadioTelexBlockEntity.PAUSE) glyph = "W";
                if (glyph.length() == 1 && glyph.charAt(0) != character) color = 0xFF5555;
            }
            String rendered = format + glyph;
            int x = startX + 7 * index + (7 - font.width(glyph)) / 2;
            graphics.drawString(font, rendered, x, y, color, false);
        }
    }

    private void drawWaveform(GuiGraphics graphics, char sendingChar) {
        Random random = new Random(sendingChar);
        int previous = 0;
        for (int index = 0; index < 48; index++) {
            int next = sendingChar != ' ' && index > 4 && index < 43
                    ? Mth.clamp((int) Math.round(random.nextGaussian() * 7.0D), -7, 7)
                    : 0;
            int x = 199 + index;
            int minY = 94 + Math.min(previous, next);
            int maxY = 94 + Math.max(previous, next);
            graphics.fill(x, minY, x + 2, maxY + 1, 0xFF00FF00);
            previous = next;
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tooltip(graphics, mouseX, mouseY, 7, 85, "gui.reinhardtshbm.telex.bell",
                "gui.reinhardtshbm.telex.bell.detail")) return;
        if (tooltip(graphics, mouseX, mouseY, 27, 85, "gui.reinhardtshbm.telex.print_control",
                "gui.reinhardtshbm.telex.print_control.detail")) return;
        if (tooltip(graphics, mouseX, mouseY, 47, 85, "gui.reinhardtshbm.telex.clear_control",
                "gui.reinhardtshbm.telex.clear_control.detail")) return;
        if (tooltip(graphics, mouseX, mouseY, 67, 85, "gui.reinhardtshbm.telex.format",
                "gui.reinhardtshbm.telex.format.detail")) return;
        if (tooltip(graphics, mouseX, mouseY, 87, 85, "gui.reinhardtshbm.telex.pause",
                "gui.reinhardtshbm.telex.pause.detail")) return;
        if (tooltip(graphics, mouseX, mouseY, 127, 105, "gui.reinhardtshbm.telex.save_id")) return;
        if (tooltip(graphics, mouseX, mouseY, 147, 105, "gui.reinhardtshbm.telex.send")) return;
        if (tooltip(graphics, mouseX, mouseY, 167, 105, "gui.reinhardtshbm.telex.delete_transmit")) return;
        if (tooltip(graphics, mouseX, mouseY, 127, 219, "gui.reinhardtshbm.telex.save_id")) return;
        if (tooltip(graphics, mouseX, mouseY, 147, 219, "gui.reinhardtshbm.telex.print_receive")) return;
        if (tooltip(graphics, mouseX, mouseY, 167, 219, "gui.reinhardtshbm.telex.clear_receive")) return;
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean tooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, String... keys) {
        if (!isHovering(x, y, 18, 18, mouseX, mouseY)) return false;
        graphics.renderComponentTooltip(font, Arrays.stream(keys).<Component>map(Component::translatable).toList(),
                mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) return handled;

        if (inside(mouseX, mouseY, 7, 7, 242, 74)) {
            textFocus = true;
            transmitChannel.setFocused(false);
            receiveChannel.setFocused(false);
            return true;
        }
        textFocus = false;

        if (button(mouseX, mouseY, 7, 85)) return insert(RadioTelexBlockEntity.BELL);
        if (button(mouseX, mouseY, 27, 85)) return insert(RadioTelexBlockEntity.PRINT);
        if (button(mouseX, mouseY, 47, 85)) return insert(RadioTelexBlockEntity.CLEAR);
        if (button(mouseX, mouseY, 67, 85)) return insert('\u00a7');
        if (button(mouseX, mouseY, 87, 85)) return insert(RadioTelexBlockEntity.PAUSE);
        if (button(mouseX, mouseY, 127, 105) || button(mouseX, mouseY, 127, 219)) {
            return send(RadioTelexControlPayload.SAVE_CHANNELS,
                    transmitChannel.getValue(), receiveChannel.getValue());
        }
        if (button(mouseX, mouseY, 147, 105)) {
            return send(RadioTelexControlPayload.SEND, serializedTransmit(), "");
        }
        if (button(mouseX, mouseY, 167, 105)) {
            Arrays.fill(transmit, "");
            cursorLine = 0;
            return send(RadioTelexControlPayload.DELETE_TRANSMIT, "", "");
        }
        if (button(mouseX, mouseY, 147, 219)) {
            return send(RadioTelexControlPayload.PRINT_RECEIVE, "", "");
        }
        if (button(mouseX, mouseY, 167, 219)) {
            return send(RadioTelexControlPayload.CLEAR_RECEIVE, "", "");
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (transmitChannel.isFocused() || receiveChannel.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (textFocus) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                textFocus = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                cursorLine = Math.max(0, cursorLine - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                cursorLine = Math.min(transmit.length - 1, cursorLine + 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !transmit[cursorLine].isEmpty()) {
                transmit[cursorLine] = transmit[cursorLine].substring(0, transmit[cursorLine].length() - 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (transmitChannel.isFocused() || receiveChannel.isFocused()) {
            return super.charTyped(character, modifiers);
        }
        if (textFocus && StringUtil.isAllowedChatCharacter(character)) {
            submit(character);
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private boolean insert(char character) {
        textFocus = true;
        transmitChannel.setFocused(false);
        receiveChannel.setFocused(false);
        submit(character);
        click();
        return true;
    }

    private void submit(char character) {
        if (transmit[cursorLine].length() < RadioTelexBlockEntity.LINE_WIDTH) {
            transmit[cursorLine] += character;
        }
    }

    private boolean send(int action, String transmitData, String receiveData) {
        PacketDistributor.sendToServer(new RadioTelexControlPayload(menu.blockPos(), action,
                transmitData, receiveData));
        click();
        return true;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean button(double mouseX, double mouseY, int x, int y) {
        return inside(mouseX, mouseY, x, y, 18, 18);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private String serializedTransmit() {
        return String.join("\n", transmit);
    }

    private RadioTelexBlockEntity telex() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioTelexBlockEntity value
                ? value : null;
    }

    @Override
    public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new RadioTelexControlPayload(menu.blockPos(),
                    RadioTelexControlPayload.UPDATE_TRANSMIT, serializedTransmit(), ""));
        }
        super.removed();
    }
}
