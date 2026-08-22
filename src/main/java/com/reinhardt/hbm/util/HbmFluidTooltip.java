package com.reinhardt.hbm.util;

import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HbmFluidTooltip {
    private HbmFluidTooltip() {
    }

    public static List<Component> forTank(HbmFluidDefinition fluid, int amount, int capacity) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(fluid.translationKey()));
        lines.add(Component.literal(amount + "/" + capacity + " mB").withStyle(ChatFormatting.GRAY));
        appendTraitInfo(lines, fluid, false);
        return lines;
    }

    public static List<Component> forTank(HbmFluidDefinition fluid, int amount, int capacity, int pressure) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(fluid.translationKey()));
        lines.add(Component.literal(amount + "/" + capacity + " mB").withStyle(ChatFormatting.GRAY));
        if (pressure != 0) {
            lines.add(Component.translatable("info.reinhardtshbm.fluid.pressure", pressure).withStyle(ChatFormatting.RED));
        }
        appendTraitInfo(lines, fluid, false);
        return lines;
    }

    public static void appendTraitInfo(List<Component> tooltip, HbmFluidDefinition fluid, boolean advanced) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        tooltip.add(Component.translatable(
                "info.reinhardtshbm.fluid.hazard",
                fluid.poison(),
                fluid.flammability(),
                fluid.reactivity()
        ).withStyle(ChatFormatting.DARK_GRAY));
        if (fluid.temperatureCelsius() != 20) {
            ChatFormatting color = fluid.temperatureCelsius() < 20 ? ChatFormatting.BLUE : ChatFormatting.RED;
            tooltip.add(Component.literal(fluid.temperatureCelsius() + " C").withStyle(color));
        }
        if (fluid.flammableHeatEnergy() > 0L) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.flammable").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable(
                    "trait.reinhardtshbm.flammable.desc",
                    shortNumber(fluid.flammableHeatEnergy())
            ).withStyle(ChatFormatting.YELLOW));
        }
        if (fluid.combustibleHeatEnergy() > 0L && fluid.combustibleFuelGrade() != null) {
            CombustibleFuelGrade grade = fluid.combustibleFuelGrade();
            tooltip.add(Component.translatable("trait.reinhardtshbm.combustible").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable(
                    "trait.reinhardtshbm.combustible.desc",
                    shortNumber(fluid.combustibleHeatEnergy())
            ).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable(
                    "trait.reinhardtshbm.combustible.grade",
                    Component.translatable(grade.translationKey()).withStyle(ChatFormatting.RED)
            ).withStyle(ChatFormatting.GOLD));
        }
        if (isKnownPollutingFluid(fluid)) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.polluting").withStyle(ChatFormatting.GOLD));
            if (!advanced) {
                tooltip.add(Component.translatable("desc.reinhardtshbm.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
        if (advanced && !fluid.rawTraits().isBlank()) {
            tooltip.add(Component.literal(fluid.rawTraits()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static String shortNumber(long number) {
        if (number < 1_000L) {
            return Long.toString(number);
        }
        if (number < 1_000_000L) {
            return String.format(Locale.ROOT, "%.2fk", number / 1_000.0D);
        }
        if (number < 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fM", number / 1_000_000.0D);
        }
        if (number < 1_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fG", number / 1_000_000_000.0D);
        }
        if (number < 1_000_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fT", number / 1_000_000_000_000.0D);
        }
        if (number < 1_000_000_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fE", number / 1_000_000_000_000_000.0D);
        }
        return "INFINITE";
    }

    private static boolean isKnownPollutingFluid(HbmFluidDefinition fluid) {
        return switch (fluid.name()) {
            case "oil", "hotoil", "heavyoil", "bitumen", "smear", "lubricant", "crackoil", "coaloil",
                 "hotcrackoil", "woodoil", "coalcreosote", "heavyoil_vacuum", "heatingoil_vacuum",
                 "oil_coker", "naphtha_coker", "oil_ds", "hotoil_ds", "crackoil_ds", "hotcrackoil_ds",
                 "reclaimed", "petroil", "naphtha", "diesel", "lightoil", "kerosene", "biofuel",
                 "balefire", "gasoline", "coalgas", "ethanol", "reformate", "lightoil_vacuum", "xylene",
                 "diesel_reform", "diesel_crack_reform", "kerosene_reform", "fishoil", "sunfloweroil",
                 "naphtha_ds", "lightoil_ds", "petroil_leaded", "gasoline_leaded", "coalgas_leaded",
                 "gas", "petroleum", "biogas", "aromatics", "unsaturateds", "sourgas", "reformgas",
                 "gas_coker", "lpg", "watZ", "watz", "carbondioxide", "nitric_acid", "phosgene",
                 "mustardgas", "redmud", "fullerene" -> true;
            default -> false;
        };
    }
}
