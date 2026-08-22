package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AmsCoreItem extends Item {
    private final Kind kind;
    private final long powerBase;
    private final int heatBase;
    private final int fuelBase;
    private final int dfcMultiplier;

    public AmsCoreItem(Properties properties, Kind kind) {
        super(properties.rarity(kind == Kind.THINGY ? Rarity.EPIC : Rarity.UNCOMMON));
        this.kind = kind;
        this.powerBase = kind.powerBase;
        this.heatBase = kind.heatBase;
        this.fuelBase = kind.fuelBase;
        this.dfcMultiplier = kind.dfcMultiplier;
    }

    public Kind kind() {
        return this.kind;
    }

    public long powerBase() {
        return this.powerBase;
    }

    public int heatBase() {
        return this.heatBase;
    }

    public int fuelBase() {
        return this.fuelBase;
    }

    public int dfcMultiplier() {
        return this.dfcMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int line = 1; line <= this.kind.tooltipLines; line++) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm." + this.kind.id + "." + line).withStyle(ChatFormatting.GRAY));
        }
    }

    public enum Kind {
        SING("ams_core_sing", 1_000_000_000L, 200, 10, 500, 6),
        WORMHOLE("ams_core_wormhole", 1_500_000_000L, 200, 15, 650, 10),
        EYEOFHARMONY("ams_core_eyeofharmony", 2_500_000_000L, 300, 10, 800, 7),
        THINGY("ams_core_thingy", 5_000_000_000L, 250, 5, 2500, 10);

        private final String id;
        private final long powerBase;
        private final int heatBase;
        private final int fuelBase;
        private final int dfcMultiplier;
        private final int tooltipLines;

        Kind(String id, long powerBase, int heatBase, int fuelBase, int dfcMultiplier, int tooltipLines) {
            this.id = id;
            this.powerBase = powerBase;
            this.heatBase = heatBase;
            this.fuelBase = fuelBase;
            this.dfcMultiplier = dfcMultiplier;
            this.tooltipLines = tooltipLines;
        }
    }
}
