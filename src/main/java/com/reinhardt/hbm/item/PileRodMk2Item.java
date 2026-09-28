package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Flattened form of the current upstream {@code ItemPileRodMK2}. Each old
 * metadata value has its own registry id while depletion remains stack data.
 */
public final class PileRodMk2Item extends Item {
    private static final String DEPLETION_TAG = "depletion";

    private final Kind kind;

    public PileRodMk2Item(Properties properties, Kind kind) {
        // Current upstream inherits Item's default stack limit through ItemEnumMulti.
        // This is also required by the exact assembly recipe which produces three
        // zirconium channel rods in one operation.
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    public static boolean isPileRod(ItemStack stack) {
        return stack.getItem() instanceof PileRodMk2Item;
    }

    public static double depletion(ItemStack stack) {
        if (!(stack.getItem() instanceof PileRodMk2Item)) {
            return 0.0D;
        }
        return Math.max(0.0D, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getDouble(DEPLETION_TAG));
    }

    public static double depletionPercent(ItemStack stack) {
        if (!(stack.getItem() instanceof PileRodMk2Item rod) || rod.kind.life <= 0.0D) {
            return 0.0D;
        }
        return depletion(stack) * 100.0D / rod.kind.life;
    }

    public static double reactivity(ItemStack stack, double incomingFlux) {
        if (!(stack.getItem() instanceof PileRodMk2Item rod)) {
            return 0.0D;
        }
        double output = rod.kind.neutronSource;
        if (rod.kind.reactionMultiplier > 0.0D) {
            output += squirt(incomingFlux) * rod.kind.reactionMultiplier;
        }
        return output;
    }

    public static double heatPerNeutron(ItemStack stack) {
        return stack.getItem() instanceof PileRodMk2Item rod ? rod.kind.heatMultiplier : 0.0D;
    }

    public static ItemStack react(ItemStack stack, double incomingFlux) {
        if (!(stack.getItem() instanceof PileRodMk2Item rod) || rod.kind.life <= 0.0D) {
            return stack;
        }
        double next = depletion(stack) + incomingFlux;
        if (next < rod.kind.life) {
            setDepletion(stack, next);
            return stack;
        }
        return new ItemStack(rod.kind.turnsInto());
    }

    public static void clearDepletion(ItemStack stack) {
        setDepletion(stack, 0.0D);
    }

    private static void setDepletion(ItemStack stack, double depletion) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (depletion <= 0.0D) {
            tag.remove(DEPLETION_TAG);
        } else {
            tag.putDouble(DEPLETION_TAG, depletion);
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** Exact current-upstream BobMathUtil.squirt transfer function. */
    private static double squirt(double value) {
        return Math.sqrt(value + 1.0D / ((value + 2.0D) * (value + 2.0D))) - 1.0D / (value + 2.0D);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return this.kind.life > 0.0D && depletion(stack) > 0.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (this.kind.life <= 0.0D) {
            return 13;
        }
        double remaining = 1.0D - Mth.clamp(depletion(stack) / this.kind.life, 0.0D, 1.0D);
        return Mth.clamp((int) Math.round(13.0D * remaining), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.kind.life > 0.0D) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.pile_rod_mk2.lifetime",
                    (int) Math.round(this.kind.life)).withStyle(ChatFormatting.GRAY));
            double percent = depletionPercent(stack);
            if (percent > 0.0D) {
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.pile_rod_mk2.depletion",
                        (int) Math.round(percent)).withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pile_rod_mk2." + this.kind.id)
                .withStyle(ChatFormatting.YELLOW));
    }

    public enum Kind {
        RA226BE("ra226be", 0.0D, 0.0D, 0.0D, 1.0D),
        PO210BE("po210be", 0.0D, 0.0D, 0.0D, 1.0D),
        ZR("zr", 0.0D, 0.0D, 0.0D, 0.0D),
        NU("nu", 1.0D, 25_000.0D, 0.25D, 0.0D),
        PU239("pu239", 1.0D, 500.0D, 0.5D, 0.0D),
        RGP("rgp", 1.0D, 1_000.0D, 0.5D, 0.0D),
        WASTE("waste", 1.0D, 0.0D, 1.5D, 0.0D),
        THORIUM("thorium", 1.0D, 35_000.0D, 0.25D, 0.0D),
        THORIUM_FUEL("thorium_fuel", 1.0D, 2_000.0D, 0.5D, 0.0D);

        private final String id;
        private final double reactionMultiplier;
        private final double life;
        private final double heatMultiplier;
        private final double neutronSource;

        Kind(String id, double reactionMultiplier, double life, double heatMultiplier, double neutronSource) {
            this.id = id;
            this.reactionMultiplier = reactionMultiplier;
            this.life = life;
            this.heatMultiplier = heatMultiplier;
            this.neutronSource = neutronSource;
        }

        public double life() {
            return this.life;
        }

        public Item turnsInto() {
            return switch (this) {
                case ZR -> HbmItems.PILE_ROD_ZR.get();
                case NU -> HbmItems.PILE_ROD_PU239.get();
                case PU239 -> HbmItems.PILE_ROD_RGP.get();
                case RGP, WASTE, THORIUM_FUEL -> HbmItems.PILE_ROD_WASTE.get();
                case THORIUM -> HbmItems.PILE_ROD_THORIUM_FUEL.get();
                case RA226BE -> HbmItems.PILE_ROD_RA226BE.get();
                case PO210BE -> HbmItems.PILE_ROD_PO210BE.get();
            };
        }
    }
}
