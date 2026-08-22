package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class DrillbitItem extends Item {
    private static final String TYPE = "type";

    public DrillbitItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.drillbit." + type(stack).id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DrillType type = type(stack);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.speed", (int) (type.speed() * 100.0D)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.tier", type.tier()).withStyle(ChatFormatting.YELLOW));
        if (type.fortune() > 0) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.fortune", type.fortune()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (type.vein()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.vein").withStyle(ChatFormatting.GREEN));
        }
        if (type.silk()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drillbit.silk").withStyle(ChatFormatting.GREEN));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (DrillType type : DrillType.values()) {
            output.accept(stackFor(this, type));
        }
    }

    public DrillType type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TYPE)) {
            return DrillType.byId(tag.getString(TYPE));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null) {
            return DrillType.byModelData(modelData.value());
        }
        return DrillType.STEEL;
    }

    public static DrillType typeOf(ItemStack stack) {
        return stack.getItem() instanceof DrillbitItem drillbit ? drillbit.type(stack) : null;
    }

    public static ItemStack stackFor(Supplier<? extends Item> item, DrillType type) {
        return stackFor(item.get(), type);
    }

    public static ItemStack stackFor(Item item, DrillType type) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof DrillbitItem drillbit) {
            drillbit.setType(stack, type);
        }
        return stack;
    }

    private void setType(ItemStack stack, DrillType type) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TYPE, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
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

        public String id() {
            return name().toLowerCase(Locale.ROOT);
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

        private static DrillType byId(String id) {
            if (id != null && !id.isBlank()) {
                for (DrillType type : values()) {
                    if (type.id().equals(id) || type.name().equalsIgnoreCase(id)) {
                        return type;
                    }
                }
            }
            return STEEL;
        }

        private static DrillType byModelData(int modelData) {
            DrillType[] values = values();
            return modelData >= 0 && modelData < values.length ? values[modelData] : STEEL;
        }
    }
}
