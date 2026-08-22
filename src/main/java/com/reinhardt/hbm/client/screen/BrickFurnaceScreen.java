package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.BrickFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class BrickFurnaceScreen extends AbstractContainerScreen<BrickFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_furnace_brick.png");

    public BrickFurnaceScreen(BrickFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(62, 54, 14, 13, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.furnace_brick.burn", menu.burnTime() / 20)), mouseX, mouseY);
        }
        if (isHovering(85, 34, 25, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.furnace_brick.progress", menu.progress() * 100 / menu.processTime())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.working()) {
            int burn = menu.burnScaled(13);
            if (burn > 0) graphics.blit(TEXTURE, leftPos + 62, topPos + 66 - burn, 176, 12 - burn, 14, burn);
            int progress = menu.progressScaled(24);
            if (progress > 0) graphics.blit(TEXTURE, leftPos + 85, topPos + 34, 176, 14, progress, 16);
        }
    }
}
