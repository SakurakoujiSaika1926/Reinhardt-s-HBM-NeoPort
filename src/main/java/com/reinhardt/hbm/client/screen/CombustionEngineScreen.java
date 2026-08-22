package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.CombustionEngineBlockEntity;
import com.reinhardt.hbm.menu.CombustionEngineMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class CombustionEngineScreen extends AbstractContainerScreen<CombustionEngineMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/generators/gui_combustion.png");
    private boolean draggingThrottle;

    public CombustionEngineScreen(CombustionEngineMenu menu, Inventory playerInventory, Component title) {
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fuelFluid(), this.menu.fuelAmount(), this.menu.fuelCapacity()), mouseX, mouseY);
        }
        if (isHovering(143, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    this.menu.powerCap()
            )), mouseX, mouseY);
        }
        if (isHovering(79, 38, 36, 8, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(String.format(Locale.ROOT, "%.1f mB/t", this.menu.throttle() * 0.2D))), mouseX, mouseY);
        }
        if (isHovering(79, 50, 35, 14, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(String.format(Locale.ROOT, "%,d HE/t", this.menu.hePerTick())),
                    Component.literal(String.format(Locale.ROOT, "%,d HE/s", this.menu.hePerTick() * 20)),
                    Component.translatable("tooltip.reinhardtshbm.combustion_engine.efficiency", this.menu.efficiencyPercent())
            ), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(89, 13, 16, 14, mouseX, mouseY)) {
            sendButton(0);
            playClick();
            return true;
        }
        if (button == 0 && isHovering(79, 38, 36, 8, mouseX, mouseY)) {
            this.draggingThrottle = true;
            sendThrottle(mouseX);
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingThrottle) {
            sendThrottle(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingThrottle = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int fuel = this.menu.fuelScaled(52);
        if (fuel > 0 && !this.menu.fuelFluid().isNone()) {
            int color = 0xFF000000 | this.menu.fuelFluid().color();
            guiGraphics.fill(this.leftPos + 35, this.topPos + 69 - fuel, this.leftPos + 51, this.topPos + 69, color);
            guiGraphics.fill(this.leftPos + 35, this.topPos + 69 - fuel, this.leftPos + 51, this.topPos + 70 - fuel, 0x66FFFFFF);
        }
        if (this.menu.enabled()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 13, 192, 0, 35, 15);
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 79 + this.menu.throttleSliderX(), this.topPos + 38, 192, 15, 4, 8);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - power, 176, 52 - power, 16, power);
        }
    }

    private void sendThrottle(double mouseX) {
        int throttle = (int) ((mouseX - this.leftPos - 81) * CombustionEngineBlockEntity.MAX_THROTTLE / 32.0D);
        throttle = Math.max(0, Math.min(CombustionEngineBlockEntity.MAX_THROTTLE, throttle));
        sendButton(throttle + 1);
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
