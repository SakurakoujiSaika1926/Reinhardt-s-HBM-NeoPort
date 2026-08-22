package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/** Exact 1.7.10 Telelinker workflow: bind a normal block, then apply it to a teleporter. */
public final class TeleLinkItem extends Item {
    private static final String TARGET_X = "x";
    private static final String TARGET_Y = "y";
    private static final String TARGET_Z = "z";
    private static final String TARGET_DIMENSION = "dim";

    public TeleLinkItem(Properties properties) {
        super(properties);
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
        CompoundTag tag = tag(stack);
        if (!tag.contains(TARGET_Y)) {
            tooltip.add(Component.translatable("item.reinhardtshbm.linker.no_target").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.literal("X: " + tag.getInt(TARGET_X)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Y: " + tag.getInt(TARGET_Y)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Z: " + tag.getInt(TARGET_Z)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("D: " + tag.getString(TARGET_DIMENSION)).withStyle(ChatFormatting.GRAY));
    }

    private static InteractionResult useOnTarget(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity target = level.getBlockEntity(pos);
        if (!level.isClientSide && target instanceof LegacyMachineBlockEntity machine
                && machine.machineId().equals("machine_teleporter")) {
            CompoundTag tag = tag(stack);
            if (!tag.contains(TARGET_Y)) {
                level.playSound(null, pos, HbmSoundEvents.TECH_BOOP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.linker.missing_target").withStyle(ChatFormatting.RED), false);
                return InteractionResult.FAIL;
            }
            BlockPos destination = new BlockPos(tag.getInt(TARGET_X), tag.getInt(TARGET_Y), tag.getInt(TARGET_Z));
            net.minecraft.resources.ResourceLocation dimension = net.minecraft.resources.ResourceLocation.tryParse(tag.getString(TARGET_DIMENSION));
            if (dimension == null) {
                level.playSound(null, pos, HbmSoundEvents.TECH_BOOP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.FAIL;
            }
            machine.setTeleporterTarget(destination, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
            level.playSound(null, pos, HbmSoundEvents.TECH_BLEEP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.linker.applied").withStyle(ChatFormatting.AQUA), false);
        } else if (!level.isClientSide) {
            CompoundTag tag = tag(stack);
            tag.putInt(TARGET_X, pos.getX());
            tag.putInt(TARGET_Y, pos.getY());
            tag.putInt(TARGET_Z, pos.getZ());
            tag.putString(TARGET_DIMENSION, level.dimension().location().toString());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            level.playSound(null, pos, HbmSoundEvents.TECH_BLEEP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.linker.saved", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), false);
        }
        player.swing(context.getHand());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
