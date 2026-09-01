package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SteelFurnaceBlockEntity;
import com.reinhardt.hbm.menu.SteelFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class SteelFurnaceScreen extends AbstractContainerScreen<SteelFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_furnace_steel.png");

    public SteelFurnaceScreen(SteelFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            super.renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        Component tooltip = this.legacyInfoTooltip(mouseX, mouseY);
        if (tooltip != null) {
            this.setTooltipForNextRenderPass(tooltip);
        }
    }

    private Component legacyInfoTooltip(int mouseX, int mouseY) {
        for (int index = 0; index < 3; index++) {
            if (isHovering(53, 17 + 18 * index, 70, 7, mouseX, mouseY)) {
                return Component.literal(format(this.menu.progress(index)) + " / "
                        + format(SteelFurnaceBlockEntity.PROCESS_TIME) + "TU");
            }
            if (isHovering(53, 26 + 18 * index, 70, 7, mouseX, mouseY)) {
                return Component.literal("Bonus: " + this.menu.bonus(index) + "%");
            }
        }

        if (isHovering(151, 18, 9, 50, mouseX, mouseY)) {
            return Component.literal(format(this.menu.heat()) + " / "
                    + format(SteelFurnaceBlockEntity.MAX_HEAT) + "TU");
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int heat = this.menu.heatScaled(48);
        if (heat > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 67 - heat, 176, 76 - heat, 7, heat);
        }

        for (int index = 0; index < 3; index++) {
            int progress = this.menu.progressScaled(index, 69);
            if (progress > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 54, this.topPos + 18 + 18 * index, 176, 18, progress, 5);
            }

            int bonus = this.menu.bonusScaled(index, 69);
            if (bonus > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 54, this.topPos + 27 + 18 * index, 176, 23, bonus, 5);
            }

            if (this.menu.wasOn()) {
                guiGraphics.blit(TEXTURE, this.leftPos + 16, this.topPos + 16 + 18 * index, 176, 0, 18, 18);
            }
        }
    }

    private static String format(int value) {
        return String.format(Locale.US, "%,d", value);
    }
}
