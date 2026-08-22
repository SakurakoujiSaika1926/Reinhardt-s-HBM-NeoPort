package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.ResearchReactorMenu;
import com.reinhardt.hbm.network.ResearchReactorControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ResearchReactorScreen extends AbstractContainerScreen<ResearchReactorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_research_reactor.png");
    private static final int FIELD_X = 8;
    private static final int FIELD_Y = 99;
    private static final int FIELD_WIDTH = 33;
    private static final int FIELD_HEIGHT = 16;
    private static final int CONFIRM_X = 44;
    private static final int CONFIRM_Y = 97;
    private static final int CONFIRM_WIDTH = 11;
    private static final int CONFIRM_HEIGHT = 20;

    private EditBox controlField;
    private int confirmFlashTicks;

    public ResearchReactorScreen(ResearchReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 121 - this.font.width(this.title) / 2;
        this.controlField = new EditBox(this.font, this.leftPos + FIELD_X, this.topPos + FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        this.controlField.setBordered(false);
        this.controlField.setMaxLength(3);
        this.controlField.setTextColor(0x08FF00);
        this.controlField.setValue(Integer.toString(this.menu.controlPercent()));
        this.addRenderableWidget(this.controlField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(-14, 23, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.research_reactor.cooling.0"),
                    Component.translatable("tooltip.reinhardtshbm.research_reactor.cooling.1"),
                    Component.translatable("tooltip.reinhardtshbm.research_reactor.cooling.2"),
                    Component.translatable("tooltip.reinhardtshbm.research_reactor.cooling.3")
            ), mouseX, mouseY);
        }
        if (isHovering(-14, 61, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.research_reactor.fuel.0"),
                    Component.translatable("tooltip.reinhardtshbm.research_reactor.fuel.1")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.controlPercent() <= 50) {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 81 + 36 * x, this.topPos + 26 + 36 * y, 176, 0, 8, 8);
                }
            }
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 99 + 36 * x, this.topPos + 44 + 36 * y, 176, 0, 8, 8);
                }
            }
        }

        if (this.confirmFlashTicks > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + CONFIRM_X, this.topPos + CONFIRM_Y, 176, 8, CONFIRM_WIDTH, CONFIRM_HEIGHT);
            this.confirmFlashTicks--;
        }

        drawInfoPanel(guiGraphics, -14, 23, 3);
        drawInfoPanel(guiGraphics, -14, 61, 2);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 15066597, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("label.reinhardtshbm.research_reactor.flux"), 6, 13, 15066597, false);
        guiGraphics.drawString(this.font, Component.translatable("label.reinhardtshbm.research_reactor.heat"), 6, 51, 15066597, false);
        guiGraphics.drawString(this.font, Component.translatable("label.reinhardtshbm.research_reactor.control"), 6, 89, 15066597, false);
        drawDisplay(guiGraphics, this.menu.flux(), 14, 25, 4);
        drawDisplay(guiGraphics, this.menu.temperature(), 12, 63, 3);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && this.leftPos + CONFIRM_X <= mouseX
                && this.leftPos + CONFIRM_X + CONFIRM_WIDTH > mouseX
                && this.topPos + CONFIRM_Y < mouseY
                && this.topPos + CONFIRM_Y + CONFIRM_HEIGHT >= mouseY) {
            int level = parseControlPercent();
            this.controlField.setValue(Integer.toString(level));
            PacketDistributor.sendToServer(new ResearchReactorControlPayload(this.menu.blockPos(), level));
            this.confirmFlashTicks = 15;
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int parseControlPercent() {
        try {
            return Mth.clamp(Integer.parseInt(this.controlField.getValue()), 0, 100);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void drawDisplay(GuiGraphics graphics, int value, int x, int y, int digits) {
        String text = Integer.toString(Math.max(0, value));
        if (text.length() > digits) {
            text = text.substring(text.length() - digits);
        }
        graphics.drawString(this.font, text, x, y, 0x08FF00, false);
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int height) {
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 187, 0, 16, 8 + height * 8);
    }

    private void playClick() {
        if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
        }
    }
}
