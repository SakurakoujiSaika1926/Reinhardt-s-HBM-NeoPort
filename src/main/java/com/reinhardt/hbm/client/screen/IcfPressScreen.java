package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.IcfPressBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.IcfPressMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class IcfPressScreen extends AbstractContainerScreen<IcfPressMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/processing/gui_icf_press.png");

    public IcfPressScreen(IcfPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 179;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        for (int tank = 0; tank < 2; tank++) {
            int x = tank == 0 ? 44 : 152;
            if (isHovering(x, 18, 16, 52, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                        this.menu.fluid(tank),
                        this.menu.fluidAmount(tank),
                        IcfPressBlockEntity.TANK_CAPACITY,
                        this.menu.fluidPressure(tank)
                ), mouseX, mouseY);
                return;
            }
        }
        if (!this.menu.getSlot(IcfPressBlockEntity.LEFT_SOLID_FUEL_SLOT).hasItem()
                && isHovering(62, 54, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.machine_icf_press.input_top_bottom"
            ).withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
        if (!this.menu.getSlot(IcfPressBlockEntity.RIGHT_SOLID_FUEL_SLOT).hasItem()
                && isHovering(134, 54, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.machine_icf_press.input_sides"
            ).withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int muonPixels = Math.min(52, this.menu.muon() * 52 / IcfPressBlockEntity.MAX_MUON);
        if (muonPixels > 0) {
            graphics.blit(TEXTURE, this.leftPos + 28, this.topPos + 70 - muonPixels,
                    176, 52 - muonPixels, 4, muonPixels);
        }
        drawFluid(graphics, 44, 70, 16, this.menu.fluidScaled(0, 52), this.menu.fluid(0));
        drawFluid(graphics, 152, 70, 16, this.menu.fluidScaled(1, 52), this.menu.fluid(1));
    }

    private void drawFluid(
            GuiGraphics graphics,
            int x,
            int bottomY,
            int width,
            int height,
            HbmFluidDefinition fluid
    ) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height,
                    this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + tileY,
                    0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
