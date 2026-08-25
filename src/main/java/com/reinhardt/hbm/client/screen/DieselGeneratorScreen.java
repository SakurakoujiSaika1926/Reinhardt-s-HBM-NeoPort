package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DieselGeneratorMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DieselGeneratorScreen extends AbstractContainerScreen<DieselGeneratorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/generators/gui_diesel.png");

    public DieselGeneratorScreen(DieselGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
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

        if (isHovering(80, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fuelFluid(), this.menu.fuelAmount(), this.menu.fuelCapacity()), mouseX, mouseY);
        }
        if (isHovering(152, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    this.menu.powerCap()
            )), mouseX, mouseY);
        }
        if (isHovering(-16, 36, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.diesel.fuel_consumption"),
                    Component.literal("1 mB/t"),
                    Component.literal("20 mB/s"),
                    Component.translatable("tooltip.reinhardtshbm.diesel.fuel_consumption_constant")
            ), mouseX, mouseY);
        }
        if (!this.menu.running() && !this.menu.fuelFluid().isNone() && this.menu.hePerTick() <= 0 && isHovering(-16, 68, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.diesel.unsupported_fuel")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 69 - power, 176, 52 - power, 16, power);
        }
        int fuel = this.menu.fuelScaled(52);
        if (fuel > 0 && !this.menu.fuelFluid().isNone()) {
            int color = 0xFF000000 | this.menu.fuelFluid().color();
            guiGraphics.fill(this.leftPos + 80, this.topPos + 69 - fuel, this.leftPos + 96, this.topPos + 69, color);
            guiGraphics.fill(this.leftPos + 80, this.topPos + 69 - fuel, this.leftPos + 96, this.topPos + 70 - fuel, 0x66FFFFFF);
        }
        if (this.menu.running()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 115, this.topPos + 34, 208, 0, 18, 18);
        }
        guiGraphics.blit(TEXTURE, this.leftPos - 16, this.topPos + 36, 176, 52, 16, 16);
        if (!this.menu.running() && !this.menu.fuelFluid().isNone() && this.menu.hePerTick() <= 0) {
            guiGraphics.blit(TEXTURE, this.leftPos - 16, this.topPos + 68, 192, 52, 16, 16);
        }
    }
}
