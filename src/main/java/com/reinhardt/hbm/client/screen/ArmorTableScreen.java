package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.ArmorTableMenu;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ArmorTableScreen extends AbstractContainerScreen<ArmorTableMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_armor_modifier.png");
    private static final String[] SLOT_HINTS = {
            "armor_mod.reinhardtshbm.type.helmet",
            "armor_mod.reinhardtshbm.type.chestplate",
            "armor_mod.reinhardtshbm.type.leggings",
            "armor_mod.reinhardtshbm.type.boots",
            "armor_mod.reinhardtshbm.type.servo",
            "armor_mod.reinhardtshbm.type.cladding",
            "armor_mod.reinhardtshbm.type.insert",
            "armor_mod.reinhardtshbm.type.special",
            "armor_mod.reinhardtshbm.type.battery",
            "armor_mod.reinhardtshbm.insert_here"
    };

    public ArmorTableScreen(ArmorTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 198;
        this.imageHeight = 222;
        this.inventoryLabelX = 30;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - 22) / 2 - this.font.width(this.title) / 2 + 22;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos + 22, this.topPos, 0, 0, this.imageWidth - 22, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos + 31, 176, 96, 22, 100);

        if (!this.menu.armorStack().isEmpty()) {
            int iconV = this.menu.armorStack().getItem() instanceof net.minecraft.world.item.ArmorItem ? 74 : 52;
            guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 60, 176, iconV, 22, 22);
        } else if (System.currentTimeMillis() % 1000L < 500L) {
            guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 60, 176, 52, 22, 22);
        }

        for (int slot = 0; slot < ArmorModHandler.MOD_SLOTS; slot++) {
            if (this.menu.getSlot(slot).hasItem()) {
                int u = this.menu.modApplicable(slot) ? 176 : 176;
                int v = this.menu.modApplicable(slot) ? 34 : 16;
                guiGraphics.blit(TEXTURE, this.leftPos + this.menu.getSlot(slot).x - 1, this.topPos + this.menu.getSlot(slot).y - 1, u, v, 18, 18);
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft != null
                && this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && !this.hoveredSlot.hasItem()
                && this.hoveredSlot.index >= 0
                && this.hoveredSlot.index < SLOT_HINTS.length) {
            ChatFormatting color = this.hoveredSlot.index < ArmorModHandler.MOD_SLOTS
                    ? ChatFormatting.LIGHT_PURPLE
                    : ChatFormatting.YELLOW;
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable(SLOT_HINTS[this.hoveredSlot.index]).withStyle(color),
                    mouseX,
                    mouseY
            );
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
