package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.CompressorMenu;
import com.reinhardt.hbm.network.CompressorControlPayload;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class CompressorScreen extends AbstractContainerScreen<CompressorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_compressor.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public CompressorScreen(CompressorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
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
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!renderHoverTooltips(guiGraphics, mouseX, mouseY)) {
            super.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        drawFluid(guiGraphics, 17, 70, 16, this.menu.inputFluidScaled(52), this.menu.inputFluid());
        drawFluid(guiGraphics, 107, 70, 16, this.menu.outputFluidScaled(52), this.menu.outputFluid());

        int energy = this.menu.energyScaled(52);
        if (energy > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - energy, 176, 52 - energy, 16, energy, TEX_W, TEX_H);
        }

        int progress = this.menu.progressScaled(55);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 42, this.topPos + 26, 192, 0, progress, 17, TEX_W, TEX_H);
        }

        int pressure = Math.max(0, Math.min(4, this.menu.inputPressure()));
        guiGraphics.blit(TEXTURE, this.leftPos + 43 + pressure * 11, this.topPos + 46, 193, 18, 8, 14, TEX_W, TEX_H);

        if (this.menu.energy() >= this.menu.usage()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 156, this.topPos + 4, 176, 52, 9, 12, TEX_W, TEX_H);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= this.topPos + 46 && mouseY < this.topPos + 60) {
            for (int pressure = 0; pressure < 5; pressure++) {
                int x = this.leftPos + 43 + pressure * 11;
                if (mouseX >= x && mouseX < x + 8) {
                    PacketDistributor.sendToServer(new CompressorControlPayload(this.menu.blockPos(), pressure));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean renderHoverTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(17, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.inputFluid(),
                    this.menu.inputAmount(),
                    CompressorBlockEntity.TANK_CAPACITY,
                    this.menu.inputPressure()
            ), mouseX, mouseY);
            return true;
        }
        if (isHovering(107, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.outputFluid(),
                    this.menu.outputAmount(),
                    CompressorBlockEntity.TANK_CAPACITY,
                    this.menu.outputPressure()
            ), mouseX, mouseY);
            return true;
        }
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    CompressorBlockEntity.MAX_POWER
            )), mouseX, mouseY);
            return true;
        }
        for (int pressure = 0; pressure < 5; pressure++) {
            if (isHovering(43 + pressure * 11, 46, 8, 14, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                        "tooltip.reinhardtshbm.compressor.pressure",
                        pressure,
                        pressure + 1
                )), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
