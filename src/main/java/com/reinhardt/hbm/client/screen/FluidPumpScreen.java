package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.menu.FluidPumpMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FluidPumpScreen extends AbstractContainerScreen<FluidPumpMenu> {
    public FluidPumpScreen(FluidPumpMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 124;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addButton(0, 90, 50, 68, 20, Component.literal(this.menu.pressure() + " PU"));
        addButton(1, 90, 74, 68, 20, Component.literal(this.menu.priority().name()));
        addButton(2, 18, 98, 28, 20, Component.literal("-"));
        addButton(3, 48, 98, 28, 20, Component.literal("+"));
        addButton(4, 92, 98, 28, 20, Component.literal("--"));
        addButton(5, 122, 98, 28, 20, Component.literal("++"));
    }

    private void addButton(int id, int x, int y, int width, int height, Component label) {
        this.addRenderableWidget(Button.builder(label, button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(this.leftPos + x, this.topPos + y, width, height).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF202020);
        guiGraphics.fill(this.leftPos + 2, this.topPos + 2, this.leftPos + this.imageWidth - 2, this.topPos + this.imageHeight - 2, 0xFF4A4A4A);
        guiGraphics.fill(this.leftPos + 8, this.topPos + 30, this.leftPos + 78, this.topPos + 88, 0xFF151515);
        int height = this.menu.bufferSize() <= 0 ? 0 : Math.min(54, this.menu.amount() * 54 / Math.max(1, this.menu.bufferSize()));
        if (height > 0) {
            guiGraphics.fill(this.leftPos + 10, this.topPos + 86 - height, this.leftPos + 76, this.topPos + 86, 0xFF2EA0D8);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFF00, false);
        guiGraphics.drawString(this.font, Component.translatable(this.menu.fluid().translationKey()), 10, 18, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.menu.amount() + "/" + this.menu.bufferSize() + " mB", 10, 90, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.fluid.pressure"), 90, 38, 0xD0D0D0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.fluid.priority"), 90, 62, 0xD0D0D0, false);
        guiGraphics.drawString(this.font, this.menu.powered() ? "RS" : "", 150, 18, 0xFF4040, false);
    }
}
