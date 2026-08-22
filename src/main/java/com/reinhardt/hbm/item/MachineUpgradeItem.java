package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
