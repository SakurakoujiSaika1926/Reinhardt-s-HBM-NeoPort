package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.PwrMenu;
import com.reinhardt.hbm.network.PwrControlPayload;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class PwrScreen extends AbstractContainerScreen<PwrMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_pwr.png");
    private EditBox rodTarget;

    public PwrScreen(PwrMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.rodTarget = new EditBox(this.font, this.leftPos + 127, this.topPos + 33, 36, 12, Component.empty());
        this.rodTarget.setMaxLength(3);
        this.rodTarget.setValue(Integer.toString((int) Math.round(this.menu.rodTarget())));
        this.rodTarget.setFilter(value -> value.isEmpty() || value.matches("\\d{0,3}"));
        this.addRenderableWidget(this.rodTarget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderHoverInfo(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawVertical(guiGraphics, 63, 22, 7, 52, this.menu.heatScaled(52, true), 176, 0);
        drawVertical(guiGraphics, 73, 22, 7, 52, this.menu.heatScaled(52, false), 183, 0);
        int progress = this.menu.progressScaled(24);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 96, this.topPos + 53, 190, 0, progress, 16);
        }
        int coolant = Math.min(52, this.menu.coolantAmount() * 52 / 128_000);
        int hot = Math.min(52, this.menu.hotCoolantAmount() * 52 / 128_000);
        drawVertical(guiGraphics, 84, 22, 7, 52, coolant, 214, 0);
        drawVertical(guiGraphics, 93, 22, 7, 52, hot, 221, 0);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.pwr.rods"), 124, 20, 4210752, false);
        guiGraphics.drawString(this.font, this.menu.amountLoaded() + "/" + this.menu.rodCount(), 124, 52, 4210752, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.rodTarget != null && this.rodTarget.keyPressed(keyCode, scanCode, modifiers)) {
            sendRodTarget();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.rodTarget != null && this.rodTarget.charTyped(codePoint, modifiers)) {
            sendRodTarget();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void sendRodTarget() {
        if (this.rodTarget == null || this.rodTarget.getValue().isEmpty()) {
            return;
        }
        int value = Math.max(0, Math.min(100, Integer.parseInt(this.rodTarget.getValue())));
        PacketDistributor.sendToServer(new PwrControlPayload(this.menu.blockPos(), value));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.4F));
        }
    }

    private void renderHoverInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(63, 22, 7, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.pwr.core_heat", this.menu.coreHeat())), mouseX, mouseY);
        }
        if (isHovering(73, 22, 7, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.pwr.hull_heat", this.menu.hullHeat())), mouseX, mouseY);
        }
        if (isHovering(84, 22, 7, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.coolantFluid(), this.menu.coolantAmount(), 128_000), mouseX, mouseY);
        }
        if (isHovering(93, 22, 7, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.hotCoolantFluid(), this.menu.hotCoolantAmount(), 128_000), mouseX, mouseY);
        }
    }

    private void drawVertical(GuiGraphics graphics, int x, int y, int width, int height, int scaled, int u, int v) {
        if (scaled <= 0) {
            return;
        }
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y + height - scaled, u, v + height - scaled, width, scaled);
    }
}
