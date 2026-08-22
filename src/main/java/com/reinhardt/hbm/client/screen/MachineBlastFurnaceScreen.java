package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.MachineBlastFurnaceMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachineBlastFurnaceScreen extends AbstractContainerScreen<MachineBlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_blast_furnace_1710.png");

    public MachineBlastFurnaceScreen(MachineBlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
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

        if (isHovering(25, 17, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.flueFluid(),
                    this.menu.flueAmount(),
                    this.menu.flueCapacity()
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(25, 71, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.airblastFluid(),
                    this.menu.airblastAmount(),
                    this.menu.airblastCapacity()
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(62, 80, 56, 26, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.blast_furnace.fuel",
                    this.menu.fuel(),
                    com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity.MAX_FUEL
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(62, 18, 56, 88, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.progress_percent", Math.round(this.menu.progressFraction() * 100.0F)),
                    Component.translatable("tooltip.reinhardtshbm.blast_furnace.speed", this.menu.speedPercent())
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int fuelPixels = this.menu.fuelScaled(26);
        if (fuelPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 106 - fuelPixels, 176, 128 - fuelPixels, 56, fuelPixels);
        }

        int progressPixels = this.menu.progressScaled(Math.max(0, 88 - fuelPixels));
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 106 - progressPixels - fuelPixels, 176, 102 - progressPixels - fuelPixels, 56, progressPixels);
        }

        if (this.menu.isProcessing()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 81, this.topPos + 64, 176, 0, 14, 14);
        }

        drawFluid(guiGraphics, 25, 35, this.menu.flueAmount() * 18 / Math.max(1, this.menu.flueCapacity()), this.menu.flueFluid());
        drawFluid(guiGraphics, 25, 89, this.menu.airblastAmount() * 18 / Math.max(1, this.menu.airblastCapacity()), this.menu.airblastFluid());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int height, com.reinhardt.hbm.fluid.HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + 18, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + 18, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }
}
