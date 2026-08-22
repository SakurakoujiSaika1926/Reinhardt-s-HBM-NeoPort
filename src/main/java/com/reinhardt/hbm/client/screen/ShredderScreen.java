package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.menu.ShredderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ShredderScreen extends AbstractContainerScreen<ShredderMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_shredder.png");

    public ShredderScreen(ShredderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
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

        if (isHovering(8, 18, 16, 88, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    ShredderBlockEntity.ENERGY_CAPACITY
            )), mouseX, mouseY);
        }

        if (isHovering(63, 89, 35, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    ShredderBlockEntity.PROCESSING_SPEED
            )), mouseX, mouseY);
        }

        if (isHovering(43, 71, 18, 18, mouseX, mouseY)) {
            renderGearTooltip(guiGraphics, mouseX, mouseY, true, this.menu.leftGear());
        }
        if (isHovering(79, 71, 18, 18, mouseX, mouseY)) {
            renderGearTooltip(guiGraphics, mouseX, mouseY, false, this.menu.rightGear());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energyPixels = this.menu.energyScaled(88);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 8,
                    this.topPos + 106 - energyPixels,
                    176,
                    160 - energyPixels,
                    16,
                    energyPixels
            );
        }

        int progressPixels = this.menu.progressScaled(34);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 89, 176, 54, progressPixels + 1, 18);
        }

        renderGearIcon(guiGraphics, true, this.menu.leftGear());
        renderGearIcon(guiGraphics, false, this.menu.rightGear());
    }

    private void renderGearIcon(GuiGraphics guiGraphics, boolean left, int state) {
        if (state <= 0) {
            return;
        }
        int u = left ? 176 : 194;
        int v = Math.min(2, state - 1) * 18;
        int x = left ? 43 : 79;
        guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + 71, u, v, 18, 18);
    }

    private void renderGearTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean left, int state) {
        Component side = Component.translatable(left ? "tooltip.reinhardtshbm.shredder.left_blade" : "tooltip.reinhardtshbm.shredder.right_blade");
        Component status = switch (state) {
            case 1 -> Component.translatable("tooltip.reinhardtshbm.shredder.blade_good");
            case 2 -> Component.translatable("tooltip.reinhardtshbm.shredder.blade_worn");
            case 3 -> Component.translatable("tooltip.reinhardtshbm.shredder.blade_broken");
            default -> Component.translatable("tooltip.reinhardtshbm.shredder.blade_missing");
        };
        guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                "tooltip.reinhardtshbm.shredder.blade_state",
                side,
                status
        )), mouseX, mouseY);
    }
}
