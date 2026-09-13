package com.reinhardt.hbm.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.reinhardt.hbm.util.ArmorModHandler;

public class GasMaskFilterItem extends Item {
    public GasMaskFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack target = helmet;
        if (!(target.getItem() instanceof FilterableGasMask)) {
            // 1.7.10 ItemFilter also searched the helmet_only armor-mod slot
            // (the common attachment_mask/attachment_mask_mono path).
            for (ItemStack mod : ArmorModHandler.pryMods(helmet, player.registryAccess())) {
                if (mod.getItem() instanceof FilterableGasMask
                        && mod.getItem() instanceof ArmorModItem
                        && ((ArmorModItem) mod.getItem()).slotType() == ArmorModHandler.HELMET_ONLY) {
                    target = mod;
                    break;
                }
            }
        }
        if (target.isEmpty() || !(target.getItem() instanceof FilterableGasMask)) {
            return InteractionResultHolder.pass(filter);
        }

        ItemStack oldFilter = GasMaskItem.getInstalledFilter(target, player.registryAccess());
        ItemStack installed = filter.copy();
        installed.setCount(1);
        if (!GasMaskItem.installFilter(target, installed, player)) {
            return InteractionResultHolder.pass(filter);
        }
        if (target != helmet) {
            ArmorModHandler.applyMod(helmet, target, player.registryAccess());
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
