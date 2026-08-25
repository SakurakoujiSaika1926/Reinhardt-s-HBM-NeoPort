package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** 1.7.10 ItemDesingatorRange coordinate selection behaviour. */
public class LegacyRangeDesignatorItem extends Item {
    private static final String X = "xCoord";
    private static final String Z = "zCoord";

    public LegacyRangeDesignatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(300.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            BlockPos target = blockHit.getBlockPos();
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(X, target.getX());
            tag.putInt(Z, target.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.designator_range.set", target.getX(), target.getZ()), true);
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(X) || !tag.contains(Z)) {
            tooltip.add(Component.translatable("item.reinhardtshbm.designator_range.no_target").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("item.reinhardtshbm.designator_range.target", tag.getInt(X), tag.getInt(Z)).withStyle(ChatFormatting.YELLOW));
    }

    public static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(X) && tag.contains(Z) ? new BlockPos(tag.getInt(X), 0, tag.getInt(Z)) : null;
    }
}
