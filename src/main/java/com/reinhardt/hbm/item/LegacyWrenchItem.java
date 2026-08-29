package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.PipeAnchorBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.7.10 pipe-wrench selection behavior for the long-distance pipe anchors.
 */
public final class LegacyWrenchItem extends Item {
    private static final String ANCHOR = "pipe_anchor";

    public LegacyWrenchItem(Properties properties) {
        super(properties.stacksTo(1).durability(1000));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (!(level.getBlockEntity(clicked) instanceof PipeAnchorBlockEntity)) {
            return InteractionResult.PASS;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(ANCHOR)) {
            tag.putLong(ANCHOR, clicked.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (!level.isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.reinhardtshbm.wrench.start"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos firstPos = BlockPos.of(tag.getLong(ANCHOR));
        tag.remove(ANCHOR);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (!(level.getBlockEntity(firstPos) instanceof PipeAnchorBlockEntity first)
                || !(level.getBlockEntity(clicked) instanceof PipeAnchorBlockEntity second)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (first == second) {
            return pipeError(level, context, "message.reinhardtshbm.wrench.same");
        }

        HbmFluidDefinition firstType = first.type();
        HbmFluidDefinition secondType = second.type();
        if (firstType.isNone() && !secondType.isNone()) {
            first.setType(secondType);
            firstType = secondType;
        } else if (secondType.isNone() && !firstType.isNone()) {
            second.setType(firstType);
            secondType = firstType;
        }
        if (firstType != secondType) {
            return pipeError(level, context, "message.reinhardtshbm.wrench.type_error");
        }

        if (firstPos.distSqr(clicked) > 100.0D) {
            return pipeError(level, context, "message.reinhardtshbm.wrench.distance");
        }

        if (!level.isClientSide) {
            first.addLink(clicked);
            second.addLink(firstPos);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.reinhardtshbm.wrench.end"), true);
            }
            level.playSound(null, clicked, HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult pipeError(Level level, UseOnContext context, String key) {
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable(key), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.wrench.use").withStyle(ChatFormatting.GRAY));
        if (tag.contains(ANCHOR)) {
            BlockPos pos = BlockPos.of(tag.getLong(ANCHOR));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.wrench.anchor", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

}
