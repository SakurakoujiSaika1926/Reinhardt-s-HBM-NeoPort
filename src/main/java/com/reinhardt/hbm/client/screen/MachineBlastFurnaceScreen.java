package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.MachineBlastFurnaceMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachineBlastFurnaceScreen extends AbstractContainerScreen<MachineBlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_blast_furnace_1710.png");

    public MachineBlastFurnaceScreen(MachineBlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Keep the vanilla slot tooltip from being drawn together with a legacy machine tooltip.
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            super.renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        if (isHovering(25, 17, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.flueFluid(),
                    this.menu.flueAmount(),
                    this.menu.flueCapacity()
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(25, 71, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.airblastFluid(),
                    this.menu.airblastAmount(),
                    this.menu.airblastCapacity()
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(79, 62, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.blast_furnace.speed",
                    this.menu.speedPercent()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Keep the legacy order: progress is behind the fuel column, then the fuel column is drawn over it.
        int fuelPixels = this.menu.fuelScaled(26);
        int progressPixels = this.menu.progressScaled(Math.max(0, 88 - fuelPixels));
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 106 - progressPixels - fuelPixels, 176, 102 - progressPixels - fuelPixels, 56, progressPixels);
        }
        if (fuelPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 106 - fuelPixels, 176, 128 - fuelPixels, 56, fuelPixels);
        }

        if (this.menu.isProcessing()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 81, this.topPos + 64, 176, 0, 14, 14);
        }

        drawLegacyGauge(guiGraphics, 34, 26, this.menu.flueAmount(), this.menu.flueCapacity());
        drawLegacyGauge(guiGraphics, 34, 80, this.menu.airblastAmount(), this.menu.airblastCapacity());
    }

    private void drawLegacyGauge(GuiGraphics graphics, int centerX, int centerY, int amount, int capacity) {
        double progress = capacity <= 0 ? 0.0D : Math.max(0.0D, Math.min(1.0D, amount / (double) capacity));
        double angle = Math.toRadians(-progress * 270.0D - 45.0D);
        double[] tip = rotate(0.0D, 5.0D, angle);
        double[] left = rotate(1.0D, -2.0D, angle);
        double[] right = rotate(-1.0D, -2.0D, angle);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawGaugeTriangle(graphics, centerX, centerY, tip, left, right, 1.5D, 0x000000);
        drawGaugeTriangle(graphics, centerX, centerY, tip, left, right, 1.0D, 0x800000);
        RenderSystem.disableBlend();
    }

    private void drawGaugeTriangle(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            double[] tip,
            double[] left,
            double[] right,
            double scale,
            int color
    ) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        var pose = graphics.pose().last();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        addGaugeVertex(buffer, pose, centerX + tip[0] * scale, centerY + tip[1] * scale, red, green, blue);
        addGaugeVertex(buffer, pose, centerX + left[0] * scale, centerY + left[1] * scale, red, green, blue);
        addGaugeVertex(buffer, pose, centerX + right[0] * scale, centerY + right[1] * scale, red, green, blue);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void addGaugeVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, com.mojang.blaze3d.vertex.PoseStack.Pose pose, double x, double y, int red, int green, int blue) {
        buffer.addVertex(pose, (float) (this.leftPos + x), (float) (this.topPos + y), 0.0F)
                .setColor(red, green, blue, 255);
    }

    private static double[] rotate(double x, double y, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new double[]{x * cos - y * sin, x * sin + y * cos};
    }
}
