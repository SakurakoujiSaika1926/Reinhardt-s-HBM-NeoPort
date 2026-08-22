package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.NukeBoyBlockEntity;
import com.reinhardt.hbm.menu.NukeBoyMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class NukeBoyScreen extends AbstractContainerScreen<NukeBoyMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/weapon/lil_boy_schematic.png");

    public NukeBoyScreen(NukeBoyMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
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
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("desc.gui.nuke_boy.0").withStyle(ChatFormatting.GRAY),
                    Component.translatable("desc.gui.nuke_boy.1").withStyle(ChatFormatting.RED)
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isReady()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 142, this.topPos + 90, 176, 0, 16, 16);
        }
        if (this.menu.slotHasExpectedItem(NukeBoyBlockEntity.SLOT_SHIELDING)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 27, this.topPos + 87, 176, 16, 21, 22);
        }
        if (this.menu.slotHasExpectedItem(NukeBoyBlockEntity.SLOT_TARGET)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 27, this.topPos + 89, 176, 38, 21, 18);
        }
        if (this.menu.slotHasExpectedItem(NukeBoyBlockEntity.SLOT_BULLET)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 74, this.topPos + 94, 176, 57, 19, 8);
        }
        if (this.menu.slotHasExpectedItem(NukeBoyBlockEntity.SLOT_PROPELLANT)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 92, this.topPos + 95, 176, 66, 12, 6);
        }
        if (this.menu.slotHasExpectedItem(NukeBoyBlockEntity.SLOT_IGNITER)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 107, this.topPos + 91, 176, 75, 16, 14);
        }
        drawInfoPanel(guiGraphics, -16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 176, 89, 16, 16);
    }
}
