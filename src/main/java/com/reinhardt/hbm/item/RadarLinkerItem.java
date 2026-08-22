package com.reinhardt.hbm.item;

import com.reinhardt.hbm.api.machine.RadarCommandReceiver;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RadarScreenBlockEntity;
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

/** Direct 1.7.10 ItemRadarLinker coordinate binding. */
public final class RadarLinkerItem extends Item {
    private static final String X = "posX";
    private static final String Y = "posY";
    private static final String Z = "posZ";

    public RadarLinkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockEntity entity = level.getBlockEntity(clicked);
        if (entity instanceof MachineDummyBlockEntity dummy) {
            entity = dummy.core();
        }
        if (!level.isClientSide && !(entity instanceof RadarScreenBlockEntity)
                && !(entity instanceof RadarCommandReceiver)) {
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.radar_linker.invalid_target")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && entity != null) {
            BlockPos target = entity.getBlockPos();
            CompoundTag tag = tag(context.getItemInHand());
            tag.putInt(X, target.getX());
            tag.putInt(Y, target.getY());
            tag.putInt(Z, target.getZ());
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            level.playSound(null, target, HbmSoundEvents.TECH_BLEEP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.radar_linker.saved",
                    target.getX(), target.getY(), target.getZ()).withStyle(ChatFormatting.AQUA), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(Y)) {
            tooltip.add(Component.translatable("item.reinhardtshbm.radar_linker.no_target").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.literal("X: " + tag.getInt(X)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Y: " + tag.getInt(Y)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Z: " + tag.getInt(Z)).withStyle(ChatFormatting.GRAY));
    }

    public static BlockPos position(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.contains(Y) ? new BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z)) : null;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
