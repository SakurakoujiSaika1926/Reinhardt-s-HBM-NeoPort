package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.WatzMenu;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class WatzScreen extends AbstractContainerScreen<WatzMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_watz.png");
    private static final int LOCK_X = 142;
    private static final int LOCK_Y = 70;
    private static final int LOCK_SIZE = 18;

    public WatzScreen(WatzMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 229;
        this.inventoryLabelY = this.imageHeight - 93;
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

        if (isHovering(13, 100, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(String.format(Locale.US, "%,d TU", this.menu.heat()))), mouseX, mouseY);
        }
        if (isHovering(143, 71, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    this.menu.locked() ? "tooltip.reinhardtshbm.watz.unlock" : "tooltip.reinhardtshbm.watz.lock"
            )), mouseX, mouseY);
        }
        for (int tank = 0; tank < 3; tank++) {
            if (isHovering(142 + tank * 6, 23, 6, 45, mouseX, mouseY)) {
                HbmFluidDefinition fluid = tankFluid(tank);
                guiGraphics.renderComponentTooltip(
                        this.font,
                        HbmFluidTooltip.forTank(fluid, this.menu.tankAmount(tank), this.menu.tankCapacity(tank)),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        float heatColor = Mth.clamp(1.0F - (float) Math.log(this.menu.heat() / 100_000.0D + 1.0D) * 0.4F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, heatColor, heatColor, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 131, 122);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos + 131, this.topPos, 131, 0, 36, 122);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos + 130, 0, 130, this.imageWidth, 99);
        guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 31, 176, 31, 9, 60);
        guiGraphics.blit(TEXTURE, this.leftPos + 105, this.topPos + 96, 185, 26, 30, 26);
        guiGraphics.blit(TEXTURE, this.leftPos + 9, this.topPos + 96, 184, 0, 26, 26);

        if (this.menu.on()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 147, this.topPos + 8, 176, 0, 8, 8);
        }
        if (this.menu.locked()) {
            guiGraphics.blit(TEXTURE, this.leftPos + LOCK_X, this.topPos + LOCK_Y, 210, 0, LOCK_SIZE, LOCK_SIZE);
        }

        drawHeatGauge(guiGraphics, 13, 100, 1.0F - heatColor);
        for (int tank = 0; tank < 3; tank++) {
            drawFluid(guiGraphics, 143 + tank * 6, 69, 4, this.menu.tankScaled(tank, 43), tankFluid(tank));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 93, 4210752, false);
        String flux = String.format(Locale.US, "%,.1f", (double) this.menu.flux());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8F, 0.8F, 1.0F);
        guiGraphics.drawString(this.font, flux, Math.round((161.0F - this.font.width(flux) * 0.8F) / 0.8F), Math.round(107.0F / 0.8F), 0x00FF00, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && this.leftPos + LOCK_X <= mouseX
                && this.leftPos + LOCK_X + LOCK_SIZE > mouseX
                && this.topPos + LOCK_Y < mouseY
                && this.topPos + LOCK_Y + LOCK_SIZE >= mouseY) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private HbmFluidDefinition tankFluid(int tank) {
        return HbmFluids.byOldId(this.menu.tankFluidOldId(tank)).orElse(HbmFluids.none());
    }

    private void drawHeatGauge(GuiGraphics graphics, int x, int y, float fill) {
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 184, 0, 18, 18);
        int pixels = Mth.clamp(Math.round(fill * 16.0F), 0, 16);
        if (pixels > 0) {
            graphics.fill(this.leftPos + x + 1, this.topPos + y + 17 - pixels, this.leftPos + x + 17, this.topPos + y + 17, 0x88FF4A18);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        graphics.blit(texture, this.leftPos + x, this.topPos + bottomY - height, 0, 16 - height, width, height, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
