package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SmallBoilerBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.SmallBoilerMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SmallBoilerScreen extends AbstractContainerScreen<SmallBoilerMenu> {
    private static final ResourceLocation BOILER_TEXTURE = ReinhardtsHBM.id("textures/gui/gui_boiler.png");
    private static final ResourceLocation ELECTRIC_TEXTURE = ReinhardtsHBM.id("textures/gui/gui_boiler_electric.png");

    public SmallBoilerScreen(SmallBoilerMenu menu, Inventory playerInventory, Component title) {
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

        if (isHovering(62, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.inputFluid(), this.menu.inputAmount(), this.menu.inputCapacity()), mouseX, mouseY);
        }
        if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.outputFluid(), this.menu.outputAmount(), this.menu.outputCapacity()), mouseX, mouseY);
        }
        if (this.menu.electric() && isHovering(123, 35, 7, 34, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    SmallBoilerBlockEntity.ENERGY_CAPACITY
            )), mouseX, mouseY);
        }
        if (!this.menu.electric() && isHovering(102, 36, 16, 17, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.burn_time_ticks",
                    this.menu.burnTime(),
                    this.menu.burnTimeTotal()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = this.menu.electric() ? ELECTRIC_TEXTURE : BOILER_TEXTURE;
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawFluid(guiGraphics, 62, 69, 16, this.menu.inputScaled(52), this.menu.inputFluid());
        drawFluid(guiGraphics, 134, 69, 16, this.menu.outputScaled(52), this.menu.outputFluid());

        if (this.menu.electric()) {
            int energyPixels = this.menu.powerScaled(34);
            if (energyPixels > 0) {
                guiGraphics.blit(texture, this.leftPos + 123, this.topPos + 69 - energyPixels, 200, 34 - energyPixels, 7, energyPixels);
            }
        } else {
            int burnPixels = this.menu.burnScaled(14);
            if (burnPixels > 0) {
                guiGraphics.blit(texture, this.leftPos + 103, this.topPos + 49 - burnPixels, 176, 14 - burnPixels, 14, burnPixels + 1);
            }
        }
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

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}
