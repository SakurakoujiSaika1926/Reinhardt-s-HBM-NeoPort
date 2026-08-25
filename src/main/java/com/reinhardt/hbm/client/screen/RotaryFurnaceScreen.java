package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.menu.RotaryFurnaceMenu;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class RotaryFurnaceScreen extends AbstractContainerScreen<RotaryFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_rotary_furnace.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public RotaryFurnaceScreen(RotaryFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
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

        if (isHovering(8, 36, 52, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.additiveFluid(), this.menu.additiveAmount(), this.menu.additiveCapacity()), mouseX, mouseY);
        }
        if (isHovering(134, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(HbmFluids.byName("steam").orElse(HbmFluids.none()), this.menu.steamAmount(), this.menu.steamCapacity()), mouseX, mouseY);
        }
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), this.menu.spentSteamAmount(), this.menu.spentSteamCapacity()), mouseX, mouseY);
        }
        if (isHovering(98, 18, 16, 52, mouseX, mouseY)) {
            FoundryMaterial material = this.menu.outputMaterial();
            Component name = material == null
                    ? Component.translatable("tooltip.reinhardtshbm.rotary_furnace.empty")
                    : Component.translatable(material.translationKey());
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    name,
                    Component.literal(format(this.menu.outputAmount()) + " / " + format(com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity.MAX_OUTPUT) + " quanta")
            ), mouseX, mouseY);
        }
        if (isHovering(44, 54, 16, 16, mouseX, mouseY) && !this.menu.getSlot(4).hasItem()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.rotary_furnace.burn_time"),
                    Component.translatable("tooltip.reinhardtshbm.rotary_furnace.coke_time"),
                    Component.translatable("tooltip.reinhardtshbm.rotary_furnace.burn_heat"),
                    Component.translatable("tooltip.reinhardtshbm.rotary_furnace.solid_heat"),
                    Component.translatable("tooltip.reinhardtshbm.rotary_furnace.rocket_heat"),
                    Component.translatable("tooltip.reinhardtshbm.rotary_furnace.balefire_heat")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        int progress = this.menu.progressScaled(33);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 63, this.topPos + 30, 176, 0, progress, 10, TEX_W, TEX_H);
        }

        int burn = this.menu.burnScaled(14);
        if (burn > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 69 - burn, 176, 24 - burn, 14, burn, TEX_W, TEX_H);
        }

        drawFluidHorizontal(guiGraphics, 8, 52, this.menu.additiveScaled(52), 16, this.menu.additiveFluid());
        drawFluid(guiGraphics, 134, 70, 16, this.menu.steamScaled(52), HbmFluids.byName("steam").orElse(HbmFluids.none()));
        drawFluid(guiGraphics, 152, 70, 16, this.menu.spentSteamScaled(52), HbmFluids.byName("spentsteam").orElse(HbmFluids.none()));

        FoundryMaterial material = this.menu.outputMaterial();
        int output = this.menu.outputScaled(52);
        if (material != null && output > 0) {
            int color = 0xFF000000 | material.moltenColor();
            guiGraphics.fill(this.leftPos + 98, this.topPos + 70 - output, this.leftPos + 114, this.topPos + 70, color);
            guiGraphics.fill(this.leftPos + 98, this.topPos + 70 - output, this.leftPos + 114, this.topPos + 71 - output, 0x66FFFFFF);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private void drawFluidHorizontal(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (width <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private static List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }

    private static String format(int value) {
        return String.format(Locale.US, "%,d", value);
    }
}
