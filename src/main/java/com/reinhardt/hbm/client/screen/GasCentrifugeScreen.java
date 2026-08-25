package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.GasCentrifugeBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.GasCentrifugeMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class GasCentrifugeScreen extends AbstractContainerScreen<GasCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_centrifuge_gas.png");
    private static final ResourceLocation UTILITY = ReinhardtsHBM.id("textures/gui/gui_utility.png");

    public GasCentrifugeScreen(GasCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 206;
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

        if (isHovering(182, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    GasCentrifugeBlockEntity.MAX_POWER
            )), mouseX, mouseY);
        }
        if (isHovering(70, 35, 36, 13, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    this.menu.processTime()
            )), mouseX, mouseY);
        }
        if (isHovering(15, 15, 24, 55, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.inputFluid(),
                    this.menu.inputAmount(),
                    GasCentrifugeBlockEntity.TANK_CAPACITY
            ), mouseX, mouseY);
        }
        if (isHovering(137, 15, 25, 55, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.outputFluid(),
                    this.menu.outputAmount(),
                    GasCentrifugeBlockEntity.TANK_CAPACITY
            ), mouseX, mouseY);
        }
        if (isHovering(91, 15, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.inputFluid(),
                    this.menu.inputAmount(),
                    GasCentrifugeBlockEntity.TANK_CAPACITY
            ), mouseX, mouseY);
        }
        if (isHovering(-12, 16, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.gasCent.enrichment", ChatFormatting.GREEN),
                    this.leftPos - 8, this.topPos + 32);
        }
        if (isHovering(-12, 32, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.gasCent.output", ChatFormatting.GOLD),
                    this.leftPos - 8, this.topPos + 48);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerPixels = this.menu.energyScaled(52);
        if (powerPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 182, this.topPos + 69 - powerPixels, 206, 52 - powerPixels, 16, powerPixels);
        }

        int progressPixels = this.menu.progressScaled(36);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 70, this.topPos + 35, 206, 52, progressPixels, 13);
        }

        drawFluid(guiGraphics, 16, 68, 6, this.menu.inputScaled(52), this.menu.inputFluid());
        drawFluid(guiGraphics, 32, 68, 6, this.menu.inputScaled(52), this.menu.inputFluid());
        drawFluid(guiGraphics, 138, 68, 6, this.menu.outputScaled(52), this.menu.outputFluid());
        drawFluid(guiGraphics, 154, 68, 6, this.menu.outputScaled(52), this.menu.outputFluid());
        drawInfoPanel(guiGraphics, -12, 16, 3);
        drawInfoPanel(guiGraphics, -12, 32, 2);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0xFF404040, false);
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
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

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int type) {
        int sourceX = switch (type) {
            case 3, 7, 11 -> 24;
            case 2, 6, 10 -> 8;
            default -> 0;
        };
        int sourceY = switch (type) {
            case 2, 3 -> 0;
            case 6, 7 -> 16;
            case 10, 11 -> 32;
            default -> 0;
        };
        graphics.blit(UTILITY, this.leftPos + x, this.topPos + y, sourceX, sourceY, 16, 16, 256, 256);
    }

    private List<Component> tooltipLines(String key, ChatFormatting headingColor) {
        String[] lines = Component.translatable(key).getString().split("\\$", -1);
        List<Component> result = new ArrayList<>(lines.length);
        for (int index = 0; index < lines.length; index++) {
            result.add(index == 0
                    ? Component.literal(lines[index]).withStyle(headingColor)
                    : Component.literal(lines[index]));
        }
        return result;
    }
}
