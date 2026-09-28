package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DrillbitItem extends Item {
    private final DrillType type;

    public DrillbitItem(Properties properties, DrillType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.speed", (int) (this.type.speed() * 100.0D)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.tier", this.type.tier()).withStyle(ChatFormatting.YELLOW));
        if (this.type.fortune() > 0) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.fortune", this.type.fortune()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (this.type.vein()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.vein").withStyle(ChatFormatting.GREEN));
        }
        if (this.type.silk()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.silk").withStyle(ChatFormatting.GREEN));
        }
    }

    public DrillType type() {
        return this.type;
    }

    public static DrillType typeOf(ItemStack stack) {
        return stack.getItem() instanceof DrillbitItem drillbit ? drillbit.type() : null;
    }

    public enum DrillType {
        STEEL(1.0D, 1, 0, false, false),
        STEEL_DIAMOND(1.0D, 1, 2, false, true),
        HSS(1.2D, 2, 0, true, false),
        HSS_DIAMOND(1.2D, 2, 3, true, true),
        DESH(1.5D, 3, 1, true, true),
        DESH_DIAMOND(1.5D, 3, 4, true, true),
        TCALLOY(2.0D, 4, 1, true, true),
        TCALLOY_DIAMOND(2.0D, 4, 4, true, true),
        FERRO(2.5D, 5, 1, true, true),
        FERRO_DIAMOND(2.5D, 5, 4, true, true);

        private final double speed;
        private final int tier;
        private final int fortune;
        private final boolean vein;
        private final boolean silk;

        DrillType(double speed, int tier, int fortune, boolean vein, boolean silk) {
            this.speed = speed;
            this.tier = tier;
            this.fortune = fortune;
            this.vein = vein;
            this.silk = silk;
        }

        public double speed() {
            return this.speed;
        }

        public int tier() {
            return this.tier;
        }

        public int fortune() {
            return this.fortune;
        }

        public boolean vein() {
            return this.vein;
        }

        public boolean silk() {
            return this.silk;
        }

    }
}
