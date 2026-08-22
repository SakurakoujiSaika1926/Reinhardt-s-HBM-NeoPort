package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
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

import java.util.List;
import java.util.Locale;

public final class IcfPelletItem extends Item {
    public static final long BASE_MAX_DEPLETION = 50_000_000_000L;
    public static final long BASE_FUSING_DIFFICULTY = 10_000_000L;

    private static final String TYPE_1 = "type1";
    private static final String TYPE_2 = "type2";
    private static final String MUON = "muon";
    private static final String DEPLETION = "depletion";

    public IcfPelletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(setup(Fuel.DEUTERIUM, Fuel.TRITIUM, false));
        output.accept(setup(Fuel.HELIUM3, Fuel.HELIUM4, false));
        output.accept(setup(Fuel.LITHIUM, Fuel.OXYGEN, false));
        output.accept(setup(Fuel.SODIUM, Fuel.CHLORINE, true));
        output.accept(setup(Fuel.BERYLLIUM, Fuel.CALCIUM, true));
    }

    public static ItemStack setup(Fuel type1, Fuel type2, boolean muon) {
        return setup(new ItemStack(HbmItems.ICF_PELLET.get()), type1, type2, muon);
    }

    public static ItemStack setup(ItemStack stack, Fuel type1, Fuel type2, boolean muon) {
        CompoundTag tag = data(stack);
        tag.putByte(TYPE_1, (byte) type1.ordinal());
        tag.putByte(TYPE_2, (byte) type2.ordinal());
        tag.putBoolean(MUON, muon);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Fuel getType(ItemStack stack, boolean first) {
        CompoundTag tag = data(stack);
        return Fuel.byOrdinal(tag.getByte(first ? TYPE_1 : TYPE_2), first ? Fuel.DEUTERIUM : Fuel.TRITIUM);
    }

    public static boolean isMuonCatalyzed(ItemStack stack) {
        return data(stack).getBoolean(MUON);
    }

    public static long getMaxDepletion(ItemStack stack) {
        long base = BASE_MAX_DEPLETION;
        base = (long) (base / getType(stack, true).depletionSpeed());
        base = (long) (base / getType(stack, false).depletionSpeed());
        return Math.max(1L, base);
    }

    public static long getFusingDifficulty(ItemStack stack) {
        long base = BASE_FUSING_DIFFICULTY;
        base = (long) (base * getType(stack, true).fusingDifficulty());
        base = (long) (base * getType(stack, false).fusingDifficulty());
        if (isMuonCatalyzed(stack)) {
            base /= 4L;
        }
        return Math.max(1L, base);
    }

    public static long getDepletion(ItemStack stack) {
        return Math.max(0L, data(stack).getLong(DEPLETION));
    }

    public static long react(ItemStack stack, long heat) {
        CompoundTag tag = data(stack);
        tag.putLong(DEPLETION, tag.getLong(DEPLETION) + heat);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return (long) (heat * getType(stack, true).reactionMultiplier() * getType(stack, false).reactionMultiplier());
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        int first = getType(stack, true).color();
        int second = getType(stack, false).color();
        int red = (((first >> 16) & 0xFF) + ((second >> 16) & 0xFF)) / 2;
        int green = (((first >> 8) & 0xFF) + ((second >> 8) & 0xFF)) / 2;
        int blue = ((first & 0xFF) + (second & 0xFF)) / 2;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDepletion(stack) > 0L;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double depletion = Mth.clamp((double) getDepletion(stack) / getMaxDepletion(stack), 0.0D, 1.0D);
        return Math.round(13.0F - (float) depletion * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float remaining = 1.0F - Mth.clamp((float) ((double) getDepletion(stack) / getMaxDepletion(stack)), 0.0F, 1.0F);
        return Mth.hsvToRgb(remaining / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double percent = (double) getDepletion(stack) / getMaxDepletion(stack) * 100.0D;
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.icf_pellet.depletion",
                String.format(Locale.US, "%.1f", percent)
        ).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.icf_pellet.fuel",
                Component.translatable(getType(stack, true).translationKey()),
                Component.translatable(getType(stack, false).translationKey())
        ).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.icf_pellet.heat_required",
                HbmFluidTooltip.shortNumber(getFusingDifficulty(stack))
        ).withStyle(ChatFormatting.YELLOW));
        double multiplier = getType(stack, true).reactionMultiplier() * getType(stack, false).reactionMultiplier();
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.icf_pellet.reactivity",
                (int) (multiplier * 100.0D) / 100.0D
        ).withStyle(ChatFormatting.YELLOW));
        if (isMuonCatalyzed(stack)) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.icf_pellet.muon").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public enum Fuel {
        HYDROGEN(0x4040FF, 1.00D, 0.85D, 1.00D),
        DEUTERIUM(0x2828CB, 1.25D, 1.00D, 1.00D),
        TRITIUM(0x000092, 1.50D, 1.00D, 1.05D),
        HELIUM3(0xFFF09F, 1.75D, 1.00D, 1.25D),
        HELIUM4(0xFF9B60, 2.00D, 1.00D, 1.50D),
        LITHIUM(0xE9E9E9, 1.25D, 0.85D, 2.00D),
        BERYLLIUM(0xA79D80, 2.00D, 1.00D, 2.50D),
        BORON(0x697F89, 3.00D, 0.50D, 3.50D),
        CARBON(0x454545, 2.00D, 1.00D, 5.00D),
        OXYGEN(0xB4E2FF, 1.25D, 1.50D, 7.50D),
        SODIUM(0xDFE4E7, 3.00D, 0.75D, 8.75D),
        CHLORINE(0xDAE598, 2.50D, 1.00D, 9.25D),
        CALCIUM(0xD2C7A9, 3.00D, 1.00D, 9.75D);

        private final int color;
        private final double reactionMultiplier;
        private final double depletionSpeed;
        private final double fusingDifficulty;

        Fuel(int color, double reactionMultiplier, double depletionSpeed, double fusingDifficulty) {
            this.color = color;
            this.reactionMultiplier = reactionMultiplier;
            this.depletionSpeed = depletionSpeed;
            this.fusingDifficulty = fusingDifficulty;
        }

        public int color() {
            return this.color;
        }

        public double reactionMultiplier() {
            return this.reactionMultiplier;
        }

        public double depletionSpeed() {
            return this.depletionSpeed;
        }

        public double fusingDifficulty() {
            return this.fusingDifficulty;
        }

        public String translationKey() {
            return "icffuel.reinhardtshbm." + name().toLowerCase(Locale.ROOT);
        }

        public static Fuel byOrdinal(int ordinal, Fuel fallback) {
            Fuel[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
        }
    }
}
