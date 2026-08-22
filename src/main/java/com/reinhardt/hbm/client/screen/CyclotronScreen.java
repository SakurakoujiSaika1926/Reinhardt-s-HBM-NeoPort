package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.CyclotronMenu;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CyclotronScreen extends AbstractContainerScreen<CyclotronMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_cyclotron.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public CyclotronScreen(CyclotronMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 190;
        this.imageHeight = 215;
        this.inventoryLabelX = 15;
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

        if (isHovering(168, 18, 16, 63, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    com.reinhardt.hbm.blockentity.CyclotronBlockEntity.MAX_POWER
            )), mouseX, mouseY);
        } else if (isHovering(11, 81, 34, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(HbmFluids.byName("water").orElse(HbmFluids.none()), menu.waterAmount(), menu.waterCapacity()), mouseX, mouseY);
        } else if (isHovering(11, 90, 34, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), menu.spentSteamAmount(), menu.spentSteamCapacity()), mouseX, mouseY);
        } else if (isHovering(107, 81, 34, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(HbmFluids.byName("amat").orElse(HbmFluids.none()), menu.antimatterAmount(), menu.antimatterCapacity()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        int power = this.menu.powerScaled(63);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 168, this.topPos + 80 - power, 190, 62 - power, 16, power, TEX_W, TEX_H);
        }

        int progress = this.menu.progressScaled(34);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 48, this.topPos + 27, 206, 0, progress, 34, TEX_W, TEX_H);
            guiGraphics.blit(TEXTURE, this.leftPos + 172, this.topPos + 4, 190, 63, 9, 12, TEX_W, TEX_H);
        }

        drawFluid(guiGraphics, 11, 88, 34, this.menu.tankScaled(menu.waterAmount(), menu.waterCapacity(), 7), HbmFluids.byName("water").orElse(HbmFluids.none()).color());
        drawFluid(guiGraphics, 11, 97, 34, this.menu.tankScaled(menu.spentSteamAmount(), menu.spentSteamCapacity(), 7), HbmFluids.byName("spentsteam").orElse(HbmFluids.none()).color());
        drawFluid(guiGraphics, 107, 97, 34, this.menu.tankScaled(menu.antimatterAmount(), menu.antimatterCapacity(), 16), HbmFluids.byName("amat").orElse(HbmFluids.none()).color());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, int color) {
        if (height <= 0) {
            return;
        }
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }
}
