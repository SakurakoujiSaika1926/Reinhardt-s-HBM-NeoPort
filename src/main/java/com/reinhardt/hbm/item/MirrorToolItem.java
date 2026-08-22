package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.SolarMirrorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class MirrorToolItem extends Item {
    private static final String POS_X = "posX";
    private static final String POS_Y = "posY";
    private static final String POS_Z = "posZ";

    public MirrorToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnTarget(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnTarget(context.getItemInHand(), context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.reinhardtshbm.mirror_tool.1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("desc.reinhardtshbm.mirror_tool.2").withStyle(ChatFormatting.YELLOW));
        BlockPos target = target(stack);
        if (target != null) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.mirror_tool.target", target.getX(), target.getY(), target.getZ())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static InteractionResult useOnTarget(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos corePos = resolveCore(level, clickedPos);
        if (level.getBlockState(corePos).is(HbmBlocks.MACHINE_SOLAR_BOILER.get())) {
            if (!level.isClientSide) {
                rememberTarget(stack, corePos.above());
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.mirror_tool.linked")
                        .withStyle(ChatFormatting.YELLOW), true);
                level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity instanceof SolarMirrorBlockEntity mirror) {
            BlockPos target = target(stack);
            if (target == null) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide && clickedPos.distSqr(target) < 625.0D) {
                mirror.setTarget(target);
                level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private static void rememberTarget(ItemStack stack, BlockPos target) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(POS_X, target.getX());
        tag.putInt(POS_Y, target.getY());
        tag.putInt(POS_Z, target.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(POS_X) || !tag.contains(POS_Y) || !tag.contains(POS_Z)) {
            return null;
        }
        return new BlockPos(tag.getInt(POS_X), tag.getInt(POS_Y), tag.getInt(POS_Z));
    }

    private static BlockPos resolveCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return pos;
    }
}
