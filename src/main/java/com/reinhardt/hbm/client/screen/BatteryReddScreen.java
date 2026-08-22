package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.BatteryReddMenu;
import com.reinhardt.hbm.network.BatteryReddControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class BatteryReddScreen extends AbstractContainerScreen<BatteryReddMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_battery_redd.png");
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    public BatteryReddScreen(BatteryReddMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 181;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int low = this.menu.redLow();
        int high = this.menu.redHigh();
        int priority = this.menu.priorityOrdinal();
        graphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 16, 176, 52 + low * 18, 18, 18);
        graphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 52, 176, 52 + high * 18, 18, 18);
        graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 35, 194, 52 + priority * 16 - 16, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        BigInteger power = this.menu.power();
        BigInteger delta = this.menu.delta();
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        String powerLabel = FORMAT.format(power) + " HE";
        graphics.drawString(this.font, powerLabel, 242 - this.font.width(powerLabel), 45, 0x00FF00, false);
        String deltaLabel = FORMAT.format(delta) + " HE/s";
        int deltaColor = 0xFF0000;
        if (delta.signum() >= 0) {
            deltaLabel = "+" + deltaLabel;
            deltaColor = delta.signum() > 0 ? 0x00FF00 : 0xFFFF00;
        }
        graphics.drawString(this.font, deltaLabel, 242 - this.font.width(deltaLabel), 65, deltaColor, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, 133, 16, 18, 18)) {
            send(BatteryReddControlPayload.ACTION_LOW);
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, 133, 52, 18, 18)) {
            send(BatteryReddControlPayload.ACTION_HIGH);
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, 152, 35, 16, 16)) {
            send(BatteryReddControlPayload.ACTION_PRIORITY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new BatteryReddControlPayload(this.menu.blockPos(), action));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX
                && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY
                && this.topPos + y + height >= mouseY;
    }
}
