package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JEI-only view of the armor modification table's dynamic install rule.
 *
 * <p>The 1.7.10 armor table does not use static machine recipes: the table
 * accepts any armor stack and modifier stack that ArmorModHandler accepts.
 * Keep this generated from the same applicability check so JEI never exposes
 * fake combinations that the real table rejects.</p>
 */
public record ArmorTableJeiRecipe(
        List<ItemStack> armorInputs,
        ItemStack modifier,
        List<ItemStack> modifiedArmorOutputs,
        int slotType
) {
    public static List<ArmorTableJeiRecipe> createAll(HolderLookup.Provider registries) {
        List<ItemStack> armors = collectArmors();
        List<Item> modifiers = collectModifiers();
        List<ArmorTableJeiRecipe> recipes = new ArrayList<>();

        for (Item modifierItem : modifiers) {
            ArmorModItem armorMod = (ArmorModItem) modifierItem;
            ItemStack modifierStack = new ItemStack(modifierItem);
            List<ItemStack> compatibleArmors = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();

            for (ItemStack armor : armors) {
                if (!ArmorModHandler.isApplicable(armor, modifierStack)) {
                    continue;
                }
                ItemStack armorInput = armor.copy();
                ItemStack output = armor.copy();
                if (registries != null) {
                    ArmorModHandler.applyMod(output, modifierStack.copyWithCount(1), registries);
                }
                compatibleArmors.add(armorInput);
                outputs.add(output);
            }

            if (!compatibleArmors.isEmpty()) {
                recipes.add(new ArmorTableJeiRecipe(
                        List.copyOf(compatibleArmors),
                        modifierStack.copyWithCount(1),
                        List.copyOf(outputs),
                        armorMod.slotType()
                ));
            }
        }

        return List.copyOf(recipes);
    }

    public String slotTranslationKey() {
        return ArmorModHandler.slotTranslationKey(this.slotType);
    }

    private static List<ItemStack> collectArmors() {
        List<ItemStack> armors = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ArmorItem) {
                armors.add(new ItemStack(item));
            }
        }
        armors.sort(Comparator.comparing(stack -> itemId(stack.getItem()).toString()));
        return armors;
    }

    private static List<Item> collectModifiers() {
        List<Item> modifiers = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = itemId(item);
            if (ReinhardtsHBM.MOD_ID.equals(id.getNamespace()) && item instanceof ArmorModItem) {
                modifiers.add(item);
            }
        }
        modifiers.sort(Comparator.comparing(item -> itemId(item).toString()));
        return modifiers;
    }

    private static ResourceLocation itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
