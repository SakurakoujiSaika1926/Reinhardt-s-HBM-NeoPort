package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.menu.ElectrolyzerMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {
    private static final ResourceLocation FLUID_TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_electrolyser_fluid.png");
    private static final ResourceLocation METAL_TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_electrolyser_metal.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 210;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, (this.imageWidth / 2 - this.font.width(this.title) / 2) - 16, 7, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (this.menu.isFluidMode()) {
            tankTooltip(guiGraphics, mouseX, mouseY, 42, 18, this.menu.inputFluid(), this.menu.inputAmount());
            tankTooltip(guiGraphics, mouseX, mouseY, 96, 18, this.menu.outputFluid1(), this.menu.outputAmount1());
            tankTooltip(guiGraphics, mouseX, mouseY, 116, 18, this.menu.outputFluid2(), this.menu.outputAmount2());
        } else {
            tankTooltip(guiGraphics, mouseX, mouseY, 36, 18, this.menu.acidFluid(), this.menu.acidAmount());
            materialTooltip(guiGraphics, mouseX, mouseY, 58, 18, this.menu.leftMaterial(), this.menu.leftAmount());
            materialTooltip(guiGraphics, mouseX, mouseY, 96, 18, this.menu.rightMaterial(), this.menu.rightAmount());
        }
        if (isHovering(186, 18, 16, 89, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    Integer.toUnsignedLong(this.menu.energy()),
                    ElectrolyzerBlockEntity.MAX_POWER
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isFluidMode()) {
            drawFluid(guiGraphics, 42, 70, 16, this.menu.tankScaled(this.menu.inputAmount(), 52), this.menu.inputFluid());
            drawFluid(guiGraphics, 96, 70, 16, this.menu.tankScaled(this.menu.outputAmount1(), 52), this.menu.outputFluid1());
            drawFluid(guiGraphics, 116, 70, 16, this.menu.tankScaled(this.menu.outputAmount2(), 52), this.menu.outputFluid2());

            int fluidProgress = this.menu.fluidProgressScaled(41);
            if (fluidProgress > 0) {
                guiGraphics.blit(texture(), this.leftPos + 62, this.topPos + 26, 226, 0, 12, fluidProgress, TEX_W, TEX_H);
            }
        } else {
            drawFluid(guiGraphics, 36, 70, 16, this.menu.tankScaled(this.menu.acidAmount(), 52), this.menu.acidFluid());
            drawMaterial(guiGraphics, 58, 60, 34, this.menu.leftMaterial(), this.menu.leftAmount());
            drawMaterial(guiGraphics, 96, 60, 34, this.menu.rightMaterial(), this.menu.rightAmount());
            int oreProgress = this.menu.oreProgressScaled(26);
            if (oreProgress > 0) {
                guiGraphics.blit(texture(), this.leftPos + 7, this.topPos + 71 - oreProgress, 226, 25 - oreProgress, 22, oreProgress, TEX_W, TEX_H);
            }
        }
        int energy = this.menu.energyScaled(89);
        if (energy > 0) {
            guiGraphics.blit(texture(), this.leftPos + 186, this.topPos + 107 - energy, 210, 89 - energy, 16, energy, TEX_W, TEX_H);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(8, 82, 54, 12, mouseX, mouseY)) {
            int next = this.menu.isFluidMode() ? ElectrolyzerBlockEntity.GUI_METAL : ElectrolyzerBlockEntity.GUI_FLUID;
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, next);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void tankTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, HbmFluidDefinition fluid, int amount) {
        if (isHovering(x, y, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(
                    this.font,
                    HbmFluidTooltip.forTank(fluid, amount, ElectrolyzerBlockEntity.TANK_CAPACITY),
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) return;
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private void drawMaterial(GuiGraphics graphics, int x, int bottomY, int width, FoundryMaterial material, int amount) {
        if (material == null || amount <= 0) {
            return;
        }
        int height = Math.min(42, amount * 42 / ElectrolyzerBlockEntity.MAX_MATERIAL);
        if (height <= 0) {
            return;
        }
        int color = 0xFF000000 | material.moltenColor();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private void materialTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, FoundryMaterial material, int amount) {
        if (!isHovering(x, y, 34, 42, mouseX, mouseY)) {
            return;
        }
        if (material == null || amount <= 0) {
            graphics.renderComponentTooltip(this.font, List.of(Component.literal("Empty").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            return;
        }
        graphics.renderComponentTooltip(
                this.font,
                List.of(Component.translatable(material.translationKey()).withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(formatMaterialAmount(amount))),
                mouseX,
                mouseY
        );
    }

    private static Component formatMaterialAmount(int amount) {
        int remaining = amount;
        int blocks = remaining / FoundryShape.BLOCK.quanta();
        remaining -= blocks * FoundryShape.BLOCK.quanta();
        int ingots = remaining / FoundryShape.INGOT.quanta();
        remaining -= ingots * FoundryShape.INGOT.quanta();
        int nuggets = remaining / FoundryShape.NUGGET.quanta();
        remaining -= nuggets * FoundryShape.NUGGET.quanta();

        java.util.ArrayList<Component> parts = new java.util.ArrayList<>();
        if (blocks > 0) parts.add(Component.translatable(blocks == 1 ? "matshape.reinhardtshbm.block" : "matshape.reinhardtshbm.blocks", blocks));
        if (ingots > 0) parts.add(Component.translatable(ingots == 1 ? "matshape.reinhardtshbm.ingot" : "matshape.reinhardtshbm.ingots", ingots));
        if (nuggets > 0) parts.add(Component.translatable(nuggets == 1 ? "matshape.reinhardtshbm.nugget" : "matshape.reinhardtshbm.nuggets", nuggets));
        if (remaining > 0) parts.add(Component.translatable(remaining == 1 ? "matshape.reinhardtshbm.quantum" : "matshape.reinhardtshbm.quanta", remaining));
        if (parts.isEmpty()) {
            return Component.literal("0");
        }
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(" "));
            }
            result.append(parts.get(i));
        }
        return result;
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture(), x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private ResourceLocation texture() {
        return this.menu.isFluidMode() ? FLUID_TEXTURE : METAL_TEXTURE;
    }
}
