package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyClayTabletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Exact empty/revealed-slot layout of GUIScreenClayTablet. Gun-dependent legacy recipes remain unavailable. */
public final class ClayTabletScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/guide_pedestal.png");
    private static final int WIDTH = 142;
    private static final int HEIGHT = 84;
    private final ItemStack tablet;
    private int left;
    private int top;

    private ClayTabletScreen(ItemStack tablet) {
        super(Component.translatable("item.reinhardtshbm.clay_tablet"));
        this.tablet = tablet.copy();
    }

    public static void open(ItemStack tablet) { Minecraft.getInstance().setScreen(new ClayTabletScreen(tablet)); }
    @Override protected void init() { left = (width - WIDTH) / 2; top = (height - HEIGHT) / 2; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, left, top, 0, 0, WIDTH, HEIGHT);
        // The original screen uses this masked state whenever no recipe set is available.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                graphics.blit(TEXTURE, left + 7 + column * 27, top + 7 + row * 27, 142, 16, 16, 16);
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
