package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.RebarPlacerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original 176x182 rebar placer layout. */
public final class RebarPlacerScreen extends AbstractContainerScreen<RebarPlacerMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_rebar.png");

    public RebarPlacerScreen(RebarPlacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = 88;
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
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
