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
    private static final ResourceLocation IVY_MIKE_OVERLAY = ReinhardtsHBM.id("textures/gui/weapon/ivymikeschematic.png");

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
        graphics.blit(texture(menu.definition()), this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
        if (menu.definition() == LegacyNukeDefinition.TSAR) {
            renderTsarOverlays(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                4210752, false);
    }

    private static ResourceLocation texture(LegacyNukeDefinition definition) {
        String name = switch (definition) {
            case GADGET -> "gadgetschematic";
            case MAN -> "fatmanschematic";
            case MIKE -> "ivymikeschematic";
            case TSAR -> "tsarbombaschematic";
            case FLEIJA -> "fleijaschematic";
            case PROTOTYPE -> "gui_prototype";
            case SOLINIUM -> "soliniumschematic";
            case N2 -> "n2schematic";
            case CUSTOM -> "gunbombschematic";
            case BALEFIRE -> "fstbmbschematic";
        };
        return ReinhardtsHBM.id("textures/gui/weapon/" + name + ".png");
    }

    private void renderTsarOverlays(GuiGraphics graphics) {
        if (menu.isFilled()) {
            blit(graphics, 18, 50, 176, 18, 16, 16);
        } else if (menu.isReady()) {
            blit(graphics, 18, 50, 176, 0, 16, 16);
        }

        for (int slot = 0; slot < 4; slot++) {
            if (menu.slotHasExpectedItem(slot)) {
                switch (slot) {
                    case 0 -> blit(graphics, 40, 36, 209, 1, 23, 23);
                    case 2 -> blit(graphics, 63, 36, 232, 1, 23, 23);
                    case 1 -> blit(graphics, 40, 59, 209, 24, 23, 23);
                    case 3 -> blit(graphics, 63, 59, 232, 24, 23, 23);
                    default -> {
                    }
                }
            }
        }

        if (menu.slotHasExpectedItem(5)) {
            blit(graphics, 91, 41, 176, 220, 80, 36);
        }
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(IVY_MIKE_OVERLAY, this.leftPos + x, this.topPos + y, (float) u, (float) v,
                width, height, 256, 256);
    }
}
