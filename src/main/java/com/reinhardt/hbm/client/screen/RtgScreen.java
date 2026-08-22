package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.RtgPelletItem;
import com.reinhardt.hbm.menu.RtgMenu;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Direct layout port of GUIMachineRTG from HBM 1.7.10. */
public final class RtgScreen extends AbstractContainerScreen<RtgMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_rtg.png");
    private static final ResourceLocation UTILITY = ReinhardtsHBM.id("textures/gui/gui_utility.png");

    public RtgScreen(RtgMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 13;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int heat = Math.min(51, this.menu.heat() * 51 / 600);
        if (heat > 0) {
            graphics.blit(TEXTURE, this.leftPos + 124, this.topPos + 61 - heat,
                    176, 10 + (51 - heat), 16, heat, 256, 256);
        }
        int power = Math.min(51, this.menu.energy() * 51 / Math.max(1, this.menu.energyCapacity()));
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 146, this.topPos + 61 - power,
                    192, 10 + (51 - power), 16, power, 256, 256);
        }
        drawInfoPanel(graphics, -12, 25, 2);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 13, 7, 0xFFA6A6A6, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2,
                0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(124, 9, 16, 51, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "desc.gui.rtg.heat", this.menu.heat()).withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        } else if (isHovering(146, 9, 16, 51, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        } else if (isHovering(-12, 25, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, rtgPelletTooltip(), this.leftPos - 8, this.topPos + 52);
        }
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int type) {
        int sourceX = type == 3 || type == 7 || type == 11 ? 24 : 8;
        int sourceY = type == 6 || type == 7 ? 16 : type == 10 || type == 11 ? 32 : 0;
        graphics.blit(UTILITY, this.leftPos + x, this.topPos + y, sourceX, sourceY, 16, 16, 256, 256);
    }

    private List<Component> rtgPelletTooltip() {
        List<Component> result = new ArrayList<>();
        result.add(Component.translatable("desc.gui.rtg.pellets"));
        for (Item pellet : List.of(
                HbmItems.PELLET_RTG_RADIUM.get(), HbmItems.PELLET_RTG_WEAK.get(), HbmItems.PELLET_RTG.get(),
                HbmItems.PELLET_RTG_STRONTIUM.get(), HbmItems.PELLET_RTG_COBALT.get(),
                HbmItems.PELLET_RTG_ACTINIUM.get(), HbmItems.PELLET_RTG_AMERICIUM.get(),
                HbmItems.PELLET_RTG_POLONIUM.get(), HbmItems.PELLET_RTG_GOLD.get(), HbmItems.PELLET_RTG_LEAD.get())) {
            RtgPelletItem rtgPellet = (RtgPelletItem) pellet;
            result.add(Component.translatable("desc.gui.rtg.pellet_power",
                    Component.translatable(pellet.getDescriptionId()), rtgPellet.heat(new ItemStack(pellet)) * 5));
        }
        return result;
    }
}
