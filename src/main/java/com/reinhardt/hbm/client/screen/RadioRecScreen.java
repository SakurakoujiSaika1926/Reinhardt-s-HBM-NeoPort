package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RadioRecBlockEntity;
import com.reinhardt.hbm.menu.RadioRecMenu;
import com.reinhardt.hbm.network.RadioRecControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Exact compact GUIRadioRec layout, with no artificial inventory slots. */
public final class RadioRecScreen extends AbstractContainerScreen<RadioRecMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_radio.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private EditBox frequency;

    public RadioRecScreen(RadioRecMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 42;
    }

    @Override
    protected void init() {
        super.init();
        RadioRecBlockEntity radio = minecraft == null || minecraft.level == null
                ? null
                : minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioRecBlockEntity value ? value : null;
        frequency = new EditBox(font, leftPos + 29, topPos + 21, 82, 14, Component.empty());
        frequency.setTextColor(0x00FF00);
        frequency.setTextColorUneditable(0x00FF00);
        frequency.setBordered(false);
        frequency.setMaxLength(10);
        frequency.setValue(radio == null ? "" : radio.channel());
        addRenderableWidget(frequency);
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (isOn()) {
            graphics.blit(TEXTURE, leftPos + 173, topPos + 17, 0, 42, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(137, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.reinhardtshbm.radio.save")), mouseX, mouseY);
            return;
        }
        if (isHovering(173, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.reinhardtshbm.radio.toggle")), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(137, 17, 18, 18, mouseX, mouseY)) {
            PacketDistributor.sendToServer(new RadioRecControlPayload(menu.blockPos(), 0, frequency.getValue()));
            click();
            return true;
        }
        if (button == 0 && isHovering(173, 17, 18, 18, mouseX, mouseY)) {
            PacketDistributor.sendToServer(new RadioRecControlPayload(menu.blockPos(), 1, ""));
            click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOn() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioRecBlockEntity radio
                && radio.isOn();
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
