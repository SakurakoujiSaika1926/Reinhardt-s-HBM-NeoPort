package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class FluidIconItem extends Item {
    private static final String FLUID = "fluid";
    private static final String AMOUNT = "amount";
    private static final String PRESSURE = "pressure";

    public FluidIconItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return fluid(stack)
                .<Component>map(definition -> Component.translatable(definition.translationKey()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        fluid(stack).ifPresent(definition -> {
            int amount = amount(stack);
            int pressure = pressure(stack);
            if (amount > 0) {
                tooltip.add(Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY));
            }
            if (pressure > 0) {
                tooltip.add(Component.literal(pressure + " PU").withStyle(ChatFormatting.RED));
            }
            HbmFluidTooltip.appendTraitInfo(tooltip, definition, flag.isAdvanced());
        });
    }

    public static ItemStack forFluid(HbmFluidDefinition definition) {
        return forFluid(definition, 0, 0);
    }

    public static ItemStack forFluid(HbmFluidDefinition definition, int amount, int pressure) {
        ItemStack stack = new ItemStack(HbmItems.FLUID_ICON.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(FLUID, definition.name());
        tag.putInt(AMOUNT, Math.max(0, amount));
        tag.putInt(PRESSURE, Math.max(0, pressure));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static int amount(ItemStack stack) {
        return tag(stack).getInt(AMOUNT);
    }

    public static int pressure(ItemStack stack) {
        return tag(stack).getInt(PRESSURE);
    }

    public static java.util.Optional<HbmFluidDefinition> fluid(ItemStack stack) {
        String name = tag(stack).getString(FLUID);
        return HbmFluids.byName(name);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
