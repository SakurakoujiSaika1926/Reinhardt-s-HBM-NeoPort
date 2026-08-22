package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.WandLogicBlockEntity;
import com.reinhardt.hbm.blockentity.WandLootBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class LegacyDetonatorItem extends Item {
    private final String legacyId;

    public LegacyDetonatorItem(Properties properties, String legacyId) {
        super(properties.stacksTo(1));
        this.legacyId = legacyId;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return triggerWandBlock(context.getLevel(), context.getClickedPos());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return triggerWandBlock(context.getLevel(), context.getClickedPos());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.legacy_placeholder.hint", this.legacyId)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static InteractionResult triggerWandBlock(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WandLootBlockEntity loot) {
            if (!level.isClientSide) {
                loot.triggerReplace();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockEntity instanceof WandLogicBlockEntity logic) {
            if (!level.isClientSide) {
                logic.triggerReplace();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
