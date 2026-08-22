package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.menu.StorageCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StorageCrateScreen extends AbstractContainerScreen<StorageCrateMenu> {
    public StorageCrateScreen(StorageCrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = menu.kind().guiWidth();
        this.imageHeight = menu.kind().guiHeight();
        this.inventoryLabelX = menu.kind().inventoryLabelX();
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
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(this.menu.kind().texture(this.menu.hot()), this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        StorageCrateBlockEntity.Kind kind = this.menu.kind();
        boolean hot = this.menu.hot();
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, 6, kind.titleColor(hot), false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, kind.inventoryLabelColor(hot), false);
        if (kind == StorageCrateBlockEntity.Kind.TUNGSTEN && hot) {
            String sparks = this.menu.joules() + "SPK";
            guiGraphics.drawString(this.font, sparks, this.imageWidth - 8 - this.font.width(sparks), this.inventoryLabelY, 0xFFCA53, false);
        }
    }
}
