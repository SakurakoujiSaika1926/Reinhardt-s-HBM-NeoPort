package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.DroneCrateMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** Original 176x185 transport drone crate GUI, including its two mode buttons. */
public final class DroneCrateScreen extends AbstractContainerScreen<DroneCrateMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/storage/gui_crate_drone.png");
    private static final int TANK_X = 125;
    private static final int TANK_Y = 17;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 34;

    public DroneCrateScreen(DroneCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 185;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, HbmFluidTooltip.forTank(menu.tankFluid(), menu.tankAmount(), menu.tankCapacity(), 0), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEXTURE, leftPos + 151, topPos + 16, 194, menu.itemType() ? 0 : 18, 18, 18);
        graphics.blit(TEXTURE, leftPos + 151, topPos + 52, 176, menu.sendingMode() ? 18 : 0, 18, 18);
        drawFluid(graphics, menu.tankFluid(), TANK_X, TANK_Y + TANK_HEIGHT, TANK_WIDTH, menu.tankScaled(TANK_HEIGHT));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickMode(mouseX, mouseY, 151, 16, 0) || clickMode(mouseX, mouseY, 151, 52, 1)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickMode(double mouseX, double mouseY, int x, int y, int menuButton) {
        if (mouseX < leftPos + x || mouseX >= leftPos + x + 18 || mouseY < topPos + y || mouseY >= topPos + y + 18) {
            return false;
        }
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menuButton);
        }
        return true;
    }

    private void drawFluid(GuiGraphics graphics, HbmFluidDefinition fluid, int x, int bottom, int width, int height) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (minecraft == null || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(leftPos + x, topPos + bottom - height, leftPos + x + width, topPos + bottom, 0xFF000000 | fluid.color());
            return;
        }
        int color = fluid.color();
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, leftPos + x, topPos + bottom - height + tileY, 0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
