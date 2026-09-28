package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.block.CraneMachineBlock;
import com.reinhardt.hbm.blockentity.CraneMachineBlockEntity;
import com.reinhardt.hbm.menu.CraneMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Functional, asset-independent GUI for the complete legacy crane family. */
public final class CraneMachineScreen extends AbstractContainerScreen<CraneMachineMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int BUTTON = 0xFF737373;
    private static final int BUTTON_HOVER = 0xFF929292;

    public CraneMachineScreen(CraneMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = switch (menu.kind()) {
            case ROUTER -> 256;
            case EXTRACTOR -> 212;
            default -> 176;
        };
        imageHeight = menu.kind() == CraneMachineBlock.Kind.ROUTER ? 201 : 185;
        inventoryLabelX = switch (menu.kind()) {
            case ROUTER -> 47;
            case EXTRACTOR -> 26;
            default -> 8;
        };
        inventoryLabelY = menu.kind() == CraneMachineBlock.Kind.ROUTER ? 107 : 91;
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
        renderButtons(graphics, mouseX, mouseY);
        if (hoveredSlot != null && menu.isFilterSlot(hoveredSlot)) {
            String mode = menu.filterMode(hoveredSlot.index);
            if (mode != null) {
                List<Component> lines = new java.util.ArrayList<>();
                if (hoveredSlot.hasItem()) lines.add(hoveredSlot.getItem().getHoverName());
                lines.add(Component.translatable("container.reinhardtshbm.crane.filter_mode", mode).withStyle(ChatFormatting.YELLOW));
                lines.add(Component.translatable("container.reinhardtshbm.crane.filter_help").withStyle(ChatFormatting.GRAY));
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
        for (ButtonArea area : buttons()) {
            if (area.contains(mouseX - leftPos, mouseY - topPos)) {
                graphics.renderTooltip(font, buttonDescription(area.id()), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        bevel(graphics, leftPos, topPos, imageWidth, imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            bevel(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, SLOT);
        }
        int playerX = switch (menu.kind()) {
            case ROUTER -> 44;
            case EXTRACTOR -> 23;
            default -> 5;
        };
        int playerY = menu.kind() == CraneMachineBlock.Kind.ROUTER ? 116 : 100;
        graphics.fill(leftPos + playerX, topPos + playerY, leftPos + playerX + 168,
                topPos + playerY + 76, 0x22000000);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        if (menu.kind() != CraneMachineBlock.Kind.ROUTER) {
            CraneMachineMenu.DirectionPair pair = menu.directions();
            Component directions = Component.translatable("container.reinhardtshbm.crane.directions",
                    directionName(pair.inputOrdinal()), directionName(pair.outputOrdinal()));
            graphics.drawString(font, directions, 7, 78, 0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ButtonArea area : buttons()) {
            if (area.contains(mouseX - leftPos, mouseY - topPos)) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, area.id());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ButtonArea area : buttons()) {
            boolean hover = area.contains(mouseX - leftPos, mouseY - topPos);
            bevel(graphics, leftPos + area.x(), topPos + area.y(), area.width(), area.height(),
                    hover ? BUTTON_HOVER : BUTTON);
            Component label = buttonLabel(area.id());
            int max = area.width() - 4;
            String text = font.plainSubstrByWidth(label.getString(), max);
            graphics.drawString(font, text,
                    leftPos + area.x() + (area.width() - font.width(text)) / 2,
                    topPos + area.y() + (area.height() - 8) / 2,
                    0xFFFFFF, true);
        }
    }

    private List<ButtonArea> buttons() {
        return switch (menu.kind()) {
            case BOXER, INSERTER -> List.of(new ButtonArea(0, 151, 34, 18, 18));
            case EXTRACTOR -> List.of(new ButtonArea(0, 128, 30, 14, 26), new ButtonArea(1, 187, 34, 18, 18));
            case GRABBER -> List.of(new ButtonArea(0, 97, 30, 14, 26));
            case UNBOXER -> List.of();
            case ROUTER -> List.of(
                    new ButtonArea(0, 7, 17, 18, 18), new ButtonArea(1, 7, 43, 18, 18),
                    new ButtonArea(2, 7, 69, 18, 18), new ButtonArea(3, 229, 17, 18, 18),
                    new ButtonArea(4, 229, 43, 18, 18), new ButtonArea(5, 229, 69, 18, 18));
            case PARTITIONER, SPLITTER -> List.of();
        };
    }

    private Component buttonLabel(int id) {
        return switch (menu.kind()) {
            case BOXER -> Component.literal(switch (menu.primaryMode()) {
                case CraneMachineBlockEntity.BOXER_MODE_8 -> "8";
                case CraneMachineBlockEntity.BOXER_MODE_16 -> "16";
                case CraneMachineBlockEntity.BOXER_MODE_REDSTONE -> "R";
                default -> "4";
            });
            case EXTRACTOR -> Component.literal(id == 0
                    ? (menu.primaryMode() != 0 ? "W" : "B")
                    : (menu.maxEject() ? "M" : "P"));
            case GRABBER -> Component.literal(menu.primaryMode() != 0 ? "W" : "B");
            case INSERTER -> Component.literal(menu.primaryMode() != 0 ? "X" : "O");
            case ROUTER -> Component.literal(switch (menu.routerMode(id)) {
                case CraneMachineBlockEntity.ROUTER_MODE_WHITELIST -> "W";
                case CraneMachineBlockEntity.ROUTER_MODE_BLACKLIST -> "B";
                case CraneMachineBlockEntity.ROUTER_MODE_WILDCARD -> "*";
                default -> "X";
            });
            default -> Component.empty();
        };
    }

    private Component buttonDescription(int id) {
        return switch (menu.kind()) {
            case BOXER -> Component.translatable(switch (menu.primaryMode()) {
                case CraneMachineBlockEntity.BOXER_MODE_8 -> "container.reinhardtshbm.crane.boxer_8";
                case CraneMachineBlockEntity.BOXER_MODE_16 -> "container.reinhardtshbm.crane.boxer_16";
                case CraneMachineBlockEntity.BOXER_MODE_REDSTONE -> "container.reinhardtshbm.crane.boxer_redstone";
                default -> "container.reinhardtshbm.crane.boxer_4";
            });
            case EXTRACTOR -> id == 0
                    ? Component.translatable(menu.primaryMode() != 0 ? "container.reinhardtshbm.crane.whitelist" : "container.reinhardtshbm.crane.blacklist")
                    : Component.translatable(menu.maxEject() ? "container.reinhardtshbm.crane.max_eject" : "container.reinhardtshbm.crane.partial_eject");
            case GRABBER -> Component.translatable(menu.primaryMode() != 0
                    ? "container.reinhardtshbm.crane.whitelist" : "container.reinhardtshbm.crane.blacklist");
            case INSERTER -> Component.translatable(menu.primaryMode() != 0
                    ? "container.reinhardtshbm.crane.destroy_overflow" : "container.reinhardtshbm.crane.keep_overflow");
            case ROUTER -> Component.translatable(switch (menu.routerMode(id)) {
                case CraneMachineBlockEntity.ROUTER_MODE_WHITELIST -> "container.reinhardtshbm.crane.router_whitelist";
                case CraneMachineBlockEntity.ROUTER_MODE_BLACKLIST -> "container.reinhardtshbm.crane.router_blacklist";
                case CraneMachineBlockEntity.ROUTER_MODE_WILDCARD -> "container.reinhardtshbm.crane.router_wildcard";
                default -> "container.reinhardtshbm.crane.router_disabled";
            });
            default -> Component.empty();
        };
    }

    private static Component directionName(int ordinal) {
        Direction direction = Direction.from3DDataValue(ordinal);
        return Component.translatable("container.reinhardtshbm.crane.direction." + direction.getName());
    }

    private static void bevel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, PANEL_DARK);
        graphics.fill(x, y, x + width - 1, y + height - 1, PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    private record ButtonArea(int id, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
