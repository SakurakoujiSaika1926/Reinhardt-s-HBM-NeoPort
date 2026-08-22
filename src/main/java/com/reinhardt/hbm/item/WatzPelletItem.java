package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WatzPelletItem extends Item {
    private static final String TYPE_TAG = "watz_type";
    private static final String YIELD_TAG = "yield";
    private final boolean depleted;

    public WatzPelletItem(Properties properties, boolean depleted) {
        super(properties.stacksTo(16));
        this.depleted = depleted;
    }

    public static ItemStack stack(Type type, Item item) {
        ItemStack stack = new ItemStack(item);
        setType(stack, type);
        if (item instanceof WatzPelletItem pellet && !pellet.depleted) {
            setYield(stack, type.baseYield);
        }
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            output.accept(stack(type, this));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        Type type = type(stack);
        String suffix = this.depleted ? ".depleted" : "";
        return Component.translatable("item.reinhardtshbm.watz_pellet." + type.id + suffix);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.depleted) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.depleted").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Type type = type(stack);
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.watz_pellet.depletion",
                String.format(Locale.US, "%.1f", depletionForDisplay(stack) * 100.0D)
        ).withStyle(ChatFormatting.GREEN));
        if (type.passive > 0.0D) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.base_fission", format(type.passive)).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.self_igniting").withStyle(ChatFormatting.RED));
        }
        if (type.heatEmission > 0.0D) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.heat_flux", format(type.heatEmission)).withStyle(ChatFormatting.GOLD));
        }
        if (type.burn != null) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.reaction", type.burn.label()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.fuel_type", type.burn.danger()).withStyle(ChatFormatting.GOLD));
        }
        if (type.heatDiv != null) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.thermal_multiplier", type.heatDiv.label() + " TU").withStyle(ChatFormatting.GOLD));
        }
        if (type.absorb != null) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.watz_pellet.flux_capture", type.absorb.label()).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !this.depleted && depletionForDisplay(stack) > 0.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float) depletionForDisplay(stack) * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float enrichment = (float) enrichment(stack);
        return Mth.hsvToRgb(enrichment / 3.0F, 1.0F, 1.0F);
    }

    public static boolean isActivePellet(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem pellet && !pellet.depleted;
    }

    public static boolean isDepletedPellet(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem pellet && pellet.depleted;
    }

    public static Type type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TYPE_TAG)) {
            return Type.byId(tag.getString(TYPE_TAG));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null) {
            return Type.byOrdinal(modelData.value());
        }
        return Type.SCHRABIDIUM;
    }

    public static void setType(ItemStack stack, Type type) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TYPE_TAG, type.id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
    }

    public static double yield(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(YIELD_TAG)) {
            setYield(stack, type(stack).baseYield);
            return type(stack).baseYield;
        }
        return tag.getDouble(YIELD_TAG);
    }

    public static void setYield(ItemStack stack, double yield) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(YIELD_TAG, yield);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static double enrichment(ItemStack stack) {
        Type type = type(stack);
        return WatzPelletItem.yield(stack) / type.baseYield;
    }

    private static double depletionForDisplay(ItemStack stack) {
        return 1.0D - Mth.clamp(enrichment(stack), 0.0D, 1.0D);
    }

    private static String format(double value) {
        return String.format(Locale.US, "%,.3f", value);
    }

    public enum Type {
        SCHRABIDIUM("schrabidium", 0x32FFFF, 0x005C5C, 2_000, 20.0D, 0.01D, Function.linear(1.5D), Function.sqrtFalling(10.0D), null),
        HES("hes", 0x66DCD6, 0x023933, 1_750, 20.0D, 0.005D, Function.linear(1.25D), Function.sqrtFalling(15.0D), null),
        MES("mes", 0xCBEADF, 0x28473C, 1_500, 15.0D, 0.0025D, Function.linear(1.15D), Function.sqrtFalling(15.0D), null),
        LES("les", 0xABB4A8, 0x0C1105, 1_250, 15.0D, 0.00125D, Function.linear(1.0D), Function.sqrtFalling(20.0D), null),
        HEN("hen", 0xA6B2A6, 0x030F03, 0, 10.0D, 0.0005D, Function.sqrt(100.0D), Function.sqrtFalling(10.0D), null),
        MEU("meu", 0xC1C7BD, 0x2B3227, 0, 10.0D, 0.0005D, Function.sqrt(75.0D), Function.sqrtFalling(10.0D), null),
        MEP("mep", 0x9AA3A0, 0x111A17, 0, 15.0D, 0.0005D, Function.sqrt(150.0D), Function.sqrtFalling(10.0D), null),
        LEAD("lead", 0xA6A6B2, 0x03030F, 0, 0.0D, 0.0025D, null, null, Function.sqrt(10.0D)),
        BORON("boron", 0xBDC8D2, 0x29343E, 0, 0.0D, 0.0025D, null, null, Function.linear(10.0D)),
        DU("du", 0xC1C7BD, 0x2B3227, 0, 0.0D, 0.0025D, null, null, Function.quadratic(1.0D).withDiv(100.0D)),
        NQD("nqd", 0x4B4B4B, 0x121212, 2_000, 20.0D, 0.01D, Function.linear(2.0D), Function.sqrt(1.0D / 25.0D).withOff(625.0D), null),
        NQR("nqr", 0x2D2D2D, 0x0B0B0B, 2_500, 30.0D, 0.01D, Function.linear(1.5D), Function.sqrt(1.0D / 25.0D).withOff(625.0D), null);

        public final String id;
        public final int colorLight;
        public final int colorDark;
        public final double passive;
        public final double heatEmission;
        public final double mudContent;
        public final double baseYield = 500_000_000.0D;
        public final Function burn;
        public final Function heatDiv;
        public final Function absorb;

        Type(String id, int colorLight, int colorDark, double passive, double heatEmission, double mudContent, Function burn, Function heatDiv, Function absorb) {
            this.id = id;
            this.colorLight = colorLight;
            this.colorDark = colorDark;
            this.passive = passive;
            this.heatEmission = heatEmission;
            this.mudContent = mudContent / 2.0D;
            this.burn = burn;
            this.heatDiv = heatDiv;
            this.absorb = absorb;
        }

        public static Type byId(String id) {
            if (id != null && !id.isBlank()) {
                for (Type type : values()) {
                    if (type.id.equals(id.toLowerCase(Locale.ROOT))) {
                        return type;
                    }
                }
            }
            return SCHRABIDIUM;
        }

        public static Type byOrdinal(int ordinal) {
            Type[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SCHRABIDIUM;
        }

        public static List<String> ids() {
            return Arrays.stream(values()).map(type -> type.id).toList();
        }
    }

    public static final class Function {
        private final Kind kind;
        private final double level;
        private double div = 1.0D;
        private double off = 0.0D;

        private Function(Kind kind, double level) {
            this.kind = kind;
            this.level = level;
        }

        public static Function linear(double level) {
            return new Function(Kind.LINEAR, level);
        }

        public static Function sqrt(double level) {
            return new Function(Kind.SQRT, level);
        }

        public static Function sqrtFalling(double fallFactor) {
            return sqrt(1.0D / fallFactor).withOff(fallFactor * fallFactor);
        }

        public static Function quadratic(double level) {
            return new Function(Kind.QUADRATIC, level);
        }

        public Function withDiv(double div) {
            this.div = div;
            return this;
        }

        public Function withOff(double off) {
            this.off = off;
            return this;
        }

        public double apply(double input) {
            double x = input / this.div + this.off;
            return switch (this.kind) {
                case LINEAR -> x * this.level;
                case SQRT -> Math.sqrt(Math.max(0.0D, x)) * this.level;
                case QUADRATIC -> x * x * this.level;
            };
        }

        public String label() {
            return switch (this.kind) {
                case LINEAR -> xName(true) + " * " + String.format(Locale.US, "%,.1f", this.level);
                case SQRT -> "sqrt(" + xName(false) + ") * " + String.format(Locale.US, "%,.3f", this.level);
                case QUADRATIC -> xName(true) + "^2 * " + String.format(Locale.US, "%,.1f", this.level);
            };
        }

        public String danger() {
            return switch (this.kind) {
                case LINEAR, QUADRATIC -> "DANGEROUS";
                case SQRT -> "MEDIUM";
            };
        }

        private String xName(boolean brackets) {
            String x = "x";
            boolean mod = false;
            if (this.div != 1.0D) {
                x += " / " + String.format(Locale.US, "%,.1f", this.div);
                mod = true;
            }
            if (this.off != 0.0D) {
                x += " + " + String.format(Locale.US, "%,.1f", this.off);
                mod = true;
            }
            return mod && brackets ? "(" + x + ")" : x;
        }

        private enum Kind {
            LINEAR,
            SQRT,
            QUADRATIC
        }
    }
}
