package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.ObjMachineItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Metadata-preserving item for the four legacy CRT screen variants. */
public final class DecoCrtBlockItem extends BlockItem {
    private static final String VARIANT_TAG = "variant";
    private static final String[] VARIANTS = {"crt_clean", "crt_broken", "crt_blinking", "crt_bsod"};

    public DecoCrtBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.deco_crt");
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(com.reinhardt.hbm.block.DecoCrtBlock.VARIANT, variantIndex(context.getItemInHand()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int variant = 0; variant < VARIANTS.length; variant++) {
            output.accept(stackFor(this, variant));
        }
    }

    public int variantIndex(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            String value = tag.getString(VARIANT_TAG);
            for (int index = 0; index < VARIANTS.length; index++) {
                if (VARIANTS[index].equals(value)) {
                    return index;
                }
            }
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? 0 : Math.max(0, Math.min(VARIANTS.length - 1, modelData.value()));
    }

    public String variantId(ItemStack stack) {
        return VARIANTS[variantIndex(stack)];
    }

    public static ItemStack stackFor(DecoCrtBlockItem item, int variant) {
        int clamped = Math.max(0, Math.min(VARIANTS.length - 1, variant));
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString(VARIANT_TAG, VARIANTS[clamped]);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
