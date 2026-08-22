package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.SatelliteLinkerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Original GUI layout for the satellite ID manager. */
public final class SatelliteLinkerScreen extends AbstractContainerScreen<SatelliteLinkerMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_linker.png");
    private static final ResourceLocation UTILITY = ReinhardtsHBM.id("textures/gui/gui_utility.png");

    public SatelliteLinkerScreen(SatelliteLinkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderInfoTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawInfoPanel(graphics, this.leftPos - 16, this.topPos + 36, 2);
        drawInfoPanel(graphics, this.leftPos - 16, this.topPos + 52, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    private static void drawInfoPanel(GuiGraphics graphics, int x, int y, int type) {
        graphics.blit(UTILITY, x, y, type == 3 ? 24 : 8, 0, 16, 16, 256, 256);
    }

    private void renderInfoTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHoveringInfo(mouseX, mouseY, 36)) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.satlinker.copy.1"),
                    Component.translatable("tooltip.reinhardtshbm.satlinker.copy.2")
            ), mouseX, mouseY);
        } else if (isHoveringInfo(mouseX, mouseY, 52)) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.satlinker.randomize.1"),
                    Component.translatable("tooltip.reinhardtshbm.satlinker.randomize.2")
            ), mouseX, mouseY);
        }
    }

    private boolean isHoveringInfo(int mouseX, int mouseY, int y) {
        return mouseX >= this.leftPos - 16 && mouseX < this.leftPos
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + 16;
    }
}
