package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.drone.DroneItemMatcher;
import com.reinhardt.hbm.menu.DroneRequesterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Exact 1.7.10 requester layout with the filter-mode hover text restored. */
public final class DroneRequesterScreen extends AbstractContainerScreen<DroneRequesterMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_drone_requester.png");

    public DroneRequesterScreen(DroneRequesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty()) {
            int filter = hoveredFilter(mouseX, mouseY);
            String mode = filter < 0 ? null : menu.filterMode(filter);
            if (mode != null) {
                Component modeLabel = DroneItemMatcher.EXACT.equals(mode)
                        ? Component.translatable("tooltip.reinhardtshbm.autocrafter.mode.exact")
                        : DroneItemMatcher.WILDCARD.equals(mode)
                        ? Component.translatable("tooltip.reinhardtshbm.autocrafter.mode.wildcard")
                        : Component.literal(mode);
                graphics.renderComponentTooltip(font, java.util.List.of(
                        Component.translatable("tooltip.reinhardtshbm.autocrafter.change").withStyle(ChatFormatting.RED), modeLabel
                ), mouseX, mouseY - 30);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    private int hoveredFilter(int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (isHovering(98 + column * 18, 17 + row * 18, 18, 18, mouseX, mouseY)) {
                    return column + row * 3;
                }
            }
        }
        return -1;
    }
}
