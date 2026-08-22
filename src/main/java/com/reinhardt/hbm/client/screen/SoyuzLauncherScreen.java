package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.SoyuzLauncherMenu;
import com.reinhardt.hbm.network.SoyuzLauncherControlPayload;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class SoyuzLauncherScreen extends AbstractContainerScreen<SoyuzLauncherMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_soyuz.png");
    private static final int SAT_MODE_X = 88;
    private static final int SAT_MODE_Y = 17;
    private static final int CARGO_MODE_X = 88;
    private static final int CARGO_MODE_Y = 35;
    private static final int START_X = 151;
    private static final int START_Y = 17;

    public SoyuzLauncherScreen(SoyuzLauncherMenu menu, Inventory playerInventory, Component title) {
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(8, 36, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(kerosene(), this.menu.keroseneAmount(), this.menu.keroseneCapacity()), mouseX, mouseY);
        }
        if (isHovering(26, 36, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(oxygen(), this.menu.oxygenAmount(), this.menu.oxygenCapacity()), mouseX, mouseY);
        }
        if (isHovering(49, 72, 6, 34, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    SoyuzLauncherBlockEntity.MAX_POWER
            )), mouseX, mouseY);
        }
        if (isHovering(43, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.soyuz.rocket")), mouseX, mouseY);
        }
        if (isHovering(43, 35, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.soyuz.designator")), mouseX, mouseY);
        }
        if (isHovering(133, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.soyuz.satellite")), mouseX, mouseY);
        }
        if (isHovering(133, 35, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.soyuz.module")), mouseX, mouseY);
        }
        if (isHovering(SAT_MODE_X, SAT_MODE_Y, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.soyuz.mode_satellite")), mouseX, mouseY);
        }
        if (isHovering(CARGO_MODE_X, CARGO_MODE_Y, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.soyuz.mode_cargo")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        int countdown = Math.max(0, this.menu.countdown());
        String seconds = Integer.toString(countdown / 20);
        String cents = Integer.toString((countdown % 20) * 5);
        if (seconds.length() == 1) {
            seconds = "0" + seconds;
        }
        if (cents.length() == 1) {
            cents += "0";
        }
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(this.font, seconds + ":" + cents, 307, 75, 0xFF0000, false);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerPixels = this.menu.energyScaled(34);
        if (powerPixels > 0) {
            graphics.blit(TEXTURE, this.leftPos + 49, this.topPos + 106 - powerPixels, 194, 52 - powerPixels, 6, powerPixels);
        }

        graphics.blit(TEXTURE, this.leftPos + 61, this.topPos + 17, 176 + (this.menu.hasRocket() ? 18 : 0), 0, 18, 18);
        int designator = this.menu.designatorState();
        if (designator > 0) {
            graphics.blit(TEXTURE, this.leftPos + 61, this.topPos + 35, 176 + (designator - 1) * 18, 0, 18, 18);
        }

        int mode = this.menu.mode();
        graphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 17 + mode * 18, 176, 18 + mode * 18, 18, 18);

        int orbital = this.menu.orbitalState();
        if (orbital > 0) {
            graphics.blit(TEXTURE, this.leftPos + 115, this.topPos + 35, 176 + (orbital - 1) * 18, 0, 18, 18);
        }

        int satellite = this.menu.satelliteState();
        if (satellite > 0) {
            graphics.blit(TEXTURE, this.leftPos + 115, this.topPos + 17, 176 + (satellite - 1) * 18, 0, 18, 18);
        }

        if (this.menu.starting()) {
            graphics.blit(TEXTURE, this.leftPos + START_X, this.topPos + START_Y, 176, 54, 18, 18);
        }

        graphics.blit(TEXTURE, this.leftPos + 13, this.topPos + 23, this.menu.hasFuel() ? 212 : 218, 0, 6, 8);
        graphics.blit(TEXTURE, this.leftPos + 31, this.topPos + 23, this.menu.hasOxygen() ? 212 : 218, 0, 6, 8);
        graphics.blit(TEXTURE, this.leftPos + 49, this.topPos + 59, this.menu.hasPower() ? 212 : 218, 0, 6, 8);

        drawFluid(graphics, 8, 88, 16, this.menu.keroseneScaled(52), kerosene());
        drawFluid(graphics, 26, 88, 16, this.menu.oxygenScaled(52), oxygen());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inButton(mouseX, mouseY, SAT_MODE_X, SAT_MODE_Y)) {
            PacketDistributor.sendToServer(new SoyuzLauncherControlPayload(this.menu.blockPos(), SoyuzLauncherControlPayload.BUTTON_MODE, 0));
            playClick();
            return true;
        }
        if (button == 0 && inButton(mouseX, mouseY, CARGO_MODE_X, CARGO_MODE_Y)) {
            PacketDistributor.sendToServer(new SoyuzLauncherControlPayload(this.menu.blockPos(), SoyuzLauncherControlPayload.BUTTON_MODE, 1));
            playClick();
            return true;
        }
        if (button == 0 && inButton(mouseX, mouseY, START_X, START_Y)) {
            PacketDistributor.sendToServer(new SoyuzLauncherControlPayload(this.menu.blockPos(), SoyuzLauncherControlPayload.BUTTON_START, 0));
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inButton(double mouseX, double mouseY, int x, int y) {
        return this.leftPos + x <= mouseX && this.leftPos + x + 18 > mouseX
                && this.topPos + y < mouseY && this.topPos + y + 18 >= mouseY;
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
        int top = bottomY - height;
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            for (int tileY = 0; tileY < height; tileY += 16) {
                int tileHeight = Math.min(16, height - tileY);
                graphics.blit(texture, this.leftPos + x + tileX, this.topPos + top + tileY, 0, 16 - tileHeight, tileWidth, tileHeight, 16, 16);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static HbmFluidDefinition kerosene() {
        return HbmFluids.byName("kerosene").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition oxygen() {
        return HbmFluids.byName("oxygen").orElse(HbmFluids.none());
    }
}
