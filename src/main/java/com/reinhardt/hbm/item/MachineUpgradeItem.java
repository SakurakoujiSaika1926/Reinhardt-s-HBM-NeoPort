package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MachineUpgradeItem extends Item {
    private final UpgradeType type;
    private final int tier;

    public MachineUpgradeItem(Properties properties) {
        this(properties, UpgradeType.SPECIAL, 0);
    }

    public MachineUpgradeItem(Properties properties, UpgradeType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        UpgradeType resolvedType = typeFor(stack);
        int resolvedTier = Math.max(1, tierFor(stack));

        switch (resolvedType) {
            case SPEED -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.speed", ChatFormatting.RED);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.speed_cost", ChatFormatting.DARK_RED);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_damage", ChatFormatting.GREEN,
                        resolvedTier * 0.25D);
                addMachineSpecificNote(tooltip);
            }
            case EFFECT -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.effect", ChatFormatting.GREEN);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_range", ChatFormatting.GREEN,
                        resolvedTier * 3);
                addMachineSpecificNote(tooltip);
            }
            case POWER -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.power", ChatFormatting.BLUE);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.power_cost", ChatFormatting.DARK_RED);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_consumption", ChatFormatting.GREEN,
                        resolvedTier * 300);
                addMachineSpecificNote(tooltip);
            }
            case FORTUNE -> add(tooltip, "tooltip.reinhardtshbm.upgrade.fortune", ChatFormatting.GREEN,
                    resolvedTier);
            case AFTERBURN -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.afterburn", ChatFormatting.LIGHT_PURPLE);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.afterburn_oil", ChatFormatting.GREEN,
                        resolvedTier * 10, resolvedTier * 50);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_fire", ChatFormatting.GREEN,
                        resolvedTier * 3);
            }
            case OVERDRIVE -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.overdrive", ChatFormatting.DARK_GRAY);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.overdrive_cost", ChatFormatting.DARK_RED);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_overdrive_damage", ChatFormatting.GREEN,
                        resolvedTier * 2.5D);
                addMachineSpecificNote(tooltip);
            }
            case SMELTER -> add(tooltip, "tooltip.reinhardtshbm.upgrade.smelter", ChatFormatting.GREEN);
            case SHREDDER -> add(tooltip, "tooltip.reinhardtshbm.upgrade.shredder", ChatFormatting.GREEN);
            case CENTRIFUGE -> add(tooltip, "tooltip.reinhardtshbm.upgrade.centrifuge", ChatFormatting.GREEN);
            case CRYSTALLIZER -> add(tooltip, "tooltip.reinhardtshbm.upgrade.crystallizer", ChatFormatting.GREEN);
            case NULLIFIER -> add(tooltip, "tooltip.reinhardtshbm.upgrade.nullifier", ChatFormatting.GREEN);
            case SCREAM -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.scream", ChatFormatting.YELLOW);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_scream", ChatFormatting.GRAY);
            }
            case SPECIAL -> addSpecialTooltip(stack, tooltip);
        }
    }

    private static void addSpecialTooltip(ItemStack stack, List<Component> tooltip) {
        switch (registryPath(stack)) {
            case "upgrade_radius" -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.forcefield_radius", ChatFormatting.GREEN,
                        HbmConfig.FORCEFIELD_RADIUS_UPGRADE.get());
                add(tooltip, "tooltip.reinhardtshbm.upgrade.forcefield_consumption", ChatFormatting.RED,
                        HbmConfig.FORCEFIELD_RADIUS_CONSUMPTION.get());
                add(tooltip, "tooltip.reinhardtshbm.upgrade.stacks_to", ChatFormatting.DARK_GRAY, 16);
            }
            case "upgrade_health" -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.forcefield_health", ChatFormatting.GREEN,
                        HbmConfig.FORCEFIELD_SHIELD_UPGRADE.get());
                add(tooltip, "tooltip.reinhardtshbm.upgrade.forcefield_consumption", ChatFormatting.RED,
                        HbmConfig.FORCEFIELD_SHIELD_CONSUMPTION.get());
                add(tooltip, "tooltip.reinhardtshbm.upgrade.stacks_to", ChatFormatting.DARK_GRAY, 16);
            }
            case "upgrade_gc_speed" -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.gc_speed", ChatFormatting.GREEN);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.gc_speed_stats", ChatFormatting.GRAY,
                        150, 80, 200, 300);
            }
            case "upgrade_5g" -> {
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_5g", ChatFormatting.LIGHT_PURPLE);
                add(tooltip, "tooltip.reinhardtshbm.upgrade.maxwell_5g_warning", ChatFormatting.RED);
            }
            case "upgrade_muffler" -> add(tooltip, "tooltip.reinhardtshbm.upgrade.muffler_legacy",
                    ChatFormatting.DARK_GRAY);
            case "upgrade_template" -> add(tooltip, "tooltip.reinhardtshbm.upgrade.template",
                    ChatFormatting.GRAY);
            default -> {
            }
        }
    }

    private static void addMachineSpecificNote(List<Component> tooltip) {
        add(tooltip, "tooltip.reinhardtshbm.upgrade.machine_specific", ChatFormatting.DARK_GRAY);
    }

    private static void add(List<Component> tooltip, String key, ChatFormatting color, Object... args) {
        tooltip.add(Component.translatable(key, args).withStyle(color));
    }

    public static boolean isMachineUpgrade(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MachineUpgradeItem)) {
            return false;
        }
        String path = registryPath(stack);
        return path.startsWith("upgrade_")
                && !path.equals("upgrade_template")
                && !path.equals("upgrade_muffler");
    }

    public static UpgradeType upgradeType(ItemStack stack) {
        if (stack.getItem() instanceof MachineUpgradeItem upgrade) {
            return upgrade.typeFor(stack);
        }
        return UpgradeType.SPECIAL;
    }

    public static int upgradeTier(ItemStack stack) {
        if (stack.getItem() instanceof MachineUpgradeItem upgrade) {
            return upgrade.tierFor(stack);
        }
        return 0;
    }

    private UpgradeType typeFor(ItemStack stack) {
        if (this.type != UpgradeType.SPECIAL || this.tier != 0) {
            return this.type;
        }

        String path = registryPath(stack);
        if (path.startsWith("upgrade_speed_") || path.startsWith("upgrade_stack_")) {
            return UpgradeType.SPEED;
        }
        if (path.startsWith("upgrade_effect_")) {
            return UpgradeType.EFFECT;
        }
        if (path.startsWith("upgrade_power_")) {
            return UpgradeType.POWER;
        }
        if (path.startsWith("upgrade_fortune_")) {
            return UpgradeType.FORTUNE;
        }
        if (path.startsWith("upgrade_afterburn_")) {
            return UpgradeType.AFTERBURN;
        }
        if (path.startsWith("upgrade_overdrive_")) {
            return UpgradeType.OVERDRIVE;
        }
        if (path.startsWith("upgrade_smelter")) {
            return UpgradeType.SMELTER;
        }
        if (path.startsWith("upgrade_shredder")) {
            return UpgradeType.SHREDDER;
        }
        if (path.startsWith("upgrade_centrifuge")) {
            return UpgradeType.CENTRIFUGE;
        }
        if (path.startsWith("upgrade_crystallizer")) {
            return UpgradeType.CRYSTALLIZER;
        }
        if (path.equals("upgrade_nullifier")) {
            return UpgradeType.NULLIFIER;
        }
        if (path.equals("upgrade_screm")) {
            return UpgradeType.SCREAM;
        }
        return UpgradeType.SPECIAL;
    }

    private int tierFor(ItemStack stack) {
        if (this.type != UpgradeType.SPECIAL || this.tier != 0) {
            return this.tier;
        }

        String path = registryPath(stack);
        if (path.startsWith("upgrade_stack_")) {
            return 1;
        }

        int lastUnderscore = path.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == path.length() - 1) {
            return 0;
        }

        try {
            return Integer.parseInt(path.substring(lastUnderscore + 1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String registryPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
            return "";
        }
        return id.getPath();
    }

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        SMELTER,
        SHREDDER,
        CENTRIFUGE,
        CRYSTALLIZER,
        NULLIFIER,
        SCREAM,
        SPECIAL
    }
}
