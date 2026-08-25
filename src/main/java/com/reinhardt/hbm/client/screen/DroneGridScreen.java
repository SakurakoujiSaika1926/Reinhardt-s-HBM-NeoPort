package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DroneGridMenu;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Shared legacy GUI for the logistics drone dock and provider crate. */
public final class DroneGridScreen extends AbstractContainerScreen<DroneGridMenu> {
    private static final ResourceLocation DOCK_TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_drone_dock.png");
    private static final ResourceLocation PROVIDER_TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_drone_provider.png");

    public DroneGridScreen(DroneGridMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.getType() == HbmMenus.DRONE_DOCK.get() ? 185 : 186;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(menu.getType() == HbmMenus.DRONE_DOCK.get() ? DOCK_TEXTURE : PROVIDER_TEXTURE,
                leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }
}
