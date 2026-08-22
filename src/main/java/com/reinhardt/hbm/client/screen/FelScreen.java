package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.FelMenu;
import com.reinhardt.hbm.util.Wavelength;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class FelScreen extends AbstractContainerScreen<FelMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_fel.png");

    public FelScreen(FelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 203;
        this.imageHeight = 169;
        this.inventoryLabelY = this.imageHeight - 26;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 90 + this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inButton(mouseX, mouseY, 142, 41, 29, 17)) {
            Minecraft minecraft = this.minecraft;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.enabled()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 142, this.topPos + 41, 203, 0, 29, 17);
        }

        int power = this.menu.powerScaled(113);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 182, this.topPos + 27 + 113 - power, 203, 17 + 113 - power, 16, power);
        }

        Wavelength mode = this.menu.mode();
        if (mode != Wavelength.NULL && this.menu.enabled() && this.menu.distance() > 0 && this.menu.power() > 0) {
            int color = 0xFF000000 | mode.guiColor(this.minecraft == null || this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime());
            guiBeam(guiGraphics, this.leftPos + 113, this.topPos + 31, this.leftPos + 135, this.topPos + 31, color);
            guiBeam(guiGraphics, this.leftPos, this.topPos + 31, this.leftPos + 4, this.topPos + 31, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 90 + this.imageWidth / 2 - this.font.width(this.title) / 2, 7, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 98, 0x404040, false);

        if (this.menu.missingValidSilex() && this.menu.enabled()) {
            Component text = Component.literal("ERR.");
            guiGraphics.drawString(this.font, text, 55 + this.imageWidth / 2 - this.font.width(this.title) / 2, 9, 0xFF0000, false);
        } else if (this.menu.enabled()) {
            Component text = Component.literal("LIVE");
            guiGraphics.drawString(this.font, text, 54 + this.imageWidth / 2 - this.font.width(this.title) / 2, 9, 0x00FF00, false);
        }
    }

    private boolean inButton(double mouseX, double mouseY, int x, int y, int width, int height) {
        double left = this.leftPos + x;
        double top = this.topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private static void guiBeam(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        for (int y = -2; y <= 2; y++) {
            guiGraphics.hLine(minX, maxX, y1 + y, color);
        }
    }
}
