package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DfcInjectorMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DfcInjectorScreen extends AbstractContainerScreen<DfcInjectorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/dfc/gui_injector.png");

    public DfcInjectorScreen(DfcInjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
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
        if (isHovering(44, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0)), mouseX, mouseY);
        } else if (isHovering(116, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        DfcScreenUtil.drawFluid(this.minecraft, guiGraphics, this.leftPos, this.topPos, 44, 69, 16, this.menu.tankScaled(0, 52), this.menu.tankFluid(0));
        DfcScreenUtil.drawFluid(this.minecraft, guiGraphics, this.leftPos, this.topPos, 116, 69, 16, this.menu.tankScaled(1, 52), this.menu.tankFluid(1));
    }
}
