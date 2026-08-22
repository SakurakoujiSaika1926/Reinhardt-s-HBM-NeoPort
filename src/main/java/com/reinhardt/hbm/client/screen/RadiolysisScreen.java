package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.RtgPelletItem;
import com.reinhardt.hbm.menu.RadiolysisMenu;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
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

public final class RadiolysisScreen extends AbstractContainerScreen<RadiolysisMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_radiolysis.png");
    private static final ResourceLocation UTILITY = ReinhardtsHBM.id("textures/gui/gui_utility.png");

    public RadiolysisScreen(RadiolysisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 88 - this.font.width(this.title) / 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int power = Math.min(34, this.menu.energy() * 34 / Math.max(1, this.menu.energyCapacity()));
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 51 - power, 240, 34 - power, 16, power);
        }
        drawFluid(graphics, 61, 69, 8, 52, this.menu.fluid(0), this.menu.fluidAmount(0), this.menu.fluidCapacity(0));
        drawFluid(graphics, 87, 33, 12, 16, this.menu.fluid(1), this.menu.fluidAmount(1), this.menu.fluidCapacity(1));
        drawFluid(graphics, 87, 69, 12, 16, this.menu.fluid(2), this.menu.fluidAmount(2), this.menu.fluidCapacity(2));

        // Direct port of GUIRadiolysis: description, heat, then accepted RTG pellets.
        drawInfoPanel(graphics, -16, 16, 10);
        drawInfoPanel(graphics, -16, 34, 2);
        drawInfoPanel(graphics, -16, 52, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(61, 17, 8, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fluid(0), this.menu.fluidAmount(0), this.menu.fluidCapacity(0)), mouseX, mouseY);
        } else if (isHovering(87, 17, 12, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fluid(1), this.menu.fluidAmount(1), this.menu.fluidCapacity(1)), mouseX, mouseY);
        } else if (isHovering(87, 53, 12, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(this.menu.fluid(2), this.menu.fluidAmount(2), this.menu.fluidCapacity(2)), mouseX, mouseY);
        } else if (isHovering(8, 17, 16, 34, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), this.menu.energyCapacity())), mouseX, mouseY);
        } else if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.radiolysis.desc", ChatFormatting.BLUE),
                    this.leftPos - 8, this.topPos + 32);
        } else if (isHovering(-16, 34, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("desc.gui.rtg.heat", this.menu.heat()).withStyle(ChatFormatting.YELLOW)),
                    this.leftPos - 8, this.topPos + 50);
        } else if (isHovering(-16, 52, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, rtgPelletTooltip(), this.leftPos - 8, this.topPos + 68);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottom, int width, int maxHeight,
                           HbmFluidDefinition fluid, int amount, int capacity) {
        if (fluid.isNone() || amount <= 0) return;
        int height = Math.min(maxHeight, amount * maxHeight / Math.max(1, capacity));
        int color = 0xFF000000 | fluid.color();
        graphics.fill(this.leftPos + x, this.topPos + bottom - height,
                this.leftPos + x + width, this.topPos + bottom, color);
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int type) {
        int sourceX = switch (type) {
            case 3, 7, 11 -> 24;
            case 2, 6, 10 -> 8;
            default -> 0;
        };
        int sourceY = switch (type) {
            case 2, 3 -> 0;
            case 6, 7 -> 16;
            case 10, 11 -> 32;
            default -> 0;
        };
        graphics.blit(UTILITY, this.leftPos + x, this.topPos + y, sourceX, sourceY, 16, 16, 256, 256);
    }

    private List<Component> tooltipLines(String key, ChatFormatting headingColor) {
        String[] lines = Component.translatable(key).getString().split("\\$", -1);
        List<Component> result = new ArrayList<>(lines.length);
        for (int index = 0; index < lines.length; index++) {
            result.add(index == 0
                    ? Component.literal(lines[index]).withStyle(headingColor)
                    : Component.literal(lines[index]));
        }
        return result;
    }

    private List<Component> rtgPelletTooltip() {
        List<Component> result = new ArrayList<>();
        result.add(Component.translatable("desc.gui.rtg.pellets"));
        for (Item pellet : List.of(
                HbmItems.PELLET_RTG_RADIUM.get(),
                HbmItems.PELLET_RTG_WEAK.get(),
                HbmItems.PELLET_RTG.get(),
                HbmItems.PELLET_RTG_STRONTIUM.get(),
                HbmItems.PELLET_RTG_COBALT.get(),
                HbmItems.PELLET_RTG_ACTINIUM.get(),
                HbmItems.PELLET_RTG_AMERICIUM.get(),
                HbmItems.PELLET_RTG_POLONIUM.get(),
                HbmItems.PELLET_RTG_GOLD.get(),
                HbmItems.PELLET_RTG_LEAD.get()
        )) {
            RtgPelletItem rtgPellet = (RtgPelletItem) pellet;
            result.add(Component.translatable("desc.gui.rtg.pellet_power",
                    Component.translatable(pellet.getDescriptionId()), rtgPellet.heat(new ItemStack(pellet)) * 10));
        }
        return result;
    }
}
