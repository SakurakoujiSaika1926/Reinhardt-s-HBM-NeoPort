package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.BlastFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class BlastFurnaceScreen extends AbstractContainerScreen<BlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/guidifurnace.png");

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory playerInventory, Component title) {
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.menu.getCarried().isEmpty()) {
            for (int slotIndex = 0; slotIndex < 3; slotIndex++) {
                Slot slot = this.menu.slots.get(slotIndex);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    guiGraphics.renderComponentTooltip(
                            this.font,
                            List.of(Component.translatable(
                                    "tooltip.reinhardtshbm.accepts_items_from",
                                    Component.literal(BlastFurnaceMenu.legacyDirectionName(this.menu.sideMode(slotIndex)))
                            )),
                            mouseX,
                            mouseY - (slot.hasItem() ? 15 : 0)
                    );
                    return;
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.hasFuel()) {
            int fuelPixels = this.menu.fuelScaled(52);
            guiGraphics.blit(TEXTURE, this.leftPos + 44, this.topPos + 70 - fuelPixels, 201, 53 - fuelPixels, 16, fuelPixels);
        }

        int progressPixels = this.menu.progressScaled(24);
        guiGraphics.blit(TEXTURE, this.leftPos + 101, this.topPos + 35, 176, 14, progressPixels + 1, 17);

        if (this.menu.hasFuel() && (this.menu.canProcess() || progressPixels > 0)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 37, 176, 0, 14, 14);
        }
    }
}
