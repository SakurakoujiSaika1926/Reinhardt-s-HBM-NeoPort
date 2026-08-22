package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.IronFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class IronFurnaceScreen extends AbstractContainerScreen<IronFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_furnace_iron.png");

    public IronFurnaceScreen(IronFurnaceMenu menu, Inventory playerInventory, Component title) {
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

        if (isHovering(52, 35, 71, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.iron_furnace.progress",
                    this.menu.progress() * 100 / this.menu.processingTime()
            )), mouseX, mouseY);
        }
        if (isHovering(52, 44, 71, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.iron_furnace.burn",
                    this.menu.burnTime() / 20
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 36, 176, 18, progress, 5);
        }

        int burn = this.menu.burnScaled(70);
        if (burn > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 45, 176, 23, burn, 5);
        }

        if (this.menu.canSmelt()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 70, this.topPos + 16, 176, 0, 18, 18);
        }
    }
}
