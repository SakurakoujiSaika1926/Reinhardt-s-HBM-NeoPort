package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
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
 * 1.7.10 pipe-wrench selection behavior. Modern ducts connect by adjacent
 * topology, so the second click refreshes both endpoint components instead of
 * creating the removed long-distance TileEntityPipelineBase link.
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
        if (!(level.getBlockEntity(clicked) instanceof FluidPipeBlockEntity)) {
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
        if (!(level.getBlockEntity(firstPos) instanceof FluidPipeBlockEntity first)
                || !(level.getBlockEntity(clicked) instanceof FluidPipeBlockEntity second)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        HbmFluidDefinition firstType = first.type();
        HbmFluidDefinition secondType = second.type();
        boolean compatible = firstType.isNone() || secondType.isNone() || firstType == secondType
                || first.isExhaustPipe() && second.isExhaustPipe();
        if (!compatible) {
            if (!level.isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.reinhardtshbm.wrench.type_error"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            refresh(level, firstPos);
            refresh(level, clicked);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.reinhardtshbm.wrench.end"), true);
            }
            level.playSound(null, clicked, HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
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

    private static void refresh(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof FluidDuctBlock duct) {
            duct.refreshConnections(level, pos);
        }
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.markNetworkChanged();
        }
    }
}
