package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.RadGenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direct layout port of GUIMachineRadGen from HBM 1.7.10. */
public final class RadGenScreen extends AbstractContainerScreen<RadGenMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_radgen.png");

    public RadGenScreen(RadGenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        for (int slot = 0; slot < 12; slot++) {
            int duration = this.menu.duration(slot);
            if (duration <= 0) continue;
            int width = Math.min(44, this.menu.progress(slot) * 44 / duration);
            if (width > 0) {
                graphics.blit(TEXTURE, this.leftPos + 66, this.topPos + 19 + slot * 5,
                        176, 0, width, 3, 256, 256);
            }
        }
        int power = Math.min(48, this.menu.energy() * 48 / Math.max(1, this.menu.energyCapacity()));
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 64, this.topPos + 83,
                    176, 3, power, 4, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(64, 83, 48, 4, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, java.util.List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }
        for (int slot = 0; slot < 12; slot++) {
            int duration = this.menu.duration(slot);
            if (duration <= 0 || !isHovering(65, 18 + slot * 5, 46, 5, mouseX, mouseY)) continue;
            int remaining = Math.max(0, duration - this.menu.progress(slot));
            graphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.translatable("gui.reinhardtshbm.radgen.slot", slot + 1),
                    Component.translatable("gui.reinhardtshbm.radgen.production", this.menu.production(slot)),
                    Component.translatable("gui.reinhardtshbm.radgen.remaining", remaining,
                            remaining * 100 / duration)
            ), mouseX, mouseY);
            break;
        }
    }
}
