package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.OrbusMenu;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Direct port of GUIBarrel for Orbus. */
public final class OrbusScreen extends AbstractContainerScreen<OrbusMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_barrel.png");
    private static final int CAPACITY = 512_000;

    public OrbusScreen(OrbusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(151, 34, 18, 18, mouseX, mouseY) && this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(71, 17, 34, 52, mouseX, mouseY)) {
            HbmFluidDefinition fluid = HbmFluids.byOldId(this.menu.fluidId()).orElse(HbmFluids.none());
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(fluid, this.menu.fluidAmount(), CAPACITY), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        graphics.blit(TEXTURE, this.leftPos + 151, this.topPos + 34, 176, this.menu.mode() * 18, 18, 18);
        drawFluid(graphics, HbmFluids.byOldId(this.menu.fluidId()).orElse(HbmFluids.none()), this.menu.fluidAmount());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    private void drawFluid(GuiGraphics graphics, HbmFluidDefinition fluid, int amount) {
        if (fluid.isNone() || amount <= 0) return;
        int height = Math.min(52, amount * 52 / CAPACITY);
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + 71, this.topPos + 69 - height, this.leftPos + 105, this.topPos + 69, 0xFF000000 | fluid.color());
            return;
        }
        int color = fluid.color();
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        // FluidTank#renderTank tiles the 16x16 fluid sheet.  Do not stretch
        // one sample across the full barrel: that is visibly different from
        // the original GUI and breaks at fill heights above 16 pixels.
        int top = 69 - height;
        for (int tileX = 0; tileX < 34; tileX += 16) {
            int tileWidth = Math.min(16, 34 - tileX);
            for (int tileY = 0; tileY < height; tileY += 16) {
                int tileHeight = Math.min(16, height - tileY);
                graphics.blit(texture,
                        this.leftPos + 71 + tileX,
                        this.topPos + top + tileY,
                        0,
                        16 - tileHeight,
                        tileWidth,
                        tileHeight,
                        16,
                        16);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
