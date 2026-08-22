package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MicrowaveBlockEntity;
import com.reinhardt.hbm.menu.MicrowaveMenu;
import com.reinhardt.hbm.network.MicrowaveControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class MicrowaveScreen extends AbstractContainerScreen<MicrowaveMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/processing/gui_microwave.png");
    private static final int TEXTURE_SIZE = 256;

    public MicrowaveScreen(MicrowaveMenu menu, Inventory playerInventory, Component title) {
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

        if (isHovering(8, 17, 16, 34, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), MicrowaveBlockEntity.MAX_POWER)), mouseX, mouseY);
        }
        if (isHovering(104, 34, 22, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress", this.menu.progress(), MicrowaveBlockEntity.MAX_TIME)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        int energy = this.menu.energyScaled(34);
        if (energy > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 51 - energy,
                    176, 34 - energy, 16, energy, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        int progress = Math.min(this.menu.progressScaled(23), 22);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 104, this.topPos + 34,
                    192, 0, progress, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        int speed = this.menu.speedScaled(34);
        if (speed > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 60 - speed,
                    214, 34 - speed, 4, speed, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.blockPos() != null) {
            int delta = 0;
            if (inside(mouseX, mouseY, 43, 25, 18, 18)) {
                delta = 1;
            } else if (inside(mouseX, mouseY, 43, 43, 18, 18)) {
                delta = -1;
            }
            if (delta != 0) {
                PacketDistributor.sendToServer(new MicrowaveControlPayload(this.menu.blockPos(), delta));
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }
}
