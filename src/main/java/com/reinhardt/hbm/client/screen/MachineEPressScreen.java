package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.menu.MachineEPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachineEPressScreen extends AbstractContainerScreen<MachineEPressMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_epress.png");

    public MachineEPressScreen(MachineEPressMenu menu, Inventory playerInventory, Component title) {
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

        if (isHovering(17, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    PressBlockEntity.ELECTRIC_ENERGY_CAPACITY
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

        int energy = this.menu.energyScaled(52);
        if (energy > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 17, this.topPos + 69 - energy, 176, 52 - energy, 16, energy);
        }

        int progress = this.menu.progressScaled(16);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 35, 192, 0, 18, progress);
        }
    }
}
