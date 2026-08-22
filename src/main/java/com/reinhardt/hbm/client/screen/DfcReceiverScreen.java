package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DfcReceiverMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DfcReceiverScreen extends AbstractContainerScreen<DfcReceiverMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/dfc/gui_receiver.png");

    public DfcReceiverScreen(DfcReceiverMenu menu, Inventory playerInventory, Component title) {
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
        if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.dfc.input"), 40, 25, 0xFF7F7F, false);
        guiGraphics.drawString(this.font, DfcScreenUtil.shortNumber(this.menu.joules()) + "Spk", 50, 35, 0xFF7F7F, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.dfc.output"), 40, 45, 0xFF7F7F, false);
        guiGraphics.drawString(this.font, DfcScreenUtil.shortNumber(this.menu.power()) + "HE", 50, 55, 0xFF7F7F, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        DfcScreenUtil.drawFluid(this.minecraft, guiGraphics, this.leftPos, this.topPos, 8, 69, 16, this.menu.fluidScaled(52), this.menu.fluid());
    }
}
