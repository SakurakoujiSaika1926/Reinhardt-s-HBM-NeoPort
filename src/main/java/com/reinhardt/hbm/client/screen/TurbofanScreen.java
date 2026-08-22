package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.TurbofanMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Direct layout port of GUIMachineTurbofan from HBM 1.7.10. */
public final class TurbofanScreen extends AbstractContainerScreen<TurbofanMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/generators/gui_turbofan.png");

    public TurbofanScreen(TurbofanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 43 - this.font.width(this.title) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int energy = Math.min(52, this.menu.energy() * 52 / Math.max(1, this.menu.energyCapacity()));
        if (energy > 0) {
            graphics.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - energy, 192, 52 - energy, 16, energy, 256, 256);
        }
        int afterburner = this.menu.afterburner();
        if (afterburner > 0) {
            graphics.blit(TEXTURE, this.leftPos + 98, this.topPos + 44, 176, (Math.min(afterburner, 6) - 1) * 16, 16, 16, 256, 256);
        }
        drawFluid(graphics, 35, 69, 34, 52, this.menu.fuel(), this.menu.fuelAmount(), 24_000);
        if (this.menu.showBlood()) {
            drawFluid(graphics, 98, 32, 16, 16, this.menu.blood(), this.menu.bloodAmount(), 24_000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(35, 17, 34, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fuel(), this.menu.fuelAmount(), 24_000), mouseX, mouseY);
        } else if (this.menu.showBlood() && isHovering(98, 16, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.blood(), this.menu.bloodAmount(), 24_000), mouseX, mouseY);
        } else if (isHovering(143, 17, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottom, int width, int maxHeight,
                           HbmFluidDefinition fluid, int amount, int capacity) {
        if (fluid.isNone() || amount <= 0) return;
        int height = Math.min(maxHeight, amount * maxHeight / Math.max(1, capacity));
        graphics.fill(this.leftPos + x, this.topPos + bottom - height,
                this.leftPos + x + width, this.topPos + bottom, 0xFF000000 | fluid.color());
    }
}
