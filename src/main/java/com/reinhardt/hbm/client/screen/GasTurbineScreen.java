package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.GasTurbineMenu;
import com.reinhardt.hbm.network.GasTurbineControlPayload;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GasTurbineScreen extends AbstractContainerScreen<GasTurbineMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/generators/gui_turbinegas.png");
    private static final ResourceLocation GAUGE = ReinhardtsHBM.id("textures/gui/gauges/button_big.png");
    private int yStart;
    private int slidStart;
    private int numberToDisplay;
    private int digitNumber;
    private int exponent;

    public GasTurbineScreen(GasTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 223;
        this.inventoryLabelY = this.imageHeight - 94;
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
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos + 74, this.topPos + 86, this.menu.autoMode() ? 194 : 194, this.menu.autoMode() ? 11 : 24, 29, 13);

        switch (this.menu.state()) {
            case 0 -> guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 32, 178, 38, 16, 16);
            case -1 -> {
                guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 32, 194, 38, 16, 16);
                displayStartup(guiGraphics);
            }
            case 1 -> {
                guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 32, 210, 38, 16, 16);
                drawPowerMeterDisplay(guiGraphics, 20 * this.menu.instantPowerOutput());
            }
            default -> {
            }
        }

        guiGraphics.blit(TEXTURE, this.leftPos + 36, this.topPos + 97 - this.menu.powerSliderPos(), 178, 0, 16, 6);
        int powerPixels = this.menu.powerScaled(142);
        if (powerPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 109, 0, 223, powerPixels, 16);
        }

        drawRPMGauge(guiGraphics, this.menu.rpm());
        drawThermometer(guiGraphics, Math.max(20, this.menu.temp()));
        drawInfoPanels(guiGraphics);

        drawFluid(guiGraphics, 8, 65, this.menu.fuelScaled(48), this.menu.fuelFluid(), 16);
        drawFluid(guiGraphics, 8, 103, this.menu.lubricantScaled(32), this.menu.lubricantFluid(), 16);
        drawFluid(guiGraphics, 147, 98, this.menu.waterScaled(36), this.menu.waterFluid(), 16);
        drawFluid(guiGraphics, 147, 58, this.menu.steamScaled(36), this.menu.steamFluid(), 16);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && Math.sqrt(Math.pow(mouseX - (this.leftPos + 88), 2) + Math.pow(mouseY - (this.topPos + 40), 2)) <= 8.0D) {
            if (this.menu.counterOrOutput() == 0 || this.menu.counterOrOutput() == 579) {
                sendState(this.menu.state() - 1);
                playClick();
                return true;
            }
        }
        if (button == 0
                && this.menu.state() == 1
                && mouseX > this.leftPos + 74
                && mouseX <= this.leftPos + 103
                && mouseY >= this.topPos + 86
                && mouseY < this.topPos + 99) {
            sendAuto(!this.menu.autoMode());
            playClick();
            return true;
        }

        this.slidStart = this.menu.powerSliderPos();
        this.yStart = (int) mouseY;
        if (button == 0
                && this.menu.state() == 1
                && this.topPos + 97 - this.slidStart <= this.yStart
                && this.topPos + 103 - this.slidStart > this.yStart
                && this.leftPos + 36 < mouseX
                && this.leftPos + 52 >= mouseX) {
            sendAuto(false);
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.menu.autoMode()
                && this.menu.state() == 1
                && this.leftPos + 36 < mouseX
                && this.leftPos + 52 >= mouseX
                && this.topPos + 37 < mouseY
                && this.topPos + 103 >= mouseY
                && this.topPos + 97 - this.slidStart <= this.yStart
                && this.topPos + 103 - this.slidStart > this.yStart) {
            int slidPos = this.topPos + 100 - (int) mouseY;
            if (slidPos > 60) {
                slidPos = 60;
            } else if (slidPos < 0) {
                slidPos = 0;
            }
            sendSlider(slidPos);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(26, 108, 142, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    GasTurbineBlockEntity.ENERGY_CAPACITY
            )), mouseX, mouseY);
        }
        if (isHovering(36, 36, 16, 66, mouseX, mouseY)) {
            String line = this.menu.state() == 1
                    ? String.format(Locale.ROOT, "Fuel consumption: %.1f mb/s", this.menu.fuelConsumptionPerSecond())
                    : "Generator offline";
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(line)), mouseX, mouseY);
        }
        if (isHovering(133, 23, 8, 72, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Temperature: " + Math.max(20, this.menu.temp()) + "C")), mouseX, mouseY);
        }
        if (isHovering(8, 16, 16, 48, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.fuelFluid(), this.menu.fuelAmount(), GasTurbineBlockEntity.FUEL_CAPACITY), mouseX, mouseY);
        }
        if (isHovering(8, 70, 16, 32, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.lubricantFluid(), this.menu.lubricantAmount(), GasTurbineBlockEntity.LUBRICANT_CAPACITY), mouseX, mouseY);
        }
        if (isHovering(147, 61, 16, 36, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.waterFluid(), this.menu.waterAmount(), GasTurbineBlockEntity.WATER_CAPACITY), mouseX, mouseY);
        }
        if (isHovering(147, 21, 16, 36, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.steamFluid(), this.menu.steamAmount(), GasTurbineBlockEntity.STEAM_CAPACITY), mouseX, mouseY);
        }
        if (isHovering(-16, 34, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, splitTooltip("desc.gui.turbinegas.automode"), mouseX, mouseY);
        }
        if (isHovering(-16, 50, 16, 16, mouseX, mouseY)) {
            ArrayList<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("desc.gui.turbinegas.fuels"));
            for (HbmFluidDefinition definition : HbmFluids.niceOrder()) {
                if (GasTurbineBlockEntity.isGasFuel(definition)) {
                    lines.add(Component.literal("  ").append(Component.translatable(definition.translationKey())));
                }
            }
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
        if ((this.menu.fuelAmount() < 5000 || this.menu.lubricantAmount() < 1000) && isHovering(-16, 66, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, splitTooltip("desc.gui.turbinegas.warning"), mouseX, mouseY);
        }
    }

    private void displayStartup(GuiGraphics guiGraphics) {
        if (this.numberToDisplay < 8_888_888 && this.menu.counterOrOutput() < 60) {
            this.digitNumber++;
            if (this.digitNumber == 9) {
                this.digitNumber = 1;
                this.exponent++;
            }
            this.numberToDisplay += (int) Math.pow(10, this.exponent);
        }
        if (this.menu.counterOrOutput() > 50) {
            this.numberToDisplay = 0;
        }
        drawPowerMeterDisplay(guiGraphics, this.numberToDisplay);
    }

    private void drawPowerMeterDisplay(GuiGraphics guiGraphics, int number) {
        int firstDigitX = 65;
        int firstDigitY = 71;
        int[] digit = new int[7];
        for (int i = 6; i >= 0; i--) {
            digit[i] = number % 10;
            number /= 10;
            guiGraphics.blit(TEXTURE, this.leftPos + firstDigitX + i * 7, this.topPos + firstDigitY, 194 + digit[i] * 5, 0, 5, 11);
        }

        int uselessZeros = 0;
        for (int i = 0; i < 6; i++) {
            if (digit[i] == 0) {
                uselessZeros++;
            } else {
                break;
            }
        }
        for (int i = 0; i < uselessZeros; i++) {
            guiGraphics.blit(TEXTURE, this.leftPos + firstDigitX + i * 7, this.topPos + firstDigitY, 244, 0, 5, 11);
        }
    }

    private void drawRPMGauge(GuiGraphics guiGraphics, int position) {
        int clamped = Math.max(0, Math.min(100, position));
        guiGraphics.blit(GAUGE, this.leftPos + 64, this.topPos + 16, clamped * 48, 0, 48, 48, 4848, 48);
    }

    private void drawThermometer(GuiGraphics guiGraphics, int temp) {
        int clamped = Math.max(0, Math.min(800, temp));
        int height = 64 * clamped / 800;
        if (height > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 136, this.topPos + 28 + 64 - height, 176, 64 - height, 2, height);
        }
    }

    private void drawInfoPanels(GuiGraphics guiGraphics) {
        guiGraphics.blit(TEXTURE, this.leftPos - 16, this.topPos + 34, 176, 80, 16, 16);
        guiGraphics.blit(TEXTURE, this.leftPos - 16, this.topPos + 50, 176, 96, 16, 16);
        if (this.menu.fuelAmount() < 5000 || this.menu.lubricantAmount() < 1000) {
            int u = this.menu.fuelAmount() == 0 || this.menu.lubricantAmount() == 0 ? 176 : 192;
            guiGraphics.blit(TEXTURE, this.leftPos - 16, this.topPos + 66, u, 112, 16, 16);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int height, HbmFluidDefinition fluid, int width) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
        graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
    }

    private List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }

    private List<Component> splitTooltip(String key) {
        String translated = Component.translatable(key).getString();
        String[] parts = translated.split("\\$");
        ArrayList<Component> lines = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                lines.add(Component.literal(part));
            }
        }
        return lines;
    }

    private void sendSlider(int slider) {
        PacketDistributor.sendToServer(new GasTurbineControlPayload(this.menu.blockPos(), slider, true, false, false, 0, false));
    }

    private void sendAuto(boolean autoMode) {
        PacketDistributor.sendToServer(new GasTurbineControlPayload(this.menu.blockPos(), 0, false, autoMode, true, 0, false));
    }

    private void sendState(int state) {
        PacketDistributor.sendToServer(new GasTurbineControlPayload(this.menu.blockPos(), 0, false, false, false, state, true));
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
