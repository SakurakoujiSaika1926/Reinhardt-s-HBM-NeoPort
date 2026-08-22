package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.menu.ArcFurnaceMenu;
import com.reinhardt.hbm.item.ScrapsItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class ArcFurnaceScreen extends AbstractContainerScreen<ArcFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_arc_furnace.png");

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(151, 17, 18, 18, mouseX, mouseY)
                && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(8, 36, 7, 70, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", menu.power(), ArcFurnaceMenuPower.MAX
            )), mouseX, mouseY);
        }
        if (isHovering(17, 36, 7, 70, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress", menu.progress(), menu.processTime()
            )), mouseX, mouseY);
        }
        if (isHovering(152, 36, 16, 70, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, liquidTooltip(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.liquidMode()) graphics.blit(TEXTURE, leftPos + 151, topPos + 17, 190, 18, 18, 18);
        if (menu.working()) graphics.blit(TEXTURE, leftPos + 7, topPos + 17, 190, 0, 18, 18);

        int power = menu.powerScaled(70);
        if (power > 0) graphics.blit(TEXTURE, leftPos + 8, topPos + 106 - power, 176, 70 - power, 7, power);
        int progress = menu.progressScaled(70);
        if (progress > 0) graphics.blit(TEXTURE, leftPos + 17, topPos + 106 - progress, 183, 70 - progress, 7, progress);
        drawLiquid(graphics);
    }

    private void drawLiquid(GuiGraphics graphics) {
        int lastHeight = 0;
        int lastAmount = 0;
        for (int index = 0; index < 4; index++) {
            FoundryMaterial material = FoundryMaterial.byId(menu.liquidMaterialId(index)).orElse(null);
            int materialAmount = menu.liquidMaterialAmount(index);
            if (material == null || materialAmount <= 0) continue;
            int targetHeight = Math.min(70, (lastAmount + materialAmount) * 70 / menu.liquidCapacity());
            int height = targetHeight - lastHeight;
            lastAmount += materialAmount;
            if (height <= 0) continue;
            int color = material.moltenColor();
            graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
            graphics.blit(TEXTURE, leftPos + 152, topPos + 106 - targetHeight, 208, 70 - targetHeight, 16, height);
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.3F);
            graphics.blit(TEXTURE, leftPos + 152, topPos + 106 - targetHeight, 208, 70 - targetHeight, 16, height);
            RenderSystem.disableBlend();
            lastHeight = targetHeight;
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private List<Component> liquidTooltip() {
        List<Component> tooltip = new ArrayList<>();
        if (menu.liquidAmount() <= 0) {
            tooltip.add(Component.translatable("gui.reinhardtshbm.empty").withStyle(ChatFormatting.RED));
            return tooltip;
        }
        for (int index = 0; index < 4; index++) {
            FoundryMaterial material = FoundryMaterial.byId(menu.liquidMaterialId(index)).orElse(null);
            int amount = menu.liquidMaterialAmount(index);
            if (material != null && amount > 0) {
                tooltip.add(Component.translatable(material.translationKey()).withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(ScrapsItem.formatAmountComponent(amount)));
            }
        }
        return tooltip;
    }

    private static final class ArcFurnaceMenuPower {
        private static final int MAX = (int) com.reinhardt.hbm.blockentity.ArcFurnaceBlockEntity.MAX_POWER;
    }
}
