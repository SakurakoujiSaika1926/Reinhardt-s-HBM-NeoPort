package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.ZirnoxReactorMenu;
import com.reinhardt.hbm.network.ZirnoxControlPayload;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

public class ZirnoxReactorScreen extends AbstractContainerScreen<ZirnoxReactorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_zirnox.png");
    private static final ResourceLocation UTILITY = ReinhardtsHBM.id("textures/gui/gui_utility.png");
    private static final int TOGGLE_X = 144;
    private static final int TOGGLE_Y = 35;
    private static final int TOGGLE_WIDTH = 14;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int VENT_X = 151;
    private static final int VENT_Y = 51;
    private static final int VENT_WIDTH = 36;
    private static final int VENT_HEIGHT = 36;

    public ZirnoxReactorScreen(ZirnoxReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 203;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96;
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
        renderHoverInfo(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawFluidGauge(guiGraphics, 160, 108, this.menu.gaugeScaled(6, 0));
        drawFluidGauge(guiGraphics, 142, 108, this.menu.gaugeScaled(6, 1));
        drawFluidGauge(guiGraphics, 178, 108, this.menu.gaugeScaled(6, 2));
        drawMeterGauge(guiGraphics, 160, 33, this.menu.gaugeScaled(12, 3));
        drawMeterGauge(guiGraphics, 178, 33, this.menu.gaugeScaled(12, 4));

        if (this.menu.active()) {
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 7 + 36 * x, this.topPos + 15 + 36 * y, 238, 238, 18, 18);
                }
            }
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 25 + 36 * x, this.topPos + 33 + 36 * y, 238, 238, 18, 18);
                }
            }
            guiGraphics.blit(TEXTURE, this.leftPos + 142, this.topPos + 15, 220, 238, 18, 18);
        }

        drawInfoPanel(guiGraphics, -16, 36, 2);
        drawInfoPanel(guiGraphics, -16, 52, 3);
        if (this.menu.waterAmount() <= 0) {
            drawInfoPanel(guiGraphics, -16, 68, 6);
        }
        if (this.menu.carbonDioxideAmount() <= 4_000) {
            drawInfoPanel(guiGraphics, -16, 84, 6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            PacketDistributor.sendToServer(new ZirnoxControlPayload(this.menu.blockPos(), ZirnoxControlPayload.ACTION_TOGGLE));
            playClick();
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, VENT_X, VENT_Y, VENT_WIDTH, VENT_HEIGHT)) {
            PacketDistributor.sendToServer(new ZirnoxControlPayload(this.menu.blockPos(), ZirnoxControlPayload.ACTION_VENT));
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderHoverInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(160, 108, 18, 12, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.steamFluid(), this.menu.steamAmount(), this.menu.steamCapacity()), mouseX, mouseY);
        }
        if (isHovering(142, 108, 18, 12, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.carbonDioxideFluid(), this.menu.carbonDioxideAmount(), this.menu.carbonDioxideCapacity()), mouseX, mouseY);
        }
        if (isHovering(178, 108, 18, 12, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.waterFluid(), this.menu.waterAmount(), this.menu.waterCapacity()), mouseX, mouseY);
        }
        if (isHovering(160, 33, 18, 17, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.zirnox.temperature"),
                    Component.literal("   " + this.menu.temperatureCelsius() + " C")
            ), mouseX, mouseY);
        }
        if (isHovering(178, 33, 18, 17, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.zirnox.pressure"),
                    Component.literal("   " + this.menu.pressureBars() + " bar")
            ), mouseX, mouseY);
        }
        if (isHovering(-16, 36, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.zirnox.coolant"), this.leftPos - 8, this.topPos + 52);
        }
        if (isHovering(-16, 52, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.zirnox.pressure"), this.leftPos - 8, this.topPos + 68);
        }
        if (this.menu.waterAmount() <= 0 && isHovering(-16, 68, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.zirnox.warning1"), this.leftPos - 8, this.topPos + 84);
        }
        if (this.menu.carbonDioxideAmount() <= 4_000 && isHovering(-16, 84, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.zirnox.warning2"), this.leftPos - 8, this.topPos + 100);
        }
    }

    private void drawFluidGauge(GuiGraphics graphics, int x, int y, int scaled) {
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 238, 12 * scaled, 18, 12);
    }

    private void drawMeterGauge(GuiGraphics graphics, int x, int y, int scaled) {
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 220, 18 * scaled, 18, 17);
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int type) {
        int sourceX = switch (type) {
            case 3, 7, 11 -> 24;
            case 2, 6, 10 -> 8;
            default -> 0;
        };
        int sourceY = switch (type) {
            case 2, 3 -> 0;
            case 6, 7 -> 16;
            case 10, 11 -> 32;
            default -> 0;
        };
        graphics.blit(UTILITY, this.leftPos + x, this.topPos + y, sourceX, sourceY, 16, 16, 256, 256);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX
                && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY
                && this.topPos + y + height >= mouseY;
    }

    private List<Component> tooltipLines(String key) {
        return Arrays.stream(Component.translatable(key).getString().split("\\$"))
                .<Component>map(Component::literal)
                .toList();
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
        }
    }
}
