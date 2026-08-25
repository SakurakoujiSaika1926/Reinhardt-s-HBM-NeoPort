package com.reinhardt.hbm.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GasMaskFilterItem extends Item {
    public GasMaskFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof FilterableGasMask)) {
            return InteractionResultHolder.pass(filter);
        }

        ItemStack oldFilter = GasMaskItem.getInstalledFilter(helmet, player.registryAccess());
        ItemStack installed = filter.copy();
        installed.setCount(1);
        if (!GasMaskItem.installFilter(helmet, installed, player)) {
            return InteractionResultHolder.pass(filter);
        }

        if (!level.isClientSide) {
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
            if (!oldFilter.isEmpty()) {
                filter.shrink(1);
                if (filter.isEmpty()) {
                    return InteractionResultHolder.success(oldFilter);
                }
                if (!player.getInventory().add(oldFilter)) {
                    player.drop(oldFilter, false);
                }
            } else if (!player.getAbilities().instabuild) {
                filter.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(filter, level.isClientSide);
    }
}
