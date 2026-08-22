package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ParticleAcceleratorBlock;
import com.reinhardt.hbm.menu.ParticleAcceleratorMenu;
import com.reinhardt.hbm.network.ParticleAcceleratorControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ParticleAcceleratorScreen extends AbstractContainerScreen<ParticleAcceleratorMenu> {
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private EditBox thresholdBox;

    public ParticleAcceleratorScreen(ParticleAcceleratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = 111;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        if (this.menu.kind() == ParticleAcceleratorBlock.Kind.SOURCE) {
            addRenderableWidget(Button.builder(Component.literal("X"), button -> send(ParticleAcceleratorControlPayload.CANCEL, 0))
                    .bounds(this.leftPos + 152, this.topPos + 72, 16, 16)
                    .build());
        } else if (this.menu.kind() == ParticleAcceleratorBlock.Kind.DIPOLE) {
            addRenderableWidget(Button.builder(Component.translatable("gui.reinhardtshbm.pa_dipole.lower"), button -> send(ParticleAcceleratorControlPayload.CYCLE_LOWER, 0))
                    .bounds(this.leftPos + 62, this.topPos + 29, 52, 14)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("gui.reinhardtshbm.pa_dipole.upper"), button -> send(ParticleAcceleratorControlPayload.CYCLE_UPPER, 0))
                    .bounds(this.leftPos + 62, this.topPos + 43, 52, 14)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("gui.reinhardtshbm.pa_dipole.redstone"), button -> send(ParticleAcceleratorControlPayload.CYCLE_REDSTONE, 0))
                    .bounds(this.leftPos + 62, this.topPos + 57, 52, 14)
                    .build());
            this.thresholdBox = new EditBox(this.font, this.leftPos + 47, this.topPos + 77, 66, 10, Component.translatable("gui.reinhardtshbm.pa_dipole.threshold"));
            this.thresholdBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
            this.thresholdBox.setMaxLength(9);
            addRenderableWidget(this.thresholdBox);
            addRenderableWidget(Button.builder(Component.literal("OK"), button -> {
                        int value = this.thresholdBox.getValue().isEmpty() ? 0 : Integer.parseInt(this.thresholdBox.getValue());
                        send(ParticleAcceleratorControlPayload.SET_THRESHOLD, value);
                    })
                    .bounds(this.leftPos + 116, this.topPos + 75, 28, 14)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.energy", this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        } else if (isHovering(132, 18, 8, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.temperature_kelvin", String.format(java.util.Locale.US, "%.1f", this.menu.temperature()))), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (this.menu.kind() == ParticleAcceleratorBlock.Kind.DIPOLE) {
            guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.direction." + this.menu.directionName(this.menu.dirLower())), 118, 32, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.direction." + this.menu.directionName(this.menu.dirUpper())), 118, 46, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.direction." + this.menu.directionName(this.menu.dirRedstone())), 118, 60, 0x404040, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture(this.menu.kind());
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.fill(this.leftPos + 153, this.topPos + 70 - power, this.leftPos + 167, this.topPos + 70, 0xFFB00020);
        }
        int cold = this.menu.tankScaled(this.menu.coldAmount(), 52);
        if (cold > 0) {
            guiGraphics.fill(this.leftPos + 132, this.topPos + 70 - cold, this.leftPos + 136, this.topPos + 70, 0xFF80E8FF);
        }
        int hot = this.menu.tankScaled(this.menu.hotAmount(), 52);
        if (hot > 0) {
            guiGraphics.fill(this.leftPos + 138, this.topPos + 70 - hot, this.leftPos + 142, this.topPos + 70, 0xFFFF8040);
        }
    }

    private static ResourceLocation texture(ParticleAcceleratorBlock.Kind kind) {
        String name = switch (kind) {
            case RFC -> "gui_rfc";
            case QUADRUPOLE -> "gui_quadrupole";
            case DIPOLE -> "gui_dipole";
            case DETECTOR -> "gui_detector";
            default -> "gui_source";
        };
        return ReinhardtsHBM.id("textures/gui/particleaccelerator/" + name + ".png");
    }

    private void send(int action, int value) {
        PacketDistributor.sendToServer(new ParticleAcceleratorControlPayload(this.menu.blockPos(), action, value));
    }
}
