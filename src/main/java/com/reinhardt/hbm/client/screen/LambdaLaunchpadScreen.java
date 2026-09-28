package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LambdaLaunchpadBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.LambdaLaunchpadMenu;
import com.reinhardt.hbm.network.LambdaLaunchpadControlPayload;
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

/** Literal layout of GUILaunchpadLambda. */
public final class LambdaLaunchpadScreen extends AbstractContainerScreen<LambdaLaunchpadMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/gui_launchpad_lambda.png");

    public LambdaLaunchpadScreen(LambdaLaunchpadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 226;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = this.menu.energyScaled(52);
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 89, this.topPos + 78 - power,
                    194, 52 - power, 16, power);
        }

        int autoOffset = this.menu.autolaunch() ? 18 : 0;
        graphics.blit(TEXTURE, this.leftPos + 34 + autoOffset, this.topPos + 43,
                210 + autoOffset, 26, 18, 18);

        graphics.blit(TEXTURE, this.leftPos + 112, this.topPos + 13,
                this.menu.hasJetFuel() ? 210 : 216, 0, 6, 8);
        graphics.blit(TEXTURE, this.leftPos + 130, this.topPos + 13,
                this.menu.hasOxidizer() ? 210 : 216, 0, 6, 8);
        graphics.blit(TEXTURE, this.leftPos + 94, this.topPos + 13,
                this.menu.energy() >= LambdaLaunchpadBlockEntity.CONSUMPTION ? 210 : 216,
                0, 6, 8);

        if (this.menu.countdown() > 0) {
            graphics.blit(TEXTURE, this.leftPos + 43, this.topPos + 70, 210, 44, 18, 18);
        }

        drawFluid(graphics, 107, 78, 16, this.menu.gasolineScaled(52), gasoline());
        drawFluid(graphics, 125, 78, 16, this.menu.peroxideScaled(52), peroxide());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        String status;
        int color;
        if (this.menu.erected() && this.menu.countdown() > 0) {
            int countdown = this.menu.countdown();
            String seconds = Integer.toString(countdown / 20);
            String cents = Integer.toString((countdown % 20) * 5);
            if (seconds.length() == 1) seconds = "0" + seconds;
            if (cents.length() == 1) cents += "0";
            status = seconds + ":" + cents;
            color = 0xFF0000;
        } else if (!this.menu.hasRocket()) {
            status = Component.translatable("desc.gui.soyuz.idle").getString();
            color = 0xFF0000;
        } else if (!this.menu.erected()) {
            status = Component.translatable("desc.gui.soyuz.loading").getString();
            color = 0xFF8000;
        } else {
            status = Component.translatable("desc.gui.soyuz.ready").getString();
            color = 0x00FF00;
        }
        int width = this.font.width(status);
        float scale = width > 44 ? 44.0F / width : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(53.0F, 107.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, status, -width / 2, 0, color, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(107, 26, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    HbmFluidTooltip.forTank(gasoline(), this.menu.gasolineAmount(), this.menu.gasolineCapacity()),
                    mouseX, mouseY);
        }
        if (isHovering(125, 26, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    HbmFluidTooltip.forTank(peroxide(), this.menu.peroxideAmount(), this.menu.peroxideCapacity()),
                    mouseX, mouseY);
        }
        if (isHovering(89, 26, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), LambdaLaunchpadBlockEntity.MAX_POWER)),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, 34, 43)) {
            send(LambdaLaunchpadControlPayload.MANUAL);
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, 52, 43)) {
            send(LambdaLaunchpadControlPayload.AUTO);
            return true;
        }
        if (button == 0 && this.menu.erected() && inside(mouseX, mouseY, 43, 70)) {
            send(LambdaLaunchpadControlPayload.LAUNCH);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new LambdaLaunchpadControlPayload(this.menu.blockPos(), action));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + 18
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + 18;
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY,
                           int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) return;
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height,
                    this.leftPos + x + width, this.topPos + bottomY,
                    0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + tileY,
                    0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static HbmFluidDefinition gasoline() {
        return HbmFluids.byName("gasoline_leaded").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition peroxide() {
        return HbmFluids.byName("peroxide").orElse(HbmFluids.none());
    }
}
