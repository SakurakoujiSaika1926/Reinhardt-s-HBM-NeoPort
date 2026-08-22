package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.ExcavatorMenu;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ExcavatorScreen extends AbstractContainerScreen<ExcavatorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_mining_drill.png");
    private static final Component[] TOOLTIPS = {
            Component.translatable("excavator.drill"),
            Component.translatable("excavator.crusher"),
            Component.translatable("excavator.walling"),
            Component.translatable("excavator.veinminer"),
            Component.translatable("excavator.silktouch")
    };

    public ExcavatorScreen(ExcavatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 242;
        this.imageHeight = 204;
        this.inventoryLabelX = 41;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        for (int i = 0; i < 5; i++) {
            if (isHovering(6 + i * 24, 42, 20, 40, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, List.of(TOOLTIPS[i]), mouseX, mouseY);
            }
        }
        if (isHovering(220, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.energy", this.menu.power(), this.menu.maxPower()),
                    Component.translatable("tooltip.reinhardtshbm.consumption", this.menu.consumption()),
                    Component.translatable("tooltip.reinhardtshbm.excavator.depth", this.menu.targetDepth())
            ), mouseX, mouseY);
        }
        if (isHovering(202, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 5; i++) {
                if (isHovering(6 + i * 24, 42, 20, 40, mouseX, mouseY)) {
                    sendButton(i);
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 242, 96);
        guiGraphics.blit(TEXTURE, this.leftPos + 33, this.topPos + 104, 33, 104, 176, 100);

        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 220, this.topPos + 70 - power, 229, 156 - power, 16, power);
        }
        if (this.menu.power() > this.menu.consumption()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 224, this.topPos + 4, 239, 156, 9, 12);
        }
        boolean blink = System.currentTimeMillis() % 1000L < 500L;
        if (!this.menu.hasInstalledDrill() && blink) {
            guiGraphics.blit(TEXTURE, this.leftPos + 171, this.topPos + 74, 209, 154, 18, 18);
        }
        for (int i = 0; i < 5; i++) {
            if (this.menu.toggleState(i)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 6 + i * 24, this.topPos + 42, 209, 114, 20, 40);
                if (statusOk(i)) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 11 + i * 24, this.topPos + 5, 209, 104, 10, 10);
                } else if (blink) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 11 + i * 24, this.topPos + 5, 219, 104, 10, 10);
                }
            }
        }
        drawFluid(guiGraphics, 202, 70, 16, this.menu.fluidScaled(52), this.menu.fluid());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + tileY, 0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HbmSoundEvents.LEVER_LARGE.get(), 1.0F));
        }
    }

    private boolean statusOk(int toggle) {
        return switch (toggle) {
            case 0 -> this.menu.hasInstalledDrill() && this.menu.power() >= this.menu.consumption();
            case 3 -> this.menu.canVeinMine();
            case 4 -> this.menu.canSilkTouch();
            default -> true;
        };
    }
}
