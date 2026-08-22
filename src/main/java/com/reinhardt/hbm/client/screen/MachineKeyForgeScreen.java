package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.MachineKeyForgeMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachineKeyForgeScreen extends AbstractContainerScreen<MachineKeyForgeMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_keyforge.png");

    public MachineKeyForgeScreen(MachineKeyForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderInfoTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawInfoPanel(guiGraphics, this.leftPos - 16, this.topPos + 36, 2);
        drawInfoPanel(guiGraphics, this.leftPos - 16, this.topPos + 52, 3);
    }

    private void drawInfoPanel(GuiGraphics guiGraphics, int x, int y, int v) {
        guiGraphics.blit(TEXTURE, x, y, 176, v * 16, 16, 16);
    }

    private void renderInfoTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos - 16;
        if (mouseX >= x && mouseX < x + 16 && mouseY >= this.topPos + 36 && mouseY < this.topPos + 52) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.machine_keyforge.copy.1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.reinhardtshbm.machine_keyforge.copy.2").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
        if (mouseX >= x && mouseX < x + 16 && mouseY >= this.topPos + 52 && mouseY < this.topPos + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.machine_keyforge.random.1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.reinhardtshbm.machine_keyforge.random.2").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }
}
