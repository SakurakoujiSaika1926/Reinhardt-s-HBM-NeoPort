package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.PyroOvenMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Pixel-for-pixel slot layout of GUIPyroOven from the 1.7.10 client. */
public final class PyroOvenScreen extends AbstractContainerScreen<PyroOvenMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_pyrooven.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public PyroOvenScreen(PyroOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
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
        if (isHovering(8, 18, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.inputFluid(), this.menu.inputAmount(), 24_000), mouseX, mouseY);
        }
        if (isHovering(116, 18, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.outputFluid(), this.menu.outputAmount(), 24_000), mouseX, mouseY);
        }
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, java.util.List.of(Component.literal(this.menu.energy() + " / 10000000 HE")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(graphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energy = this.menu.energyScaled(52);
        if (energy > 0) {
            blit(graphics, this.leftPos + 152, this.topPos + 70 - energy, 176, 64 - energy, 16, energy);
        }
        int progress = this.menu.progressScaled(27);
        if (progress > 0) {
            blit(graphics, this.leftPos + 57, this.topPos + 47, 176, 0, progress, 12);
        }
        drawFluid(graphics, 8, 70, this.menu.inputScaled(52), this.menu.inputFluid());
        drawFluid(graphics, 116, 70, this.menu.outputScaled(52), this.menu.outputFluid());
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottom, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottom - height, this.leftPos + x + 16, this.topPos + bottom,
                    0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottom - height;
        for (int y = 0; y < height; y += 16) {
            int drawHeight = Math.min(16, height - y);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + y, 0, 16 - drawHeight, 16, drawHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
