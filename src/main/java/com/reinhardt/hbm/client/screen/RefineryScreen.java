package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.RefineryMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class RefineryScreen extends AbstractContainerScreen<RefineryMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_refinery.png");
    private static final int TEX_W = 350;
    private static final int TEX_H = 256;

    public RefineryScreen(RefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 210;
        this.imageHeight = 231;
        this.inventoryLabelY = this.imageHeight - 96 + 4;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - 17 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(30, 27, 21, 104, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity()), mouseX, mouseY);
        }
        for (int index = 0; index < 4; index++) {
            int x = 86 + index * 20;
            if (isHovering(x, 42, 16, 52, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.outputFluid(index), this.menu.outputAmount(index), this.menu.outputCapacity(index)), mouseX, mouseY);
            }
        }
        if (isHovering(186, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    this.menu.maxPower()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerPixels = this.menu.powerScaled(50);
        if (powerPixels > 0) {
            blit(guiGraphics, this.leftPos + 186, this.topPos + 69 - powerPixels, 210, 52 - powerPixels, 16, powerPixels);
        }

        drawFluid(guiGraphics, 33, 130, 16, this.menu.inputScaled(101), this.menu.inputFluid());
        drawRecipePipes(guiGraphics);
        for (int index = 0; index < 4; index++) {
            drawFluid(guiGraphics, 86 + index * 20, 95, 16, this.menu.outputScaled(index, 52), this.menu.outputFluid(index));
        }
    }

    private void drawRecipePipes(GuiGraphics guiGraphics) {
        HbmFluidDefinition heavy = this.menu.outputFluid(0);
        HbmFluidDefinition naphtha = this.menu.outputFluid(1);
        HbmFluidDefinition light = this.menu.outputFluid(2);
        HbmFluidDefinition gas = this.menu.outputFluid(3);

        drawTinted(guiGraphics, heavy, 52, 63, 247, 1, 33, 48);
        drawTinted(guiGraphics, naphtha, 52, 32, 247, 50, 66, 52);
        drawTinted(guiGraphics, light, 52, 24, 247, 145, 86, 35);
        drawTinted(guiGraphics, gas, 36, 16, 211, 119, 122, 25);
    }

    private void drawTinted(GuiGraphics guiGraphics, HbmFluidDefinition fluid, int x, int y, int u, int v, int width, int height) {
        if (fluid.isNone()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
            float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
            float blue = (fluid.color() & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, 1.0F);
        }
        blit(guiGraphics, this.leftPos + x, this.topPos + y, u, v, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}
