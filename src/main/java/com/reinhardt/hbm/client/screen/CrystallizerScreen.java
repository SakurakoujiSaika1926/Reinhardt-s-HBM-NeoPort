package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.CrystallizerMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_crystallizer_alt.png");

    public CrystallizerScreen(CrystallizerMenu menu, Inventory playerInventory, Component title) {
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

        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    com.reinhardt.hbm.blockentity.CrystallizerBlockEntity.MAX_POWER
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(35, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.tankFluid(),
                    this.menu.tankAmount(),
                    this.menu.tankCapacity(),
                    0
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(80, 47, 28, 12, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    this.menu.workTime()
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(117, 22, 8, 8, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.upgrades.acceptable").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("tooltip.reinhardtshbm.upgrades.speed_3").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.reinhardtshbm.upgrades.effect_3").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.reinhardtshbm.upgrades.overdrive_3").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energyPixels = this.menu.energyScaled(52);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 152,
                    this.topPos + 70 - energyPixels,
                    176,
                    64 - energyPixels,
                    16,
                    energyPixels
            );
        }

        int progressPixels = this.menu.progressScaled(28);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, progressPixels, 12);
        }

        guiGraphics.blit(TEXTURE, this.leftPos + 117, this.topPos + 22, 176, 12, 8, 8);
        drawFluid(guiGraphics, 35, 70, 16, this.menu.tankScaled(52), this.menu.tankFluid());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(
                    this.leftPos + x,
                    this.topPos + bottomY - height,
                    this.leftPos + x + width,
                    this.topPos + bottomY,
                    0xFF000000 | fluid.color()
            );
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);

        int top = bottomY - height;
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            for (int tileY = 0; tileY < height; tileY += 16) {
                int tileHeight = Math.min(16, height - tileY);
                graphics.blit(
                        texture,
                        this.leftPos + x + tileX,
                        this.topPos + top + tileY,
                        0,
                        16 - tileHeight,
                        tileWidth,
                        tileHeight,
                        16,
                        16
                );
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
