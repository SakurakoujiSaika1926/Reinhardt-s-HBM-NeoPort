package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.TrappedBrickBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Keeps the 1.7.10 trap metadata on a 1.21.1 item stack. */
public final class TrapBlockItem extends BlockItem {
    private static final String TRAP_TAG = "trap";

    public TrapBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack stackFor(TrapBlockItem item, int trap) {
        int value = Mth.clamp(trap, 0, TrappedBrickBlock.MAX_TRAP);
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(TRAP_TAG, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
        return stack;
    }

    public static int trap(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Mth.clamp(tag.getInt(TRAP_TAG), 0, TrappedBrickBlock.MAX_TRAP);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int trap = 0; trap <= TrappedBrickBlock.MAX_TRAP; trap++) {
            output.accept(stackFor(this, trap));
        }
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        return getBlock().defaultBlockState().setValue(TrappedBrickBlock.TRAP, trap(context.getItemInHand()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.brick_jungle_trap." + trap(stack))
                .withStyle(ChatFormatting.GRAY));
    }
}
