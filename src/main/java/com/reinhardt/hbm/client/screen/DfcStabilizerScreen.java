package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.DfcStabilizerMenu;
import com.reinhardt.hbm.network.DfcControlPayload;
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

public class DfcStabilizerScreen extends AbstractContainerScreen<DfcStabilizerMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/dfc/gui_stabilizer.png");
    private EditBox wattsField;

    public DfcStabilizerScreen(DfcStabilizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.wattsField = new EditBox(this.font, this.leftPos + 75, this.topPos + 57, 29, 12, Component.empty());
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
        if (button == 0 && mouseX >= this.leftPos + 124 && mouseX < this.leftPos + 142 && mouseY > this.topPos + 52 && mouseY <= this.topPos + 70) {
            sendWatts();
            Minecraft minecraft = this.minecraft;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
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
        if (isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.energy", this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        long max = Math.max(1L, this.menu.maxLensDamage());
        long remaining = Math.max(0L, max - this.menu.lensDamage());
        guiGraphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.dfc.lens", remaining * 100L / max), 58, 32, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.wattsField != null && this.wattsField.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 71, this.topPos + 53, 192, 4, 34, 16);
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 71, this.topPos + 45, 192, 0, this.menu.watts() * 34 / 100, 4);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 35, this.topPos + 69 - power, 176, 52 - power, 16, power);
        }
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
        PacketDistributor.sendToServer(new DfcControlPayload(this.menu.blockPos(), DfcControlPayload.TARGET_STABILIZER_WATTS, watts));
    }
}
