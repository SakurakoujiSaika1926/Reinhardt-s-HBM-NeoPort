package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SteamTurbineBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.SteamTurbineMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SteamTurbineScreen extends AbstractContainerScreen<SteamTurbineMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_turbine.png");

    public SteamTurbineScreen(SteamTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
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

        if (isHovering(62, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity()), mouseX, mouseY);
        }
        if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity()), mouseX, mouseY);
        }
        if (isHovering(123, 35, 7, 34, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    SteamTurbineBlockEntity.ENERGY_CAPACITY
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawFluid(guiGraphics, 62, 69, this.menu.inputScaled(52), this.menu.inputFluid());
        drawFluid(guiGraphics, 134, 69, this.menu.outputScaled(52), this.menu.outputFluid());
        drawSteamIcon(guiGraphics);

        int powerPixels = this.menu.powerScaled(34);
        if (powerPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 123, this.topPos + 69 - powerPixels, 176, 34 - powerPixels, 7, powerPixels);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + 16, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + 16, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private void drawSteamIcon(GuiGraphics graphics) {
        int v = switch (this.menu.inputFluid().name()) {
            case "steam" -> 0;
            case "hotsteam" -> 14;
            case "superhotsteam" -> 28;
            case "ultrahotsteam" -> 42;
            default -> -1;
        };
        if (v >= 0) {
            graphics.blit(TEXTURE, this.leftPos + 99, this.topPos + 18, 183, v, 14, 14);
        }
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}
