package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.MassStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class MassStorageScreen extends AbstractContainerScreen<MassStorageMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_mass_storage.png");

    public MassStorageScreen(MassStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 221;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (isHovering(62, 72, 14, 14, mouseX, mouseY)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, hasShiftDown() ? 1 : 0);
                return true;
            }
            if (isHovering(80, 72, 14, 14, mouseX, mouseY)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int gauge = menu.capacity() <= 0 ? 0 : Math.min(88, menu.stockpile() * 88 / menu.capacity());
        if (gauge > 0) graphics.blit(TEXTURE, leftPos + 97, topPos + 105 - gauge, 176, 88 - gauge, 16, gauge);
        if (menu.output()) graphics.blit(TEXTURE, leftPos + 80, topPos + 72, 192, 0, 14, 14);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        String status = String.format(Locale.ROOT, "%,d / %,d", menu.stockpile(), menu.capacity());
        graphics.drawString(font, status, imageWidth - 8 - font.width(status), 16, 0x404040, false);
    }
}
