package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.FluidTankMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class FluidTankScreen extends AbstractContainerScreen<FluidTankMenu> {
    private static final ResourceLocation BARREL_TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_barrel.png");
    private static final ResourceLocation TANK_TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_tank.png");
    private static final int TANK_X = 71;
    private static final int TANK_Y = 17;
    private static final int TANK_BOTTOM_Y = 69;
    private static final int TANK_WIDTH = 34;
    private static final int TANK_HEIGHT = 52;
    private static final int MODE_BUTTON_X = 151;
    private static final int MODE_BUTTON_Y = 34;

    public FluidTankScreen(FluidTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
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

        if (isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = this.menu.isBarrel() ? BARREL_TEXTURE : TANK_TEXTURE;
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(
                texture,
                this.leftPos + MODE_BUTTON_X,
                this.topPos + MODE_BUTTON_Y,
                176,
                this.menu.mode().ordinal() * 18,
                18,
                18
        );
        drawFluid(guiGraphics, TANK_X, TANK_BOTTOM_Y, TANK_WIDTH, this.menu.fluidScaled(TANK_HEIGHT), this.menu.fluid());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.leftPos + MODE_BUTTON_X <= mouseX
                && this.leftPos + MODE_BUTTON_X + 18 > mouseX
                && this.topPos + MODE_BUTTON_Y < mouseY
                && this.topPos + MODE_BUTTON_Y + 18 >= mouseY) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(
                    this.leftPos + x,
                    this.topPos + bottomY - height,
                    this.leftPos + x + width,
                    this.topPos + bottomY,
                    color
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

    private List<Component> fluidTooltip() {
        return HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.amount(), this.menu.capacity(), this.menu.pressure());
    }
}
