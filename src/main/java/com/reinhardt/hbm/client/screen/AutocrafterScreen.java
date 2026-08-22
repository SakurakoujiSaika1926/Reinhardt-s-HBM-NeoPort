package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.AutocrafterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Original 176x240 automatic crafting table GUI and its mode tooltips. */
public final class AutocrafterScreen extends AbstractContainerScreen<AutocrafterMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_autocrafter.png");

    public AutocrafterScreen(AutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = 146;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderModeTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int energy = (int) Math.min(52L, this.menu.power() * 52L / this.menu.maxPower());
        if (energy > 0) {
            graphics.blit(TEXTURE, this.leftPos + 17, this.topPos + 97 - energy, 176, 52 - energy, 16, energy);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    private void renderModeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(17, 45, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    NumberFormat.getIntegerInstance(Locale.US).format(this.menu.power()),
                    NumberFormat.getIntegerInstance(Locale.US).format(this.menu.maxPower())
            )), mouseX, mouseY);
        }

        if (!this.menu.getCarried().isEmpty()) {
            return;
        }

        for (int index = 0; index <= 8; index++) {
            Slot slot = this.menu.getSlot(index);
            if (!slot.hasItem() || !isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                continue;
            }
            String mode = this.menu.mode(index);
            if (!mode.isEmpty()) {
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable("tooltip.reinhardtshbm.autocrafter.change").withStyle(ChatFormatting.RED),
                        modeLabel(mode)
                ), mouseX, mouseY - 30);
            }
            return;
        }

        Slot preview = this.menu.getSlot(9);
        if (preview.hasItem() && isHovering(preview.x, preview.y, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                Component.translatable("tooltip.reinhardtshbm.autocrafter.change").withStyle(ChatFormatting.RED),
                Component.literal((this.menu.recipeIndex() + 1) + " / " + this.menu.recipeCount()).withStyle(ChatFormatting.YELLOW)
            ), mouseX, mouseY - 30);
        }
    }

    private static Component modeLabel(String mode) {
        return switch (mode) {
            case "exact" -> Component.translatable("tooltip.reinhardtshbm.autocrafter.mode.exact").withStyle(ChatFormatting.YELLOW);
            case "wildcard" -> Component.translatable("tooltip.reinhardtshbm.autocrafter.mode.wildcard").withStyle(ChatFormatting.YELLOW);
            case "bedrock" -> Component.translatable("tooltip.reinhardtshbm.autocrafter.mode.bedrock").withStyle(ChatFormatting.YELLOW);
            default -> Component.translatable("tooltip.reinhardtshbm.autocrafter.mode.tag", mode).withStyle(ChatFormatting.YELLOW);
        };
    }
}
