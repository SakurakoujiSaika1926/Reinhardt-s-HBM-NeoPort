package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyHeldInventoryItem;
import com.reinhardt.hbm.menu.LegacyHeldInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original GUI art and dimensions for ItemPlasticBag and ItemLeadBox. */
public final class LegacyHeldInventoryScreen extends AbstractContainerScreen<LegacyHeldInventoryMenu> {
    private static final ResourceLocation PLASTIC_BAG = ReinhardtsHBM.id("textures/gui/storage/gui_plastic_bag.png");
    private static final ResourceLocation CONTAINMENT_BOX = ReinhardtsHBM.id("textures/gui/gui_containment.png");

    public LegacyHeldInventoryScreen(LegacyHeldInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        boolean bag = menu.kind() == LegacyHeldInventoryItem.Kind.PLASTIC_BAG;
        this.imageWidth = 176;
        this.imageHeight = bag ? 216 : 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = menu.kind() == LegacyHeldInventoryItem.Kind.PLASTIC_BAG ? PLASTIC_BAG : CONTAINMENT_BOX;
        graphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.kind() == LegacyHeldInventoryItem.Kind.CONTAINMENT_BOX) {
            super.renderLabels(graphics, mouseX, mouseY);
        } else {
            graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        }
    }
}
