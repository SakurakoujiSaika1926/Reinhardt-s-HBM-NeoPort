package com.reinhardt.hbm.recipe.anvil;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Any food plus Plan C or a red pill, matching the hidden 1.7.10 recipe. */
public final class AnvilPoisoningRecipe extends AnvilSmithingRecipe {
    private static final String CYANIDE = "ntmCyanide";
    private static final String RED_PILL = "ntmRedPill";

    public AnvilPoisoningRecipe() {
        super(1, poisoned(new ItemStack(Items.BREAD), CYANIDE),
                AnvilIngredient.of(Items.BREAD, 1),
                AnvilIngredient.of(BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("plan_c")), 1));
    }

    @Override
    public Match match(ItemStack leftStack, ItemStack rightStack) {
        if (leftStack.getCount() < 1 || rightStack.getCount() < 1
                || !leftStack.has(DataComponents.FOOD)) {
            return null;
        }
        return isCatalyst(rightStack) ? new Match(this, false) : null;
    }

    @Override
    public ItemStack getOutput(ItemStack leftStack, ItemStack rightStack) {
        return poisoned(leftStack.copyWithCount(1), isRedPill(rightStack) ? RED_PILL : CYANIDE);
    }

    @Override
    public int amountConsumed(int slot, boolean mirrored) {
        return slot == 0 || slot == 1 ? 1 : 0;
    }

    private static boolean isCatalyst(ItemStack stack) {
        return stack.is(BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("plan_c"))) || isRedPill(stack);
    }

    private static boolean isRedPill(ItemStack stack) {
        return stack.is(BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("pill_red")));
    }

    private static ItemStack poisoned(ItemStack stack, String key) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(key, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
