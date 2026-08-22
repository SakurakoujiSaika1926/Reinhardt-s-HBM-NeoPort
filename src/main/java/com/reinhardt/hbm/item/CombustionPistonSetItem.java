package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CombustionPistonSetItem extends LegacyVariantItem {
    public CombustionPistonSetItem(Properties properties) {
        super(properties, "piston_set", variants("steel", "dura", "desh", "starmetal"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.piston_set.fuel_efficiency").withStyle(ChatFormatting.YELLOW));
        PistonType type = pistonType(stack);
        CombustibleFuelGrade[] grades = CombustibleFuelGrade.values();
        for (int index = 0; index < type.efficiency.length && index < grades.length; index++) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.piston_set.grade",
                            Component.translatable(grades[index].translationKey()),
                            (int) (type.efficiency[index] * 100.0D))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    public PistonType pistonType(ItemStack stack) {
        String id = variant(stack).id();
        for (PistonType type : PistonType.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return PistonType.STEEL;
    }

    public enum PistonType {
        STEEL("steel", 1.00D, 0.75D, 0.25D, 0.00D, 0.00D),
        DURA("dura", 0.50D, 1.00D, 0.90D, 0.50D, 0.00D),
        DESH("desh", 0.00D, 0.50D, 1.00D, 0.75D, 0.00D),
        STARMETAL("starmetal", 0.50D, 0.75D, 1.00D, 0.90D, 0.50D);

        private final String id;
        private final double[] efficiency;

        PistonType(String id, double... efficiency) {
            this.id = id;
            this.efficiency = efficiency;
        }

        public String id() {
            return this.id;
        }

        public double efficiency(CombustibleFuelGrade grade) {
            int index = grade.ordinal();
            return index >= 0 && index < this.efficiency.length ? this.efficiency[index] : 0.0D;
        }
    }
}
