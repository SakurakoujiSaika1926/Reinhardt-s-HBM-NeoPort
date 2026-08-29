package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.IcfCoreMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Exact two-part 248x222 GUI layout from GUIICF. */
public final class IcfCoreScreen extends AbstractContainerScreen<IcfCoreMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_icf.png");

    public IcfCoreScreen(IcfCoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 222;
        inventoryLabelX = 44;
        inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(44, 18, 16, 70, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, HbmFluidTooltip.forTank(menu.tankFluid(0), menu.tankAmount(0), menu.tankCapacity(0)), mouseX, mouseY);
        } else if (isHovering(188, 18, 16, 70, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, HbmFluidTooltip.forTank(menu.tankFluid(1), menu.tankAmount(1), menu.tankCapacity(1)), mouseX, mouseY);
        } else if (isHovering(224, 18, 16, 70, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, HbmFluidTooltip.forTank(menu.tankFluid(2), menu.tankAmount(2), menu.tankCapacity(2)), mouseX, mouseY);
        } else if (isHovering(8, 18, 16, 70, mouseX, mouseY)) {
            String value = menu.maxLaser() <= 0L ? "OFFLINE" : HbmFluidTooltip.shortNumber(menu.laser()) + " TU/t - " + (menu.laser() * 1000L / menu.maxLaser()) / 10.0D + "%";
            graphics.renderComponentTooltip(font, List.of(Component.literal(value)), mouseX, mouseY);
        } else if (isHovering(187, 89, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal(HbmFluidTooltip.shortNumber(menu.heat()) + " / " + HbmFluidTooltip.shortNumber(com.reinhardt.hbm.blockentity.IcfCoreBlockEntity.MAX_HEAT) + " TU")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 114);
        graphics.blit(TEXTURE, leftPos + 36, topPos + 122, 36, 122, 176, 108);
        if (menu.maxLaser() > 0L) {
            int pixels = (int) Math.min(70L, menu.laser() * 70L / menu.maxLaser());
            if (pixels > 0) {
                graphics.blit(TEXTURE, leftPos + 8, topPos + 88 - pixels, 212, 192 - pixels, 16, pixels);
            }
        }
        int heat = (int) Math.min(70L, menu.heat() * 70L / com.reinhardt.hbm.blockentity.IcfCoreBlockEntity.MAX_HEAT);
        if (heat > 0) {
            graphics.fill(leftPos + 196, topPos + 98 - heat, leftPos + 201, topPos + 98, 0xFFFF00AF);
        }
        DfcScreenUtil.drawFluid(minecraft, graphics, leftPos, topPos, 44, 88, 16, menu.tankScaled(0, 70), menu.tankFluid(0));
        DfcScreenUtil.drawFluid(minecraft, graphics, leftPos, topPos, 188, 88, 16, menu.tankScaled(1, 70), menu.tankFluid(1));
        DfcScreenUtil.drawFluid(minecraft, graphics, leftPos, topPos, 224, 88, 16, menu.tankScaled(2, 70), menu.tankFluid(2));
    }
}
