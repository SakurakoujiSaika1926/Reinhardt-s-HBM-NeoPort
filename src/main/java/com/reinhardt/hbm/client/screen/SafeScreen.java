package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.SafeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class SafeScreen extends AbstractContainerScreen<SafeMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_safe.png");

    public SafeScreen(SafeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 168;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
