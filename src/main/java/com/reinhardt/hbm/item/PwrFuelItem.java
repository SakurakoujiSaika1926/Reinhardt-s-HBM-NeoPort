package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class PwrFuelItem extends LegacyVariantItem {
    private static final String LIFE_TAG = "life";

    public static final List<Fuel> FUELS = List.of(
            new Fuel("meu", 5.0D, FunctionType.LOG_600_DIV_2500, 1_000_000_000D),
            new Fuel("heu233", 7.5D, FunctionType.SQRT_25, 1_000_000_000D),
            new Fuel("heu235", 7.5D, FunctionType.SQRT_22_5, 1_000_000_000D),
            new Fuel("men", 7.5D, FunctionType.LOG_675_DIV_2500, 1_000_000_000D),
            new Fuel("hen237", 7.5D, FunctionType.SQRT_27_5, 1_000_000_000D),
            new Fuel("mox", 7.5D, FunctionType.LOG_600_DIV_2500, 1_000_000_000D),
            new Fuel("mep", 7.5D, FunctionType.LOG_675_DIV_2500, 1_000_000_000D),
            new Fuel("hep239", 10.0D, FunctionType.SQRT_22_5, 1_000_000_000D),
            new Fuel("hep241", 10.0D, FunctionType.SQRT_25, 1_000_000_000D),
            new Fuel("mea", 7.5D, FunctionType.LOG_750_DIV_2500, 1_000_000_000D),
            new Fuel("hea242", 10.0D, FunctionType.SQRT_25, 1_000_000_000D),
            new Fuel("hes326", 12.5D, FunctionType.SQRT_27_5, 1_000_000_000D),
            new Fuel("hes327", 12.5D, FunctionType.SQRT_30, 1_000_000_000D),
            new Fuel("bfb_am_mix", 2.5D, FunctionType.SQRT_15, 250_000_000D),
            new Fuel("bfb_pu241", 2.5D, FunctionType.SQRT_15, 250_000_000D)
    );

    public PwrFuelItem(Properties properties) {
        super(properties.stacksTo(1), "pwr_fuel", LegacyVariantItem.variants(FUELS.stream().map(Fuel::id).toArray(String[]::new)));
    }

    public Fuel fuel(ItemStack stack) {
        String id = variant(stack).id();
        for (Fuel fuel : FUELS) {
            if (fuel.id().equals(id)) {
                return fuel;
            }
        }
        return FUELS.getFirst();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return life(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Fuel fuel = fuel(stack);
        return Math.min(13, Math.round(13.0F * life(stack) / Math.max(1.0F, (float) fuel.yield())));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x5fd14f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Fuel fuel = fuel(stack);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pwr_fuel.heat", fuel.heatEmission()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pwr_fuel.function", fuel.function().label()).withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static int fuelIndex(ItemStack stack) {
        if (!(stack.getItem() instanceof PwrFuelItem item)) {
            return -1;
        }
        String id = item.variant(stack).id();
        for (int index = 0; index < FUELS.size(); index++) {
            if (FUELS.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    public static ItemStack stackForFuel(net.minecraft.world.level.ItemLike item, int fuelIndex) {
        return LegacyVariantItem.stackFor(item.asItem(), FUELS.get(Math.max(0, Math.min(FUELS.size() - 1, fuelIndex))).id());
    }

    public static int life(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(LIFE_TAG);
    }

    public static void setLife(ItemStack stack, int life) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(LIFE_TAG, Math.max(0, life));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public record Fuel(String id, double heatEmission, FunctionType function, double yield) {
    }

    public enum FunctionType {
        LOG_600_DIV_2500("log(600), div 2500") {
            @Override
            public double output(double flux) {
                return Math.log1p(Math.max(0.0D, flux)) * 600.0D / 2500.0D;
            }
        },
        LOG_675_DIV_2500("log(675), div 2500") {
            @Override
            public double output(double flux) {
                return Math.log1p(Math.max(0.0D, flux)) * 675.0D / 2500.0D;
            }
        },
        LOG_750_DIV_2500("log(750), div 2500") {
            @Override
            public double output(double flux) {
                return Math.log1p(Math.max(0.0D, flux)) * 750.0D / 2500.0D;
            }
        },
        SQRT_15("sqrt x15") {
            @Override
            public double output(double flux) {
                return Math.sqrt(Math.max(0.0D, flux)) * 15.0D;
            }
        },
        SQRT_22_5("sqrt x22.5") {
            @Override
            public double output(double flux) {
                return Math.sqrt(Math.max(0.0D, flux)) * 22.5D;
            }
        },
        SQRT_25("sqrt x25") {
            @Override
            public double output(double flux) {
                return Math.sqrt(Math.max(0.0D, flux)) * 25.0D;
            }
        },
        SQRT_27_5("sqrt x27.5") {
            @Override
            public double output(double flux) {
                return Math.sqrt(Math.max(0.0D, flux)) * 27.5D;
            }
        },
        SQRT_30("sqrt x30") {
            @Override
            public double output(double flux) {
                return Math.sqrt(Math.max(0.0D, flux)) * 30.0D;
            }
        };

        private final String label;

        FunctionType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public abstract double output(double flux);
    }
}
