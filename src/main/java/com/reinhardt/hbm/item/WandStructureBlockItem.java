package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.WandStructureBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class WandStructureBlockItem extends BlockItem {
    public WandStructureBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public ItemStack stack(boolean load) {
        ItemStack stack = new ItemStack(this);
        if (load) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("load", true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        }
        return stack;
    }

    public void addCreativeVariants(Consumer<ItemStack> output) {
        output.accept(stack(false));
        output.accept(stack(true));
    }

    @Override
    public Component getName(ItemStack stack) {
        boolean load = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("load");
        return Component.translatable(load
                ? "block.reinhardtshbm.wand_structure.load"
                : "block.reinhardtshbm.wand_structure.save");
    }
}
