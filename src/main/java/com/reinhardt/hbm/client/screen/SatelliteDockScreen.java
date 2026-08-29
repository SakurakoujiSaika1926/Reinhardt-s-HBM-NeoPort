package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.SatelliteDockMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Direct GUI layout port of GUISatDock. */
public final class SatelliteDockScreen extends AbstractContainerScreen<SatelliteDockMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_dock.png");

    public SatelliteDockScreen(SatelliteDockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
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
        if (mouseX >= leftPos - 16 && mouseX < leftPos && mouseY >= topPos + 36 && mouseY < topPos + 52) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.sat_dock.1"),
                    Component.translatable("tooltip.reinhardtshbm.sat_dock.2"),
                    Component.translatable("tooltip.reinhardtshbm.sat_dock.3")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEXTURE, leftPos - 16, topPos + 36, 176, 0, 16, 16);
    }
}
