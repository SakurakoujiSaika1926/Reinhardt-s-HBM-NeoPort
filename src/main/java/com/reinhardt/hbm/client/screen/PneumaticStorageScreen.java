package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PneumaticStorageBlock;
import com.reinhardt.hbm.blockentity.PneumaticStorageBlockEntity;
import com.reinhardt.hbm.menu.PneumaticStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Uses the original endpoint GUI sheets; this is not a JEI background. */
public final class PneumaticStorageScreen extends AbstractContainerScreen<PneumaticStorageMenu> {
    private static final ResourceLocation ACCESS = texture("access");
    private static final ResourceLocation CLUTTER = texture("clutter");
    private static final ResourceLocation MONO = texture("mono");
    private static final ResourceLocation IMPORTER = texture("importer");
    private static final ResourceLocation EXPORTER = texture("exporter");

    public PneumaticStorageScreen(PneumaticStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        switch (menu.kind()) {
            case ACCESS -> {
                imageWidth = 210;
                imageHeight = 251;
            }
            case CLUTTER -> {
                imageWidth = 200;
                imageHeight = 235;
            }
            case MONO -> {
                imageWidth = 200;
                imageHeight = 181;
            }
            case IMPORTER, EXPORTER -> {
                imageWidth = 176;
                imageHeight = 185;
            }
        }
        inventoryLabelX = menu.kind() == PneumaticStorageBlock.Kind.ACCESS ? 42 : 8;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        int contentOffset = menu.kind() == PneumaticStorageBlock.Kind.ACCESS ? 34 : 0;
        titleLabelX = contentOffset + 88 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if ((menu.kind() == PneumaticStorageBlock.Kind.CLUTTER
                    || menu.kind() == PneumaticStorageBlock.Kind.MONO)
                    && isHovering(174, 36, 20, 8, mouseX, mouseY)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
            if (menu.kind() == PneumaticStorageBlock.Kind.EXPORTER) {
                if (isHovering(142, 16, 18, 18, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    return true;
                }
                if (isHovering(142, 34, 18, 18, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                    return true;
                }
                if (isHovering(142, 52, 18, 18, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 3);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        switch (menu.kind()) {
            case ACCESS -> {
                graphics.blit(ACCESS, leftPos + 34, topPos, 0, 0, 176, imageHeight);
                graphics.blit(ACCESS, leftPos, topPos, 176, 15, 32, 122);
            }
            case CLUTTER -> graphics.blit(CLUTTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case MONO -> graphics.blit(MONO, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case IMPORTER -> graphics.blit(IMPORTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case EXPORTER -> graphics.blit(EXPORTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        PneumaticStorageBlockEntity storage = menu.storage();
        if (storage == null) {
            return;
        }
        if (menu.kind() == PneumaticStorageBlock.Kind.CLUTTER || menu.kind() == PneumaticStorageBlock.Kind.MONO) {
            int pressure = storage.pressure();
            ResourceLocation texture = menu.kind() == PneumaticStorageBlock.Kind.CLUTTER ? CLUTTER : MONO;
            graphics.blit(texture, leftPos + 174 + 4 * (pressure - 1), topPos + 36, 200, 0, 4, 8);
            int gauge = Math.min(10, storage.airTank().amount() * 10 / PneumaticStorageBlockEntity.AIR_CAPACITY);
            if (gauge > 0) {
                graphics.fill(leftPos + 184, topPos + 35 - gauge, leftPos + 189, topPos + 35, 0xFFCA6C43);
            }
        }
        if (menu.kind() == PneumaticStorageBlock.Kind.MONO) {
            for (int slot = 0; slot < PneumaticStorageBlockEntity.MONO_SLOTS; slot++) {
                int amount = storage.monoAmount(slot);
                if (amount > 0) {
                    int width = amount * 124 / PneumaticStorageBlockEntity.MONO_CAPACITY;
                    graphics.blit(MONO, leftPos + 44, topPos + 17 + slot * 18, 0, 181, width, 16);
                }
            }
        }
        if (menu.kind() == PneumaticStorageBlock.Kind.EXPORTER) {
            if (!storage.continuousRequest()) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 16, 176, 0, 18, 18);
            }
            if (storage.requestMode() == PneumaticStorageBlockEntity.REQUEST_FULL_STACK) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 34, 194, 0, 18, 18);
            } else if (storage.requestMode() == PneumaticStorageBlockEntity.REQUEST_FULL_REQUEST) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 34, 194, 18, 18, 18);
            }
            if (storage.rttyConfiguredMode()) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 52, 176, 18, 18, 18);
                // The authored 1.7.10 sheet greys the manual filter area while
                // the independent RTTY filter bank is active.
                graphics.blit(EXPORTER, leftPos + 14, topPos + 14, 77, 14, 58, 58);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.kind() == PneumaticStorageBlock.Kind.ACCESS) {
            for (int slot = 0; slot < PneumaticStorageMenu.ACCESS_SLOTS; slot++) {
                int amount = menu.accessAmount(slot);
                if (amount > 0) {
                    String label = shortNumber(amount);
                    int x = 42 + (slot % 8) * 18 + 16 - font.width(label) / 2;
                    int y = 17 + (slot / 8) * 18 + 10;
                    graphics.pose().pushPose();
                    graphics.pose().scale(0.5F, 0.5F, 1.0F);
                    graphics.drawString(font, label, x * 2, y * 2, 0xFFFFFF, true);
                    graphics.pose().popPose();
                }
            }
        } else if (menu.kind() == PneumaticStorageBlock.Kind.MONO && menu.storage() != null) {
            for (int slot = 0; slot < PneumaticStorageBlockEntity.MONO_SLOTS; slot++) {
                if (!menu.storage().getItem(slot).isEmpty()) {
                    int amount = menu.storage().monoAmount(slot);
                    String label = String.format(Locale.ROOT, "%,d (%.1f%%)", amount,
                            amount * 100.0D / PneumaticStorageBlockEntity.MONO_CAPACITY);
                    graphics.drawString(font, label, 50, 22 + slot * 18, 0x000000, false);
                }
            }
        }
    }

    private static ResourceLocation texture(String suffix) {
        return ReinhardtsHBM.id("textures/gui/storage/gui_pneumatic_" + suffix + ".png");
    }

    private static String shortNumber(int value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0D);
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
        }
        return Integer.toString(value);
    }
}
