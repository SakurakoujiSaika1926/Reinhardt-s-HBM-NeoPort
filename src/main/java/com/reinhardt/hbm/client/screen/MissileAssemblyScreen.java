package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.render.MissileMultipartRenderer;
import com.reinhardt.hbm.menu.MissileAssemblyMenu;
import com.reinhardt.hbm.network.MissileAssemblyControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Direct slot and status-marker layout port of GUIMachineMissileAssembly. */
public final class MissileAssemblyScreen extends AbstractContainerScreen<MissileAssemblyMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_missile_assembly.png");

    public MissileAssemblyScreen(MissileAssemblyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int states = this.menu.stateMask();
        drawState(graphics, 0, 13, states);
        drawState(graphics, 1, 31, states);
        drawState(graphics, 2, 49, states);
        drawState(graphics, 3, 67, states);
        drawState(graphics, 4, 85, states);
        if (this.menu.canBuild()) {
            graphics.blit(TEXTURE, this.leftPos + 115, this.topPos + 35, 176, 0, 18, 18, 256, 256);
        }
        renderPreview(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.canBuild() && mouseX >= this.leftPos + 115 && mouseX < this.leftPos + 133
                && mouseY >= this.topPos + 35 && mouseY < this.topPos + 53) {
            PacketDistributor.sendToServer(new MissileAssemblyControlPayload(this.menu.blockPos()));
            if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawState(GuiGraphics graphics, int index, int x, int states) {
        int state = (states >>> (index * 2)) & 3;
        if (state == 1) {
            graphics.blit(TEXTURE, this.leftPos + x, this.topPos + 23, 194, 0, 6, 8, 256, 256);
        } else if (index == 3 && state == 2) {
            graphics.blit(TEXTURE, this.leftPos + x, this.topPos + 23, 200, 0, 6, 8, 256, 256);
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        MissileMultipartRenderer.renderGui(
                graphics,
                this.leftPos + 88,
                this.topPos + 98,
                this.minecraft.level.getBlockState(this.menu.blockPos()),
                this.menu.part(1), this.menu.part(2), this.menu.part(3), this.menu.part(4)
        );
    }
}
