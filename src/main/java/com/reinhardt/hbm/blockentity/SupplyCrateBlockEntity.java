package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class SupplyCrateBlockEntity extends BlockEntity {
    private static final String ITEM_DATA_KEY = "supply_crate_data";
    private static final String ITEMS_KEY = "items";

    private final List<ItemStack> items = new ArrayList<>();

    public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SUPPLY_CRATE.get(), pos, state);
    }

    public void setContents(List<ItemStack> contents) {
        this.items.clear();
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                this.items.add(stack.copy());
            }
        }
        setChanged();
    }

    public List<ItemStack> takeContents() {
        List<ItemStack> contents = this.items.stream().map(ItemStack::copy).toList();
        this.items.clear();
        setChanged();
        return contents;
    }

    public void loadFromItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        this.items.clear();
        if (root.contains(ITEM_DATA_KEY, Tag.TAG_COMPOUND)) {
            readItems(root.getCompound(ITEM_DATA_KEY), registries);
        }
        setChanged();
    }

    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag data = writeItems(registries);
        if (data.isEmpty()) {
            return;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(ITEM_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ITEMS_KEY, writeItems(registries).getList(ITEMS_KEY, Tag.TAG_COMPOUND));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        readItems(tag, registries);
    }

    private CompoundTag writeItems(HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                list.add(stack.saveOptional(registries));
            }
        }
        if (!list.isEmpty()) {
            data.put(ITEMS_KEY, list);
        }
        return data;
    }

    private void readItems(CompoundTag data, HolderLookup.Provider registries) {
        ListTag list = data.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                this.items.add(stack);
            }
        }
    }
}
