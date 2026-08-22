package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DfcCoreMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DfcCoreScreen extends AbstractContainerScreen<DfcCoreMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/dfc/gui_core.png");

    public DfcCoreScreen(DfcCoreMenu menu, Inventory playerInventory, Component title) {
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
        if (isHovering(26, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.tankFluid(0), this.menu.tankAmount(0), this.menu.tankCapacity(0)), mouseX, mouseY);
        } else if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.tankFluid(1), this.menu.tankAmount(1), this.menu.tankCapacity(1)), mouseX, mouseY);
        } else if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.dfc.field", this.menu.field())), mouseX, mouseY);
        } else if (isHovering(152, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.dfc.heat", this.menu.heat())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int field = this.menu.fieldScaled(52);
        if (field > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 69 - field, 176, 52 - field, 16, field);
        }
        int heat = this.menu.heatScaled(52);
        if (heat > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 69 - heat, 192, 52 - heat, 16, heat);
        }
        DfcScreenUtil.drawFluid(this.minecraft, guiGraphics, this.leftPos, this.topPos, 26, 69, 16, this.menu.tankScaled(0, 52), this.menu.tankFluid(0));
        DfcScreenUtil.drawFluid(this.minecraft, guiGraphics, this.leftPos, this.topPos, 134, 69, 16, this.menu.tankScaled(1, 52), this.menu.tankFluid(1));
    }
}
