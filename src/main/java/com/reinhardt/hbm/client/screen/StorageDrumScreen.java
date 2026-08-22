package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.StorageDrumMenu;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUIStorageDrum port using the original 176x234 background and tank positions. */
public final class StorageDrumScreen extends AbstractContainerScreen<StorageDrumMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_drum.png");
    private static final HbmFluidDefinition WASTE_FLUID = HbmFluids.byName("wastefluid").orElse(HbmFluids.none());
    private static final HbmFluidDefinition WASTE_GAS = HbmFluids.byName("wastegas").orElse(HbmFluids.none());

    public StorageDrumScreen(StorageDrumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 234;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(16, 23, 9, 108, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(WASTE_FLUID, menu.liquidAmount(), menu.liquidCapacity(), 0), mouseX, mouseY);
        }
        if (isHovering(151, 23, 9, 108, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(WASTE_GAS, menu.gasAmount(), menu.gasCapacity(), 0), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawFluid(graphics, WASTE_FLUID, 17, 130, 7, scaled(menu.liquidAmount(), menu.liquidCapacity()));
        drawFluid(graphics, WASTE_GAS, 152, 130, 7, scaled(menu.gasAmount(), menu.gasCapacity()));
    }

    private int scaled(int amount, int capacity) {
        return capacity <= 0 ? 0 : Math.min(106, amount * 106 / capacity);
    }

    private void drawFluid(GuiGraphics graphics, HbmFluidDefinition fluid, int x, int bottom, int width, int height) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottom - height, this.leftPos + x + width, this.topPos + bottom, 0xFF000000 | fluid.color());
            return;
        }
        int color = fluid.color();
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        for (int y = 0; y < height; y += 16) {
            int tileHeight = Math.min(16, height - y);
            graphics.blit(texture, this.leftPos + x, this.topPos + bottom - height + y, 0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
