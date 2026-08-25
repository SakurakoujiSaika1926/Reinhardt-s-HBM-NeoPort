package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/** Direct port of ItemCrayon's 16 dye variants and overlay tint. */
public final class LegacyCrayonItem extends Item {
    private static final String VARIANT_TAG = "crayon";
    private static final int[] COLORS = {
            1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603,
            4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320
    };
    private static final String[] NAMES = {
            "black", "red", "green", "brown", "blue", "purple", "cyan", "silver",
            "gray", "pink", "lime", "yellow", "lightblue", "magenta", "orange", "white"
    };

    public LegacyCrayonItem(Properties properties) {
        super(properties.food(new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(3).saturationModifier(0.6F).alwaysEdible().build()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.crayon." + NAMES[variant(stack)]);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int index = 0; index < NAMES.length; index++) {
            output.accept(stackFor(this, index));
        }
    }

    public static ItemStack stackFor(Item item, int variant) {
        int normalized = Math.clamp(variant, 0, NAMES.length - 1);
        ItemStack stack = new ItemStack(item);
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(VARIANT_TAG, normalized);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(normalized));
        return stack;
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (tintIndex != 1 || !(stack.getItem() instanceof LegacyCrayonItem crayon)) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | COLORS[crayon.variant(stack)];
    }

    private int variant(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            return Math.clamp(tag.getInt(VARIANT_TAG), 0, NAMES.length - 1);
        }
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return Math.clamp(model == null ? 0 : model.value(), 0, NAMES.length - 1);
    }
}
