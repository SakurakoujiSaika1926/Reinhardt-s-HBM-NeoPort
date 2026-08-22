package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class PACoilItem extends LegacyVariantItem {
    public PACoilItem(Properties properties) {
        super(properties.stacksTo(1), "pa_coil", variants("gold", "niobium", "bscco", "chlorophyte"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Spec spec = spec(stack);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pa_coil.quadrupole", number(spec.quadMin), number(spec.quadMax)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pa_coil.dipole", number(spec.dipoleMin), number(spec.dipoleMax)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pa_coil.dipole_distance", spec.dipoleDistanceMin).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pa_coil.minimum_penalty").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pa_coil.maximum_crash").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pa_coil.double_penalty_crash").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.reinhardtshbm.pa_coil").withStyle(ChatFormatting.DARK_GRAY));
    }

    public Spec spec(ItemStack stack) {
        return Spec.byId(variant(stack).id());
    }

    private static String number(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    public record Spec(int quadMin, int quadMax, int dipoleMin, int dipoleMax, int dipoleDistanceMin) {
        private static final Spec GOLD = new Spec(0, 2_200, 0, 2_200, 15);
        private static final Spec NIOBIUM = new Spec(1_500, 8_400, 1_500, 8_400, 21);
        private static final Spec BSCCO = new Spec(7_500, 15_000, 7_500, 15_000, 27);
        private static final Spec CHLOROPHYTE = new Spec(14_500, 75_000, 14_500, 75_000, 51);

        public static Spec byId(String id) {
            return switch (id) {
                case "niobium" -> NIOBIUM;
                case "bscco" -> BSCCO;
                case "chlorophyte" -> CHLOROPHYTE;
                default -> GOLD;
            };
        }
    }
}
