package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.blockentity.CustomMachineBlockEntity;
import com.reinhardt.hbm.menu.CustomMachineMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** A compact native screen; JEI intentionally uses its own recipe-only layout. */
public final class CustomMachineScreen extends AbstractContainerScreen<CustomMachineMenu> {
    public CustomMachineScreen(CustomMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = 88;
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
        int left = this.leftPos;
        int top = this.topPos;
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFFC6C6C6);
        graphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + 82, 0xFF8B8B8B);
        graphics.fill(left + 5, top + 5, left + this.imageWidth - 5, top + 81, 0xFFB5B5B5);
        drawSlot(graphics, 43, 41);
        drawSlot(graphics, 115, 41);
        drawSlot(graphics, 151, 66);

        graphics.fill(left + 66, top + 47, left + 108, top + 53, 0xFF3A3A3A);
        int progress = this.menu.progressScaled(40);
        if (progress > 0) {
            graphics.fill(left + 67, top + 48, left + 67 + progress, top + 52, 0xFF40A840);
        }
        graphics.drawString(this.font, "→", left + 82, top + 35, 0xFF404040, false);

        graphics.fill(left + 11, top + 19, left + 27, top + 71, 0xFF303030);
        int fluid = this.menu.fluidScaled(50);
        if (fluid > 0) {
            graphics.fill(left + 12, top + 70 - fluid, left + 26, top + 70,
                    0xFF000000 | this.menu.fluid().color());
        }
        graphics.fill(left + 132, top + 19, left + 148, top + 71, 0xFF303030);
        int energy = this.menu.energyScaled(50);
        if (energy > 0) {
            graphics.fill(left + 133, top + 70 - energy, left + 147, top + 70, 0xFFF2D13B);
        }

        Component status = Component.translatable(this.menu.structureValid()
                ? "gui.reinhardtshbm.custom_machine.structure_valid"
                : "gui.reinhardtshbm.custom_machine.structure_invalid");
        graphics.drawString(this.font, status, left + 7, top + 74,
                this.menu.structureValid() ? 0xFF206020 : 0xFF9A2020, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(11, 19, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.fluid(), this.menu.fluidAmount(), CustomMachineBlockEntity.TANK_CAPACITY),
                    mouseX, mouseY);
            return;
        }
        if (isHovering(132, 19, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), CustomMachineBlockEntity.MAX_POWER)),
                    mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + 18, this.topPos + y + 18, 0xFF404040);
        graphics.fill(this.leftPos + x + 1, this.topPos + y + 1,
                this.leftPos + x + 17, this.topPos + y + 17, 0xFF8B8B8B);
    }
}
