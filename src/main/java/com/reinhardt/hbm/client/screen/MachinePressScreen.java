package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.menu.MachinePressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachinePressScreen extends AbstractContainerScreen<MachinePressMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_press.png");

    public MachinePressScreen(MachinePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(25, 16, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.press.speed",
                    this.menu.speedPercent()
            )), mouseX, mouseY);
        }
        if (isHovering(25, 34, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.press.operations",
                    this.menu.operationsLeft()
            )), mouseX, mouseY);
        }
        if (isHovering(79, 35, 18, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    PressBlockEntity.MAX_PROGRESS
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.burnTime() > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 27, this.topPos + 36, 0, 202, 14, 14);
        }

        int progress = this.menu.progressScaled(16);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 35, 14, 202, 18, progress);
        }

        int speed = this.menu.speedScaled(10);
        if (speed > 0) {
            int y = this.topPos + 31 - speed;
            guiGraphics.fill(this.leftPos + 34, y, this.leftPos + 39, this.topPos + 31, 0xFF7F0000);
        }
    }
}
