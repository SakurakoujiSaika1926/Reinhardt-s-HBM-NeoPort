package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.ExposureChamberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ExposureChamberScreen extends AbstractContainerScreen<ExposureChamberMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_exposure_chamber.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public ExposureChamberScreen(ExposureChamberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(152, 18, 16, 34, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    this.menu.maxPower()
            )), mouseX, mouseY);
        } else if (isHovering(26, 36, 9, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(this.menu.savedParticles() + " / " + this.menu.maxParticles())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        int progress = this.menu.progressScaled(42);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 36, this.topPos + 39, 192, 0, progress, 10, TEX_W, TEX_H);
        }

        int particles = this.menu.particlesScaled(16);
        if (particles > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 52 - particles, 192, 26 - particles, 9, particles, TEX_W, TEX_H);
        }

        int power = this.menu.powerScaled(34);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 52 - power, 176, 34 - power, 16, power, TEX_W, TEX_H);
        }

        if (this.menu.power() >= this.menu.consumption()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 156, this.topPos + 4, 176, 34, 9, 12, TEX_W, TEX_H);
        }
    }
}
