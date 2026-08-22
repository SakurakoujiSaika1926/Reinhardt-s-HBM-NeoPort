package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DfcEmitterMenu;
import com.reinhardt.hbm.network.DfcControlPayload;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class DfcEmitterScreen extends AbstractContainerScreen<DfcEmitterMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/dfc/gui_emitter.png");
    private EditBox wattsField;

    public DfcEmitterScreen(DfcEmitterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.wattsField = new EditBox(this.font, this.leftPos + 57, this.topPos + 57, 29, 12, Component.empty());
        this.wattsField.setTextColor(0xFFFFFF);
        this.wattsField.setTextColorUneditable(0xFFFFFF);
        this.wattsField.setBordered(false);
        this.wattsField.setMaxLength(3);
        this.wattsField.setValue(Integer.toString(this.menu.watts()));
        addRenderableWidget(this.wattsField);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.wattsField != null && !this.wattsField.isFocused()) {
            this.wattsField.setValue(Integer.toString(this.menu.watts()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inBounds(mouseX, mouseY, 97, 52, 18, 18)) {
            sendWatts();
            playClick();
            return true;
        }
        if (button == 0 && inBounds(mouseX, mouseY, 133, 52, 18, 18)) {
            PacketDistributor.sendToServer(new DfcControlPayload(this.menu.blockPos(), DfcControlPayload.TARGET_EMITTER_TOGGLE, 0));
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            sendWatts();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fluid(), this.menu.fluidAmount(), this.menu.fluidCapacity()), mouseX, mouseY);
        } else if (isHovering(26, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.energy", this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.dfc.output", DfcScreenUtil.shortNumber(this.menu.prev())), 50, 30, 0xFF7F7F, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.wattsField != null && this.wattsField.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 53, 210, 4, 34, 16);
        }
        if (this.menu.on()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 52, 192, 0, 18, 18);
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 45, 210, 0, this.menu.watts() * 34 / 100, 4);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 69 - power, 176, 52 - power, 16, power);
        }
        DfcScreenUtil.drawFluid(this.minecraft, guiGraphics, this.leftPos, this.topPos, 8, 69, 16, this.menu.fluidScaled(52), this.menu.fluid());
    }

    private void sendWatts() {
        int watts;
        try {
            watts = Integer.parseInt(this.wattsField.getValue());
        } catch (NumberFormatException exception) {
            watts = this.menu.watts();
        }
        watts = Math.max(1, Math.min(100, watts));
        this.wattsField.setValue(Integer.toString(watts));
        PacketDistributor.sendToServer(new DfcControlPayload(this.menu.blockPos(), DfcControlPayload.TARGET_EMITTER_WATTS, watts));
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }

    private void playClick() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
