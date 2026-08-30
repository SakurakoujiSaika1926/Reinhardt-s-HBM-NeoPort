package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LegacyNukeDefinition;
import com.reinhardt.hbm.menu.LegacyNukeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Uses the exact 1.7.10 schematic background selected by the bomb type. */
public final class LegacyNukeScreen extends AbstractContainerScreen<LegacyNukeMenu> {
    public LegacyNukeScreen(LegacyNukeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyNukeDefinition definition = menu.definition();
        this.imageWidth = definition == LegacyNukeDefinition.TSAR ? 256 : 176;
        this.imageHeight = definition.guiHeight();
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture(menu.definition()), this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                4210752, false);
    }

    private static ResourceLocation texture(LegacyNukeDefinition definition) {
        String name = switch (definition) {
            case GADGET -> "gadgetSchematic";
            case MAN -> "fatManSchematic";
            case MIKE -> "ivyMikeSchematic";
            case TSAR -> "tsarBombaSchematic";
            case FLEIJA -> "fleijaSchematic";
            case PROTOTYPE -> "gui_prototype";
            case SOLINIUM -> "soliniumSchematic";
            case N2 -> "n2Schematic";
            case CUSTOM -> "gunBombSchematic";
            case BALEFIRE -> "fstbmbSchematic";
        };
        return ReinhardtsHBM.id("textures/gui/weapon/" + name + ".png");
    }
}
