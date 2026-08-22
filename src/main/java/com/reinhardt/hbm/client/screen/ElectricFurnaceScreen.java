package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ElectricFurnaceBlockEntity;
import com.reinhardt.hbm.menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/guielectricfurnace.png");

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
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

        if (isHovering(20, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    ElectricFurnaceBlockEntity.ENERGY_CAPACITY
            )), mouseX, mouseY);
        }

        if (isHovering(79, 34, 25, 17, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    this.menu.workTime()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energyPixels = this.menu.energyScaled(52);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 20,
                    this.topPos + 69 - energyPixels,
                    200,
                    52 - energyPixels,
                    16,
                    energyPixels
            );
        }

        if (this.menu.isWorking()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 35, 176, 0, 16, 16);
        }

        int progressPixels = this.menu.progressScaled(24);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 34, 176, 17, progressPixels + 1, 17);
        }
    }
}
