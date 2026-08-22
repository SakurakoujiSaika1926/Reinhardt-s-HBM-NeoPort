package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class PlateFuelItem extends Item {
    private static final String LIFE_TAG = "life";

    private final int lifeTime;
    private final int reactivity;
    private final Function function;

    public PlateFuelItem(Properties properties, int lifeTime, Function function, int reactivity) {
        super(properties.stacksTo(1));
        this.lifeTime = lifeTime;
        this.function = function;
        this.reactivity = reactivity;
    }

    public int react(Level level, ItemStack stack, int flux) {
        if (this.function != Function.PASSIVE) {
            setLife(stack, life(stack) + flux);
        }

        return switch (this.function) {
            case LOGARITHM -> (int) (Math.log10(flux + 1) * 0.5D * this.reactivity);
            case SQUARE_ROOT -> (int) (Math.sqrt(flux) * this.reactivity / 10.0D);
            case NEGATIVE_QUADRATIC -> (int) Math.max((flux - (flux * flux / 10000.0D)) / 100.0D * this.reactivity, 0.0D);
            case LINEAR -> (int) (flux / 100.0D * this.reactivity);
            case PASSIVE -> {
                setLife(stack, life(stack) + this.reactivity);
                yield this.reactivity;
            }
        };
    }

    public boolean isDepleted(ItemStack stack) {
        return life(stack) > this.lifeTime;
    }

    public int lifeTime() {
        return this.lifeTime;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return life(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13.0F * life(stack) / this.lifeTime));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x5fd14f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.plate_fuel.type").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.plate_fuel.function." + this.function.id(), this.reactivity)
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.plate_fuel.life", this.lifeTime)
                .withStyle(ChatFormatting.DARK_AQUA));
        if (life(stack) > 0) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.plate_fuel.used", life(stack), this.lifeTime)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static int life(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(LIFE_TAG);
    }

    public static void setLife(ItemStack stack, int life) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(LIFE_TAG, Math.max(0, life));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public enum Function {
        LOGARITHM("logarithm"),
        SQUARE_ROOT("square_root"),
        NEGATIVE_QUADRATIC("negative_quadratic"),
        LINEAR("linear"),
        PASSIVE("passive");

        private final String id;

        Function(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }
    }
}
