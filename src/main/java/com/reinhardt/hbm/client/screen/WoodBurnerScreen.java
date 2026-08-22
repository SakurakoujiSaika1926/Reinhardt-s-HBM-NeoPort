package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.WoodBurnerBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.WoodBurnerMenu;
import com.reinhardt.hbm.network.WoodBurnerControlPayload;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class WoodBurnerScreen extends AbstractContainerScreen<WoodBurnerMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/generators/gui_wood_burner_alt.png");
    private static final int TOGGLE_X = 53;
    private static final int TOGGLE_Y = 17;
    private static final int TOGGLE_WIDTH = 16;
    private static final int TOGGLE_HEIGHT = 15;
    private static final int SWITCH_X = 46;
    private static final int SWITCH_Y = 37;
    private static final int SWITCH_WIDTH = 30;
    private static final int SWITCH_HEIGHT = 14;

    public WoodBurnerScreen(WoodBurnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(143, 18, 16, 34, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    WoodBurnerBlockEntity.ENERGY_CAPACITY
            )), mouseX, mouseY);
        }

        if (this.menu.liquidBurn() && isHovering(80, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity()), mouseX, mouseY);
        }

        if (!this.menu.liquidBurn() && isHovering(16, 17, 8, 54, mouseX, mouseY) && this.menu.burnTimeTotal() > 0) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.burn_time",
                    this.menu.burnTime() / 20
            )), mouseX, mouseY);
        }

        if (isHovering(TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(this.menu.isOn() ? "ON" : "OFF")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.liquidBurn()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 16, this.topPos + 17, 176, 52, 60, 54);
            guiGraphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 17, 176, 106, 36, 54);
        }

        if (this.menu.isOn()) {
            guiGraphics.blit(TEXTURE, this.leftPos + TOGGLE_X, this.topPos + TOGGLE_Y, 196, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        }

        int energyPixels = this.menu.energyScaled(34);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 143,
                    this.topPos + 52 - energyPixels,
                    176,
                    52 - energyPixels,
                    16,
                    energyPixels
            );
        }

        if (this.menu.liquidBurn()) {
            drawFluid(guiGraphics, 80, 70, 16, this.menu.fluidScaled(52), this.menu.fluid());
        } else {
            int burnPixels = this.menu.burnScaled(52);
            if (burnPixels > 0) {
                guiGraphics.blit(
                        TEXTURE,
                        this.leftPos + 17,
                        this.topPos + 70 - burnPixels,
                        192,
                        52 - burnPixels,
                        4,
                        burnPixels
                );
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && this.leftPos + TOGGLE_X <= mouseX
                && this.leftPos + TOGGLE_X + TOGGLE_WIDTH > mouseX
                && this.topPos + TOGGLE_Y < mouseY
                && this.topPos + TOGGLE_Y + TOGGLE_HEIGHT >= mouseY) {
            PacketDistributor.sendToServer(new WoodBurnerControlPayload(this.menu.blockPos(), WoodBurnerControlPayload.ACTION_TOGGLE));
            playClick();
            return true;
        }
        if (button == 0
                && this.leftPos + SWITCH_X <= mouseX
                && this.leftPos + SWITCH_X + SWITCH_WIDTH > mouseX
                && this.topPos + SWITCH_Y < mouseY
                && this.topPos + SWITCH_Y + SWITCH_HEIGHT >= mouseY) {
            PacketDistributor.sendToServer(new WoodBurnerControlPayload(this.menu.blockPos(), WoodBurnerControlPayload.ACTION_SWITCH_MODE));
            playClick();
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

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
