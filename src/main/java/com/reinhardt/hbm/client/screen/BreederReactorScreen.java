package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.BreederReactorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class BreederReactorScreen extends AbstractContainerScreen<BreederReactorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_breeder.png");

    public BreederReactorScreen(BreederReactorMenu menu, Inventory playerInventory, Component title) {
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

        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.breeder_reactor.flux.0"),
                    Component.translatable("tooltip.reinhardtshbm.breeder_reactor.flux.1"),
                    Component.translatable("tooltip.reinhardtshbm.breeder_reactor.flux.2")
            ), mouseX, mouseY);
        }
        if (isHovering(53, 32, 70, 20, mouseX, mouseY) && this.menu.requiredFlux() > 0) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.breeder_reactor.progress",
                    this.menu.flux(),
                    this.menu.requiredFlux()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 32, 176, 0, progress, 20);
        }
        guiGraphics.blit(TEXTURE, this.leftPos - 16, this.topPos + 16, 176, 20, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        String flux = Integer.toString(this.menu.flux());
        guiGraphics.drawString(this.font, flux, 88 - this.font.width(flux) / 2, 21, 0x08FF00, false);
    }
}
