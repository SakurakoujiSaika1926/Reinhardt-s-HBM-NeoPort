package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.GasFlareMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class GasFlareScreen extends AbstractContainerScreen<GasFlareMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/generators/gui_flare_stack.png");

    public GasFlareScreen(GasFlareMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(79, 16, 35, 10, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(Component.translatable("gui.reinhardtshbm.gas_flare.valve")),
                    mouseX,
                    mouseY
            );
        }
        if (isHovering(79, 50, 35, 14, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(Component.translatable("gui.reinhardtshbm.gas_flare.ignition")),
                    mouseX,
                    mouseY
            );
        }
        if (isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity()),
                    mouseX,
                    mouseY
            );
        }
        if (isHovering(143, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(Component.translatable(
                            "tooltip.reinhardtshbm.energy",
                            this.menu.power(),
                            this.menu.powerCapacity()
                    )),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(89, 16, 16, 10, mouseX, mouseY)) {
            sendButton(0);
            playClick();
            return true;
        }
        if (button == 0 && isHovering(89, 50, 16, 14, mouseX, mouseY)) {
            sendButton(1);
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - power, 176, 94 - power, 16, power);
        }
        if (this.menu.valveOpen()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 15, 176, 0, 35, 10);
        }
        if (this.menu.ignitionEnabled()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 49, 176, 10, 35, 14);
        }
        if (this.menu.valveOpen()
                && this.menu.ignitionEnabled()
                && this.menu.fluidAmount() > 0
                && this.menu.fluid().flammableHeatPerMillibucket() > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 29, 176, 24, 18, 18);
        }

        int fluid = this.menu.fluidScaled(52);
        if (fluid > 0 && !this.menu.fluid().isNone()) {
            int color = 0xD0000000 | this.menu.fluid().color();
            guiGraphics.fill(
                    this.leftPos + 35,
                    this.topPos + 69 - fluid,
                    this.leftPos + 51,
                    this.topPos + 69,
                    color
            );
            guiGraphics.fill(
                    this.leftPos + 35,
                    this.topPos + 69 - fluid,
                    this.leftPos + 51,
                    this.topPos + 70 - fluid,
                    0x66FFFFFF
            );
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
