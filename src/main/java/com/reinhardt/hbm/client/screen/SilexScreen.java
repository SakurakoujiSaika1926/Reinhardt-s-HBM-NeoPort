package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.SilexMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import com.reinhardt.hbm.util.Wavelength;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SilexScreen extends AbstractContainerScreen<SilexMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_silex.png");

    public SilexScreen(SilexMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inButton(mouseX, mouseY, 10, 92, 12, 12)) {
            Minecraft minecraft = this.minecraft;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(8, 42, 52, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.tankFluid(), this.menu.tankAmount(), this.menu.tankCapacity(), 0), mouseX, mouseY);
            return;
        }
        if (isHovering(27, 72, 16, 52, mouseX, mouseY) && this.menu.currentItemId() >= 0) {
            ItemStack stack = this.menu.currentDisplay();
            if (!stack.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        stack.getHoverName(),
                        Component.literal(this.menu.currentFill() + "/" + this.menu.maxFill() + " mB")
                ), mouseX, mouseY);
            }
            return;
        }
        if (isHovering(10, 92, 10, 10, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("tooltip.reinhardtshbm.silex.void_contents")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        Wavelength mode = this.menu.mode();
        if (mode != Wavelength.NULL) {
            int color = 0xFF000000 | mode.guiColor(this.minecraft == null || this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime());
            drawWave(guiGraphics, this.leftPos + 81, this.topPos + 46, 84, 16, color);
        }

        if (this.menu.tankAmount() > 0) {
            boolean processable = this.menu.tankFluid().name().equals("peroxide")
                    || this.menu.tankFluid().name().equals("uf6")
                    || this.menu.tankFluid().name().equals("puf6")
                    || this.menu.tankFluid().name().equals("death")
                    || this.menu.tankFluid().name().equals("vitriol")
                    || this.menu.tankFluid().name().equals("redmud");
            guiGraphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 41, 176, processable ? 118 : 109, 54, 9);
        }

        int progress = this.menu.progressScaled(69);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 45, this.topPos + 82, 176, 0, progress, 43);
        }

        int fill = this.menu.currentFillScaled(52);
        if (fill > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 124 - fill, 176, 109 - fill, 16, fill);
        }

        int fluid = this.menu.tankScaled(52);
        if (fluid > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 42, 176, this.menu.tankFluid().name().equals("peroxide") ? 43 : 50, fluid, 7);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        if (this.menu.mode() != Wavelength.NULL) {
            Component text = Component.translatable(this.menu.mode().translationKey());
            int width = this.font.width(text);
            guiGraphics.drawString(this.font, text, 100 + (32 - width / 2), 16, 0x404040, false);
        }
    }

    private boolean inButton(double mouseX, double mouseY, int x, int y, int width, int height) {
        double left = this.leftPos + x;
        double top = this.topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private static void drawWave(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        int mid = y + height / 2;
        for (int i = 0; i < width - 1; i++) {
            double current = Math.sin((i / 84.0D) * Math.PI * 4.0D);
            double next = Math.sin(((i + 1) / 84.0D) * Math.PI * 4.0D);
            int y1 = mid + (int) Math.round(current * (height / 2.0D - 1.0D));
            int y2 = mid + (int) Math.round(next * (height / 2.0D - 1.0D));
            guiGraphics.hLine(x + i, x + i + 1, y1, color);
            guiGraphics.vLine(x + i + 1, Math.min(y1, y2), Math.max(y1, y2), color);
        }
    }
}
