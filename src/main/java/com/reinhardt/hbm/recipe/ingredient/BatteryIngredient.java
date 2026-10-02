package com.reinhardt.hbm.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Component-aware battery ingredient which deliberately excludes stored charge from matching.
 * Battery model/variant components are still checked, so a lead battery cannot substitute for a
 * lithium battery, while empty, partially charged and full instances of that model remain equal.
 */
public final class BatteryIngredient implements ICustomIngredient {
    private static final String CHARGE_TAG = "charge";

    public static final MapCodec<BatteryIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false)
                    .fieldOf("items")
                    .forGetter(BatteryIngredient::items),
            DataComponentPredicate.CODEC.fieldOf("components").forGetter(BatteryIngredient::components)
    ).apply(instance, BatteryIngredient::new));

    private final HolderSet<Item> items;
    private final DataComponentPredicate components;
    private final ItemStack[] displayStacks;

    public BatteryIngredient(HolderSet<Item> items, DataComponentPredicate components) {
        this.items = items;
        this.components = components;
        this.displayStacks = items.stream()
                .map(holder -> new ItemStack(holder, 1, components.asPatch()))
                .toArray(ItemStack[]::new);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()
                || !this.items.contains(stack.getItemHolder())
                || !BatteryPackItem.isBattery(stack)) {
            return false;
        }
        return this.components.test(withoutStoredCharge(stack));
    }

    private static ItemStack withoutStoredCharge(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return stack;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(CHARGE_TAG)) {
            return stack;
        }
        ItemStack copy = stack.copy();
        tag.remove(CHARGE_TAG);
        if (tag.isEmpty()) {
            copy.remove(DataComponents.CUSTOM_DATA);
        } else {
            copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return copy;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(this.displayStacks).map(ItemStack::copy);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return HbmRecipeTypes.BATTERY_INGREDIENT.get();
    }

    public HolderSet<Item> items() {
        return this.items;
    }

    public DataComponentPredicate components() {
        return this.components;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BatteryIngredient that)) {
            return false;
        }
        return Objects.equals(this.items, that.items)
                && Objects.equals(this.components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.items, this.components);
    }
}
