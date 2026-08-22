package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MixerBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.MixerMenu;
import com.reinhardt.hbm.network.MixerControlPayload;
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

public class MixerScreen extends AbstractContainerScreen<MixerMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_mixer.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public MixerScreen(MixerMenu menu, Inventory playerInventory, Component title) {
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
        renderHoverTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        int energy = this.menu.energyScaled(52);
        if (energy > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 23, this.topPos + 75 - energy, 176, 52 - energy, 16, energy, TEX_W, TEX_H);
        }

        int progress = this.menu.progressScaled(53);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 36, 192, 0, progress, 44, TEX_W, TEX_H);
        }

        drawFluid(guiGraphics, 43, 75, 7, this.menu.input1Scaled(52), this.menu.inputFluid1());
        drawFluid(guiGraphics, 52, 75, 7, this.menu.input2Scaled(52), this.menu.inputFluid2());
        drawFluid(guiGraphics, 117, 75, 16, this.menu.outputScaled(52), this.menu.outputFluid());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(62, 22, 12, 12, mouseX, mouseY)) {
            PacketDistributor.sendToServer(new MixerControlPayload(this.menu.blockPos()));
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderHoverTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(23, 23, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    MixerBlockEntity.MAX_POWER
            )), mouseX, mouseY);
        }
        if (isHovering(43, 23, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.inputFluid1(), this.menu.inputAmount1(), this.menu.inputCapacity1()), mouseX, mouseY);
        }
        if (isHovering(52, 23, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.inputFluid2(), this.menu.inputAmount2(), this.menu.inputCapacity2()), mouseX, mouseY);
        }
        if (isHovering(117, 23, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity()), mouseX, mouseY);
        }
        if (isHovering(62, 36, 53, 44, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    this.menu.processTime()
            )), mouseX, mouseY);
        }
        if (isHovering(62, 22, 12, 12, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.recipe_index",
                    this.menu.recipeIndex() + 1
            )), mouseX, mouseY);
        }
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
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
