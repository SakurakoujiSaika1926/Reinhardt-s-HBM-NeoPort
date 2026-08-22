package com.reinhardt.hbm.recipe.anvil;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AnvilIngredient {
    private final List<ItemStack> stacks;
    private final List<Item> items;
    private final List<TagKey<Item>> tags;
    private final int count;
    private final HbmFluidDefinition fluid;
    private final int fluidAmount;

    private AnvilIngredient(List<ItemStack> stacks, List<Item> items, List<TagKey<Item>> tags, int count) {
        this(stacks, items, tags, count, HbmFluids.none(), 0);
    }

    private AnvilIngredient(List<ItemStack> stacks, List<Item> items, List<TagKey<Item>> tags, int count, HbmFluidDefinition fluid, int fluidAmount) {
        this.stacks = stacks.stream().map(stack -> stack.copyWithCount(1)).toList();
        this.items = List.copyOf(items);
        this.tags = List.copyOf(tags);
        this.count = count;
        this.fluid = fluid == null ? HbmFluids.none() : fluid;
        this.fluidAmount = Math.max(0, fluidAmount);
    }

    public static AnvilIngredient of(ItemLike item, int count) {
        return new AnvilIngredient(List.of(), List.of(item.asItem()), List.of(), count);
    }

    public static Optional<AnvilIngredient> ofStacks(int count, ItemStack... stacks) {
        List<ItemStack> values = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                values.add(stack.copyWithCount(1));
            }
        }
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnvilIngredient(values, List.of(), List.of(), count));
    }

    public static Optional<AnvilIngredient> ofExisting(int count, ResourceLocation... itemIds) {
        List<Item> items = new ArrayList<>();
        for (ResourceLocation itemId : itemIds) {
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                items.add(BuiltInRegistries.ITEM.get(itemId));
            }
        }
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnvilIngredient(List.of(), items, List.of(), count));
    }

    public static Optional<AnvilIngredient> ofTag(int count, TagKey<Item> tag, ResourceLocation... displayItemIds) {
        List<Item> items = new ArrayList<>();
        for (ResourceLocation itemId : displayItemIds) {
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                items.add(BuiltInRegistries.ITEM.get(itemId));
            }
        }
        return Optional.of(new AnvilIngredient(List.of(), items, List.of(tag), count));
    }

    public static Optional<AnvilIngredient> ofFluid(HbmFluidDefinition fluid, int amount) {
        if (fluid == null || fluid.isNone() || amount <= 0) {
            return Optional.empty();
        }
        return Optional.of(new AnvilIngredient(List.of(), List.of(), List.of(), 0, fluid, amount));
    }

    public int count() {
        return this.count;
    }

    public boolean isFluid() {
        return !this.fluid.isNone() && this.fluidAmount > 0;
    }

    public int fluidAmount() {
        return this.fluidAmount;
    }

    public boolean matches(ItemStack stack) {
        if (isFluid()) {
            return fluidAmount(stack) >= this.fluidAmount;
        }
        return stack.getCount() >= this.count && matchesItem(stack);
    }

    public boolean matchesItem(ItemStack stack) {
        if (isFluid()) {
            return fluidAmount(stack) > 0;
        }
        if (stack.isEmpty()) {
            return false;
        }
        for (ItemStack accepted : this.stacks) {
            if (ItemStack.isSameItemSameComponents(stack, accepted)) {
                return true;
            }
        }
        for (Item item : this.items) {
            if (stack.is(item)) {
                return true;
            }
        }
        for (TagKey<Item> tag : this.tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public ItemStack displayStack() {
        if (isFluid()) {
            return FluidIconItem.forFluid(this.fluid, this.fluidAmount, 0);
        }
        if (!this.stacks.isEmpty()) {
            return this.stacks.getFirst().copyWithCount(this.count);
        }
        if (this.items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(this.items.getFirst(), this.count);
    }

    public List<ItemStack> displayStacks() {
        if (isFluid()) {
            return List.of(displayStack());
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : this.stacks) {
            stacks.add(stack.copyWithCount(this.count));
        }
        for (Item item : this.items) {
            stacks.add(new ItemStack(item, this.count));
        }
        return stacks;
    }

    public List<ItemStack> searchStacks() {
        if (isFluid()) {
            return List.of(displayStack());
        }
        if (!this.stacks.isEmpty()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : this.stacks) {
                stacks.add(stack.copyWithCount(this.count));
            }
            return stacks;
        }
        Set<Item> searchItems = new LinkedHashSet<>(this.items);
        for (TagKey<Item> tag : this.tags) {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                searchItems.add(holder.value());
            }
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : searchItems) {
            stacks.add(new ItemStack(item, this.count));
        }
        return stacks;
    }

    public Component displayName() {
        if (isFluid()) {
            return Component.translatable(this.fluid.translationKey());
        }
        ItemStack display = displayStack();
        if (display.isEmpty()) {
            return Component.empty();
        }
        return display.getHoverName();
    }

    public Component describeLine() {
        if (isFluid()) {
            return Component.literal(">" + this.fluidAmount + "mB ").append(displayName());
        }
        return Component.literal(">" + this.count + "x ").append(displayName());
    }

    boolean consumeFrom(List<ItemStack> stacks) {
        if (isFluid()) {
            return consumeFluidFrom(stacks);
        }
        int needed = this.count;
        for (ItemStack stack : stacks) {
            if (!matchesItem(stack)) {
                continue;
            }

            int taken = Math.min(needed, stack.getCount());
            stack.shrink(taken);
            needed -= taken;
            if (needed <= 0) {
                return true;
            }
        }
        return false;
    }

    private int fluidAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof InfiniteFluidContainerItem infinite && infinite.fluid() == this.fluid) {
            return Integer.MAX_VALUE;
        }
        if (!(stack.getItem() instanceof HbmFluidContainerItem container) || !container.isFilledContainer()) {
            return 0;
        }
        if (HbmFluidContainerItem.fluid(stack) != this.fluid) {
            return 0;
        }
        return container.kind().capacity() * stack.getCount();
    }

    private boolean consumeFluidFrom(List<ItemStack> stacks) {
        int needed = this.fluidAmount;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof InfiniteFluidContainerItem infinite && infinite.fluid() == this.fluid) {
                return true;
            }
        }

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof HbmFluidContainerItem container) || !container.isFilledContainer()) {
                continue;
            }
            if (HbmFluidContainerItem.fluid(stack) != this.fluid) {
                continue;
            }
            int unit = container.kind().capacity();
            while (stack.getCount() > 0 && needed >= unit) {
                stack.shrink(1);
                needed -= unit;
            }
            if (needed <= 0) {
                return true;
            }
        }
        if (needed > 0) {
            return false;
        }
        return true;
    }
}
