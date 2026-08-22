package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.blockentity.HydrotreaterBlockEntity;
import com.reinhardt.hbm.menu.HydrotreaterMenu;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class HydrotreaterScreen extends AbstractContainerScreen<HydrotreaterMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_hydrotreater.png");
    private static final int TANK_BOTTOM_Y = 70;
    private static final int TANK_TOP_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    public HydrotreaterScreen(HydrotreaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 238;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        titleLabelY = 5;
        inventoryLabelX = 8;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!renderMachineTooltip(graphics, mouseX, mouseY)) {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private boolean renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(17, TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", menu.power(), menu.maxPower())), mouseX, mouseY);
            return true;
        }
        int[] tankX = {35, 53, 125, 143};
        for (int index = 0; index < tankX.length; index++) {
            if (isHovering(tankX[index], TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
                graphics.renderComponentTooltip(font,
                        HbmFluidTooltip.forTank(menu.tankFluid(index), menu.tankAmount(index), menu.tankCapacity(index)),
                        mouseX, mouseY);
                return true;
            }
        }
        if (menu.getCarried().isEmpty()
                && !menu.getSlot(HydrotreaterBlockEntity.CATALYST_SLOT).hasItem()
                && isHovering(89, 36, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, new net.minecraft.world.item.ItemStack(HbmItems.CATALYTIC_CONVERTER.get()), mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int powerPixels = menu.powerScaled(TANK_HEIGHT);
        if (powerPixels > 0) {
            graphics.blit(TEXTURE, leftPos + 17, topPos + TANK_BOTTOM_Y - powerPixels,
                    176, TANK_HEIGHT - powerPixels, TANK_WIDTH, powerPixels);
        }
        drawFluid(graphics, 35, menu.tankScaled(0, TANK_HEIGHT), menu.tankFluid(0));
        drawFluid(graphics, 53, menu.tankScaled(1, TANK_HEIGHT), menu.tankFluid(1));
        drawFluid(graphics, 125, menu.tankScaled(2, TANK_HEIGHT), menu.tankFluid(2));
        drawFluid(graphics, 143, menu.tankScaled(3, TANK_HEIGHT), menu.tankFluid(3));
    }

    private void drawFluid(GuiGraphics graphics, int x, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (minecraft == null || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(leftPos + x, topPos + TANK_BOTTOM_Y - height,
                    leftPos + x + TANK_WIDTH, topPos + TANK_BOTTOM_Y, color);
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = TANK_BOTTOM_Y - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, leftPos + x, topPos + top + tileY, 0, 16 - tileHeight,
                    TANK_WIDTH, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
