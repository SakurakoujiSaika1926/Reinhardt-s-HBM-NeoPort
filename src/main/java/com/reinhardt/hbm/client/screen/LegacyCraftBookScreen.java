package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyCraftBookItem;
import com.reinhardt.hbm.menu.LegacyCraftBookMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original 176x166 Book of Boxcars and Lemegeton layouts. */
public final class LegacyCraftBookScreen extends AbstractContainerScreen<LegacyCraftBookMenu> {
    private static final ResourceLocation BOOK = ReinhardtsHBM.id("textures/gui/processing/gui_book.png");
    private static final ResourceLocation LEMEGETON = ReinhardtsHBM.id("textures/gui/processing/gui_lemegeton.png");

    public LegacyCraftBookScreen(LegacyCraftBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = menu.kind() == LegacyCraftBookItem.Kind.BOXCARS ? BOOK : LEMEGETON;
        graphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (menu.getSlot(0).hasItem()) {
            if (menu.kind() == LegacyCraftBookItem.Kind.BOXCARS) {
                graphics.blit(texture, this.leftPos + 29, this.topPos + 16, 176, 0, 54, 54);
            } else {
                graphics.blit(texture, this.leftPos + 7, this.topPos + 22, 0, 166, 162, 42);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String heading = menu.kind() == LegacyCraftBookItem.Kind.BOXCARS
                ? "Extended 4-Slot Crafting"
                : "Material Upgrade Conversion";
        graphics.drawString(this.font, heading, 28, 6, 4210752, false);
        graphics.drawString(this.font, "Standard Inventory", 8, this.inventoryLabelY, 4210752, false);
    }
}
