package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.CaveSpikeBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

/** Keeps sulfur and asbestos spike variants as separately placeable 1.7.10 stacks. */
public final class CaveSpikeBlockItem extends BlockItem {
    private static final String[] VARIANTS = {"sulfur", "asbestos"};

    public CaveSpikeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId() + "." + VARIANTS[CaveSpikeBlock.material(stack)]);
    }

    public static ItemStack stackFor(CaveSpikeBlockItem item, int material) {
        int clamped = Mth.clamp(material, 0, 1);
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(CaveSpikeBlock.MATERIAL_TAG, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(stackFor(this, 0));
        output.accept(stackFor(this, 1));
    }
}
