package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.menu.PortableCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PortableCrateScreen extends AbstractContainerScreen<PortableCrateMenu> {
    public PortableCrateScreen(PortableCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = menu.kind().guiWidth();
        this.imageHeight = menu.kind().guiHeight();
        this.inventoryLabelX = menu.kind().inventoryLabelX();
        this.inventoryLabelY = this.imageHeight - 94;
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
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.menu.kind().texture(false), this.leftPos, this.topPos,
                0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        StorageCrateBlockEntity.Kind kind = this.menu.kind();
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, kind.titleColor(false), false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, kind.inventoryLabelColor(false), false);
    }
}
