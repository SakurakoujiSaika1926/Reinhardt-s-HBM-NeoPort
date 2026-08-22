package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.HeaterMenu;
import com.reinhardt.hbm.network.HeatExchangerControlPayload;
import com.reinhardt.hbm.network.ToggleHeaterPayload;
import net.minecraft.client.gui.components.EditBox;
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

public class HeaterScreen extends AbstractContainerScreen<HeaterMenu> {
    private static final ResourceLocation FIREBOX = ReinhardtsHBM.id("textures/gui/machine/gui_firebox.png");
    private static final ResourceLocation OVEN = ReinhardtsHBM.id("textures/gui/machine/gui_heating_oven.png");
    private static final ResourceLocation OILBURNER = ReinhardtsHBM.id("textures/gui/machine/gui_oilburner.png");
    private static final ResourceLocation HEATEX = ReinhardtsHBM.id("textures/gui/machine/gui_heatex.png");
    private static final int COMMON_HEIGHT = 168;
    private static final int OILBURNER_HEIGHT = 203;
    private static final int HEATEX_HEIGHT = 204;
    private static final int OILBURNER_TOGGLE_X = 80;
    private static final int OILBURNER_TOGGLE_Y = 54;
    private static final int OILBURNER_TOGGLE_WIDTH = 16;
    private static final int OILBURNER_TOGGLE_HEIGHT = 14;
    private EditBox heatExchangerAmountField;
    private EditBox heatExchangerDelayField;

    public HeaterScreen(HeaterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = menu.isOilburnerLayout() ? OILBURNER_HEIGHT : menu.isHeatExchangerLayout() ? HEATEX_HEIGHT : COMMON_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        if (this.menu.isHeatExchangerLayout()) {
            this.heatExchangerAmountField = new EditBox(this.font, this.leftPos + 73, this.topPos + 31, 30, 10, Component.empty());
            this.heatExchangerAmountField.setMaxLength(5);
            this.heatExchangerAmountField.setValue(Integer.toString(this.menu.heatExchangerAmountToCool()));
            this.heatExchangerAmountField.setResponder(value -> submitHeatExchangerSettings());
            this.addRenderableWidget(this.heatExchangerAmountField);

            this.heatExchangerDelayField = new EditBox(this.font, this.leftPos + 73, this.topPos + 49, 30, 10, Component.empty());
            this.heatExchangerDelayField.setMaxLength(5);
            this.heatExchangerDelayField.setValue(Integer.toString(this.menu.heatExchangerTickDelay()));
            this.heatExchangerDelayField.setResponder(value -> submitHeatExchangerSettings());
            this.addRenderableWidget(this.heatExchangerDelayField);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.menu.isOilburnerLayout()) {
            renderOilburnerTooltips(guiGraphics, mouseX, mouseY);
            return;
        }
        if (this.menu.isHeatExchangerLayout()) {
            renderHeatExchangerTooltips(guiGraphics, mouseX, mouseY);
            return;
        }

        if (isHovering(80, 27, 71, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format("%,d / %,d TU", this.menu.heat(), this.menu.maxHeat())
            )), mouseX, mouseY);
        }
        if (isHovering(80, 36, 71, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    this.menu.burnHeat() + " TU/t, " + (this.menu.burnTime() / 20) + "s"
            )), mouseX, mouseY);
        }
        if (isHovering(80, 47, 71, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("Setting: " + this.menu.setting()),
                    Component.literal("Power: " + this.menu.power() + " HE"),
                    Component.literal("Input: " + this.menu.lastInput() + " HE/t")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture();
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.isOilburnerLayout()) {
            renderOilburnerBg(guiGraphics, texture);
            return;
        }
        if (this.menu.isHeatExchangerLayout()) {
            renderHeatExchangerBg(guiGraphics);
            return;
        }

        int heat = this.menu.heatScaled(69);
        if (heat > 0) {
            guiGraphics.blit(texture, this.leftPos + 81, this.topPos + 28, 176, 0, heat, 5);
        }

        int burn = this.menu.burnScaled(70);
        if (burn > 0) {
            guiGraphics.blit(texture, this.leftPos + 81, this.topPos + 37, 176, 5, burn, 5);
        }

        if (this.menu.enabled() && (this.menu.burnTime() > 0 || this.menu.kind() >= 2)) {
            guiGraphics.blit(texture, this.leftPos + 25, this.topPos + 26, 176, 10, 18, 18);
        }
    }

    private ResourceLocation texture() {
        return switch (this.menu.kind()) {
            case 1 -> OVEN;
            case 2 -> OILBURNER;
            case 4 -> HEATEX;
            default -> FIREBOX;
        };
    }

    private void renderOilburnerBg(GuiGraphics guiGraphics, ResourceLocation texture) {
        drawFluid(guiGraphics, 44, 69, 16, this.menu.oilScaled(52), this.menu.oilFluid());

        int heat = this.menu.heatScaled(52);
        if (heat > 0) {
            guiGraphics.blit(texture, this.leftPos + 116, this.topPos + 69 - heat, 194, 52 - heat, 16, heat);
        }

        if (this.menu.enabled()) {
            guiGraphics.blit(texture, this.leftPos + 70, this.topPos + 54, 210, 0, 35, 14);
            if (this.menu.oilAmount() > 0 && HeaterBlockEntity.flammableHeatPerMillibucket(this.menu.oilFluid()) > 0) {
                guiGraphics.blit(texture, this.leftPos + 79, this.topPos + 34, 176, 0, 18, 18);
            }
        }
    }

    private void renderHeatExchangerBg(GuiGraphics guiGraphics) {
        drawFluid(guiGraphics, 44, 88, 16, (int) ((long) this.menu.heatExchangerInputAmount() * 52 / this.menu.heatExchangerInputCapacity()), this.menu.heatExchangerInputFluid());
        drawFluid(guiGraphics, 116, 88, 16, (int) ((long) this.menu.heatExchangerOutputAmount() * 52 / this.menu.heatExchangerOutputCapacity()), this.menu.heatExchangerOutputFluid());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && this.menu.isOilburnerLayout()
                && this.leftPos + OILBURNER_TOGGLE_X <= mouseX
                && this.leftPos + OILBURNER_TOGGLE_X + OILBURNER_TOGGLE_WIDTH > mouseX
                && this.topPos + OILBURNER_TOGGLE_Y < mouseY
                && this.topPos + OILBURNER_TOGGLE_Y + OILBURNER_TOGGLE_HEIGHT >= mouseY) {
            PacketDistributor.sendToServer(new ToggleHeaterPayload(this.menu.blockPos()));
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderOilburnerTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(116, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format("%,d / %,d TU", this.menu.heat(), this.menu.maxHeat())
            )), mouseX, mouseY);
        }
        if (isHovering(79, 34, 18, 18, mouseX, mouseY)) {
            int heatPerMillibucket = HeaterBlockEntity.flammableHeatPerMillibucket(this.menu.oilFluid());
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(this.menu.setting() + " mB/t"),
                    Component.literal((this.menu.setting() * heatPerMillibucket) + " TU/t")
            ), mouseX, mouseY);
        }
        if (isHovering(44, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(this.menu.oilFluid(), this.menu.oilAmount(), this.menu.oilCapacity()), mouseX, mouseY);
        }
    }

    private void renderHeatExchangerTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(44, 36, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(
                    this.menu.heatExchangerInputFluid(),
                    this.menu.heatExchangerInputAmount(),
                    this.menu.heatExchangerInputCapacity()
            ), mouseX, mouseY);
        }
        if (isHovering(116, 36, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(
                    this.menu.heatExchangerOutputFluid(),
                    this.menu.heatExchangerOutputAmount(),
                    this.menu.heatExchangerOutputCapacity()
            ), mouseX, mouseY);
        }
        if (isHovering(70, 26, 36, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.heatex.amount_per_cycle")), mouseX, mouseY);
        }
        if (isHovering(70, 44, 36, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.heatex.cycle_tick_delay")), mouseX, mouseY);
        }
    }

    private void submitHeatExchangerSettings() {
        if (!this.menu.isHeatExchangerLayout() || this.heatExchangerAmountField == null || this.heatExchangerDelayField == null) {
            return;
        }
        int amount = parsePositive(this.heatExchangerAmountField.getValue(), this.menu.heatExchangerAmountToCool());
        int delay = parsePositive(this.heatExchangerDelayField.getValue(), this.menu.heatExchangerTickDelay());
        PacketDistributor.sendToServer(new HeatExchangerControlPayload(this.menu.blockPos(), amount, delay));
    }

    private static int parsePositive(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return Math.max(1, fallback);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
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
                graphics.blit(
                        texture,
                        this.leftPos + x + tileX,
                        this.topPos + top + tileY,
                        0,
                        16 - tileHeight,
                        tileWidth,
                        tileHeight,
                        16,
                        16
                );
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static List<Component> fluidTooltip(HbmFluidDefinition fluid, int amount, int capacity) {
        return HbmFluidTooltip.forTank(fluid, amount, capacity);
    }
}
