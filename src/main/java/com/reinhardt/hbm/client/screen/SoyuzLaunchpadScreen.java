package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SoyuzLaunchpadBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.SoyuzLaunchpadMenu;
import com.reinhardt.hbm.network.SoyuzLaunchpadControlPayload;
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

/** Exact 194x244 latest-old Soyuz launch-pad GUI. */
public final class SoyuzLaunchpadScreen extends AbstractContainerScreen<SoyuzLaunchpadMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/gui_launchpad_soyuz.png");

    public SoyuzLaunchpadScreen(SoyuzLaunchpadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 194;
        this.imageHeight = 244;
        this.inventoryLabelX = 17;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 4;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(152, 44, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    HbmFluidTooltip.forTank(kerosene(), this.menu.keroseneAmount(),
                            this.menu.keroseneCapacity()), mouseX, mouseY);
        }
        if (isHovering(170, 44, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    HbmFluidTooltip.forTank(oxygen(), this.menu.oxygenAmount(),
                            this.menu.oxygenCapacity()), mouseX, mouseY);
        }
        if (isHovering(134, 44, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(),
                    SoyuzLaunchpadBlockEntity.MAX_POWER)), mouseX, mouseY);
        }
        if (isHovering(79, 52, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.launchpad_soyuz.cargo")), mouseX, mouseY);
        }
        if (isHovering(97, 52, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.launchpad_soyuz.satellite")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        SoyuzLaunchpadBlockEntity.SoyuzStatus status = this.menu.status();
        if (status == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING) {
            int countdown = Math.max(0, this.menu.countdown());
            String seconds = Integer.toString(countdown / 20);
            String cents = Integer.toString((countdown % 20) * 5);
            if (seconds.length() == 1) seconds = "0" + seconds;
            if (cents.length() == 1) cents += "0";
            graphics.drawString(this.font, seconds + ":" + cents, 85, 121, 0xFF0000, false);
            return;
        }

        Component label = switch (status) {
            case ABSENT -> Component.translatable("gui.reinhardtshbm.launchpad_soyuz.idle");
            case LOADING -> Component.translatable("gui.reinhardtshbm.launchpad_soyuz.loading");
            case FUELING -> Component.translatable("gui.reinhardtshbm.launchpad_soyuz.fueling");
            case READY -> Component.translatable("gui.reinhardtshbm.launchpad_soyuz.ready");
            case LAUNCHING -> Component.empty();
        };
        int color = switch (status) {
            case ABSENT -> 0xFF0000;
            case LOADING -> 0xFF8000;
            case FUELING -> 0xFFFF00;
            case READY -> 0x00FF00;
            case LAUNCHING -> 0xFFFFFF;
        };
        drawCenteredConstrained(graphics, label, 97, 125, 22, color);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);

        int power = this.menu.energyScaled(52);
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 96 - power,
                    194, 52 - power, 16, power, 256, 256);
        }

        int modeOffset = this.menu.cargoMode() ? 18 : 0;
        graphics.blit(TEXTURE, this.leftPos + 97 - modeOffset, this.topPos + 52,
                228 - modeOffset, 26, 18, 18, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 157, this.topPos + 31,
                this.menu.hasJetFuel() ? 210 : 216, 0, 6, 8, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 175, this.topPos + 31,
                this.menu.hasOxidizer() ? 210 : 216, 0, 6, 8, 256, 256);
        graphics.blit(TEXTURE, this.leftPos + 139, this.topPos + 31,
                this.menu.hasPower() ? 210 : 216, 0, 6, 8, 256, 256);

        int orbital = this.menu.orbitalState();
        if (orbital > 0) {
            graphics.blit(TEXTURE, this.leftPos + 79, this.topPos + 25,
                    210 + (orbital - 1) * 18, 8, 18, 18, 256, 256);
        }
        if (this.menu.status() == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING) {
            graphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 97,
                    210, 44, 18, 18, 256, 256);
        }

        drawFluid(graphics, 152, 96, 16, this.menu.keroseneScaled(52), kerosene());
        drawFluid(graphics, 170, 96, 16, this.menu.oxygenScaled(52), oxygen());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inButton(mouseX, mouseY, 79, 52)) {
            send(SoyuzLaunchpadControlPayload.MODE_CARGO);
            return true;
        }
        if (button == 0 && inButton(mouseX, mouseY, 97, 52)) {
            send(SoyuzLaunchpadControlPayload.MODE_SATELLITE);
            return true;
        }
        if (button == 0 && inButton(mouseX, mouseY, 88, 97)) {
            send(SoyuzLaunchpadControlPayload.LAUNCH);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new SoyuzLaunchpadControlPayload(this.menu.blockPos(), action));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean inButton(double mouseX, double mouseY, int x, int y) {
        return this.leftPos + x <= mouseX && this.leftPos + x + 18 > mouseX
                && this.topPos + y < mouseY && this.topPos + y + 18 >= mouseY;
    }

    private void drawCenteredConstrained(GuiGraphics graphics, Component text,
                                         int centerX, int y, int maxWidth, int color) {
        int width = this.font.width(text);
        float scale = width > maxWidth ? maxWidth / (float) width : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, -this.font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width,
                           int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
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

    private static HbmFluidDefinition kerosene() {
        return HbmFluids.byName("kerosene").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition oxygen() {
        return HbmFluids.byName("oxygen").orElse(HbmFluids.none());
    }
}
