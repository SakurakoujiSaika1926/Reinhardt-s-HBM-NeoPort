package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LittleBoyKitItem extends Item {
    public LittleBoyKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            give(player, new ItemStack(HbmBlocks.NUKE_BOY.get()));
            give(player, new ItemStack(HbmItems.BOY_SHIELDING.get()));
            give(player, new ItemStack(HbmItems.BOY_TARGET.get()));
            give(player, new ItemStack(HbmItems.BOY_BULLET.get()));
            give(player, new ItemStack(HbmItems.BOY_PROPELLANT.get()));
            give(player, new ItemStack(HbmItems.BOY_IGNITER.get()));
            giveHazmat(player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.ITEM_UNPACK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void giveHazmat(Player player) {
        equip(player, EquipmentSlot.HEAD, new ItemStack(HbmItems.HAZMAT_HELMET.get()));
        equip(player, EquipmentSlot.CHEST, new ItemStack(HbmItems.HAZMAT_PLATE.get()));
        equip(player, EquipmentSlot.LEGS, new ItemStack(HbmItems.HAZMAT_LEGS.get()));
        equip(player, EquipmentSlot.FEET, new ItemStack(HbmItems.HAZMAT_BOOTS.get()));
    }

    private static void equip(Player player, EquipmentSlot slot, ItemStack replacement) {
        ItemStack existing = player.getItemBySlot(slot);
        if (!existing.isEmpty()) {
            player.drop(existing.copy(), false);
        }
        player.setItemSlot(slot, replacement);
    }
}
