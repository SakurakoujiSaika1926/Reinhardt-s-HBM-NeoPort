package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class TurretBiometryItem extends Item {
    private static final String PLAYER_COUNT = "playercount";
    private static final String PLAYER_PREFIX = "player_";

    public TurretBiometryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(names(stack).stream().map(Component::literal).toList());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        addName(stack, player.getDisplayName().getString());
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.addpldata"), false);
        }
        player.swing(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static List<String> names(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int count = Math.max(0, tag.getInt(PLAYER_COUNT));
        List<String> names = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            names.add(tag.getString(PLAYER_PREFIX + index));
        }
        return names;
    }

    public static void writeNames(ItemStack stack, List<String> names) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(PLAYER_COUNT, names.size());
        for (int index = 0; index < names.size(); index++) {
            tag.putString(PLAYER_PREFIX + index, names.get(index));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void addName(ItemStack stack, String name) {
        List<String> names = new ArrayList<>(names(stack));
        if (!names.contains(name)) {
            names.add(name);
            writeNames(stack, names);
        }
    }
}
