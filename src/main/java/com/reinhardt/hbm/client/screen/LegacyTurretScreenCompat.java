package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.item.AmmoArtyItem;
import com.reinhardt.hbm.item.AmmoHimarsItem;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.item.TurretAmmoItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class LegacyTurretScreenCompat {
    private LegacyTurretScreenCompat() {
    }

    static void renderWhitelistText(GuiGraphics graphics, Font font, Component selectedName, EditBox input) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, selectedName, 24, 102, 0x00FF00, false);
        graphics.drawString(font, renderedInput(input), 24, 138, 0x00FF00, false);
        graphics.pose().popPose();
    }

    static void renderAmmoHint(GuiGraphics graphics, Font font, int mouseX, int mouseY, int leftPos, int topPos, List<ItemStack> ammoStacks) {
        if (!inside(mouseX, mouseY, leftPos, topPos, 79, 62, 54, 54) || ammoStacks.isEmpty()) {
            return;
        }
        List<ItemStack> display = new ArrayList<>();
        for (ItemStack stack : ammoStacks) {
            if (!stack.isEmpty()) {
                display.add(stack.copyWithCount(1));
            }
        }
        if (display.isEmpty()) {
            return;
        }

        int selected = display.size() > 1 ? (int) ((System.currentTimeMillis() % (1000L * display.size())) / 1000L) : 0;
        List<List<AmmoCell>> rows = itemRows(display, selected);
        Component name = display.get(selected).getHoverName();
        drawStackText(graphics, font, mouseX, mouseY, rows, name);
    }

    static void renderFritzTank(GuiGraphics graphics, int leftPos, int topPos, int amount, int capacity, HbmFluidDefinition fluid) {
        if (amount <= 0 || capacity <= 0 || fluid == null || fluid.isNone()) {
            return;
        }
        int height = Math.min(52, amount * 52 / capacity);
        DfcScreenUtil.drawFluid(null, graphics, leftPos, topPos, 134, 115, 7, height, fluid);
    }

    static void renderFritzTankTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int leftPos, int topPos,
                                       int amount, int capacity, HbmFluidDefinition fluid) {
        if (inside(mouseX, mouseY, leftPos, topPos, 134, 63, 7, 52)) {
            graphics.renderComponentTooltip(font, HbmFluidTooltip.forTank(fluid, amount, capacity), mouseX, mouseY);
        }
    }

    static List<ItemStack> ammoStacks(LegacyTurretType type) {
        return switch (type) {
            case FRIENDLY -> standardFamily(StandardAmmoItem.AmmoFamily.R556);
            case FRITZ -> fritzStacks();
            case HOWARD -> List.of(new ItemStack(HbmItems.AMMO_DGK.get()));
            case MAXWELL -> maxwellUpgradeStacks();
            case RICHARD -> standardFamily(StandardAmmoItem.AmmoFamily.ROCKET_ML);
            case TAUON -> List.of(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.TAU_URANIUM));
            case ARTY -> artyStacks();
            case HIMARS -> himarsStacks();
            case SENTRY -> standardFamily(StandardAmmoItem.AmmoFamily.P9);
            case HOWARD_DAMAGED, SENTRY_DAMAGED -> List.of();
        };
    }

    static List<ItemStack> chekhovAmmoStacks() {
        return List.of(
                StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.BMG50_SP),
                StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.BMG50_FMJ),
                StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.BMG50_JHP),
                StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.BMG50_AP),
                StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.BMG50_DU)
        );
    }

    static List<ItemStack> jeremyAmmoStacks() {
        return List.of(
                TurretAmmoItem.stackFor(HbmItems.AMMO_SHELL.get(), TurretAmmoItem.AmmoType.STOCK),
                TurretAmmoItem.stackFor(HbmItems.AMMO_SHELL.get(), TurretAmmoItem.AmmoType.EXPLOSIVE),
                TurretAmmoItem.stackFor(HbmItems.AMMO_SHELL.get(), TurretAmmoItem.AmmoType.APFSDS_T),
                TurretAmmoItem.stackFor(HbmItems.AMMO_SHELL.get(), TurretAmmoItem.AmmoType.APFSDS_DU),
                TurretAmmoItem.stackFor(HbmItems.AMMO_SHELL.get(), TurretAmmoItem.AmmoType.W9)
        );
    }

    private static String renderedInput(EditBox input) {
        String text = input.getValue();
        if (!input.isFocused()) {
            return text;
        }
        int cursor = Math.max(0, Math.min(text.length(), input.getCursorPosition()));
        String marker = System.currentTimeMillis() % 1000L < 500L ? " " : "||";
        return text.substring(0, cursor) + marker + text.substring(cursor);
    }

    private static List<ItemStack> standardFamily(StandardAmmoItem.AmmoFamily family) {
        List<ItemStack> stacks = new ArrayList<>();
        for (StandardAmmoItem.StandardAmmoType type : StandardAmmoItem.StandardAmmoType.values()) {
            if (type.family() == family) {
                stacks.add(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), type));
            }
        }
        return stacks;
    }

    private static List<ItemStack> artyStacks() {
        String[] variants = {
                "ammo_arty",
                "ammo_arty_classic",
                "ammo_arty_he",
                "ammo_arty_phosphorus",
                "ammo_arty_phosphorus_multi",
                "ammo_arty_mini_nuke",
                "ammo_arty_mini_nuke_multi",
                "ammo_arty_nuke",
                "ammo_arty_cargo",
                "ammo_arty_chlorine",
                "ammo_arty_phosgene",
                "ammo_arty_mustard_gas"
        };
        List<ItemStack> stacks = new ArrayList<>(variants.length);
        for (String variant : variants) {
            stacks.add(AmmoArtyItem.stackFor(HbmItems.AMMO_ARTY.get(), variant));
        }
        return stacks;
    }

    private static List<ItemStack> himarsStacks() {
        String[] variants = {
                "standard",
                "standard_he",
                "standard_wp",
                "standard_tb",
                "standard_lava",
                "standard_mini_nuke",
                "single",
                "single_tb"
        };
        List<ItemStack> stacks = new ArrayList<>(variants.length);
        for (String variant : variants) {
            stacks.add(AmmoHimarsItem.stackFor(HbmItems.AMMO_HIMARS.get(), variant));
        }
        return stacks;
    }

    private static List<ItemStack> fritzStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.FLAME_DIESEL));
        for (HbmFluidDefinition fluid : HbmFluids.niceOrder()) {
            if (fluid.hasTrait(HbmFluidTrait.COMBUSTIBLE) && fluid.hasTrait(HbmFluidTrait.LIQUID)) {
                stacks.add(FluidIconItem.forFluid(fluid));
            }
        }
        return stacks;
    }

    private static List<ItemStack> maxwellUpgradeStacks() {
        String[] ids = {
                "upgrade_speed_1", "upgrade_speed_2", "upgrade_speed_3",
                "upgrade_effect_1", "upgrade_effect_2", "upgrade_effect_3",
                "upgrade_power_1", "upgrade_power_2", "upgrade_power_3",
                "upgrade_afterburn_1", "upgrade_afterburn_2", "upgrade_afterburn_3",
                "upgrade_overdrive_1", "upgrade_overdrive_2", "upgrade_overdrive_3",
                "upgrade_5g", "upgrade_screm"
        };
        List<ItemStack> stacks = new ArrayList<>(ids.length);
        for (String id : ids) {
            BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id(id)).map(ItemStack::new).ifPresent(stacks::add);
        }
        return stacks;
    }

    private static List<List<AmmoCell>> itemRows(List<ItemStack> stacks, int selected) {
        List<AmmoCell> cells = new ArrayList<>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            cells.add(new AmmoCell(stacks.get(i), i == selected));
        }
        List<List<AmmoCell>> rows = new ArrayList<>();
        if (cells.size() < 10) {
            rows.add(cells);
        } else if (cells.size() < 24) {
            rows.add(cells.subList(0, cells.size() / 2));
            rows.add(cells.subList(cells.size() / 2, cells.size()));
        } else {
            int bound0 = (int) Math.ceil(cells.size() / 3.0D);
            int bound1 = (int) Math.ceil(cells.size() / 3.0D * 2.0D);
            rows.add(cells.subList(0, bound0));
            rows.add(cells.subList(bound0, bound1));
            rows.add(cells.subList(bound1, cells.size()));
        }
        return rows;
    }

    private static void drawStackText(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                                      List<List<AmmoCell>> rows, Component label) {
        int height = rows.size() * 18 + 10;
        int longest = font.width(label);
        for (List<AmmoCell> row : rows) {
            longest = Math.max(longest, row.size() * 18);
        }
        int minX = mouseX + 12;
        int minY = mouseY - 12;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        if (minX + longest > screenWidth) {
            minX -= 28 + longest;
        }
        if (minY + height + 6 > screenHeight) {
            minY = screenHeight - height - 6;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        int bg = 0xF0100010;
        graphics.fill(minX - 3, minY - 4, minX + longest + 3, minY - 3, bg);
        graphics.fill(minX - 3, minY + height + 3, minX + longest + 3, minY + height + 4, bg);
        graphics.fill(minX - 3, minY - 3, minX + longest + 3, minY + height + 3, bg);
        graphics.fill(minX - 4, minY - 3, minX - 3, minY + height + 3, bg);
        graphics.fill(minX + longest + 3, minY - 3, minX + longest + 4, minY + height + 3, bg);
        int color0 = 0x505000FF;
        int color1 = (color0 & 0xFEFEFE) >> 1 | color0 & 0xFF000000;
        graphics.fillGradient(minX - 3, minY - 2, minX - 2, minY + height + 2, color0, color1);
        graphics.fillGradient(minX + longest + 2, minY - 2, minX + longest + 3, minY + height + 2, color0, color1);
        graphics.fill(minX - 3, minY - 3, minX + longest + 3, minY - 2, color0);
        graphics.fill(minX - 3, minY + height + 2, minX + longest + 3, minY + height + 3, color1);

        int y = minY;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int x = minX;
            for (AmmoCell cell : rows.get(rowIndex)) {
                if (cell.selected) {
                    graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFF0000);
                    graphics.fill(x, y, x + 16, y + 16, 0xFFB0B0B0);
                }
                graphics.renderItem(cell.stack, x, y);
                graphics.renderItemDecorations(font, cell.stack, x, y);
                x += 18;
            }
            if (rowIndex == 0) {
                y += 2;
            }
            y += 18;
        }
        graphics.drawString(font, label.copy().withStyle(ChatFormatting.WHITE), minX, y, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private static boolean inside(double mouseX, double mouseY, int leftPos, int topPos, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private record AmmoCell(ItemStack stack, boolean selected) {
    }
}
