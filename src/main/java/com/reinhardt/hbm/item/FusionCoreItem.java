package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Direct 1.7.10 ItemFusionCore port. */
public final class FusionCoreItem extends Item {
    private final long charge;

    public FusionCoreItem(Properties properties, long charge) {
        super(properties);
        this.charge = charge;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack core = player.getItemInHand(hand);
        if (!ArmorFSBItem.hasFSBArmor(player)) {
            return InteractionResultHolder.pass(core);
        }

        boolean charged = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof HbmChargeableItem battery) {
                long stored = battery.hbmCharge(armor);
                long target = Math.min(battery.hbmCapacity(armor), stored + this.charge);
                if (target > stored) {
                    battery.hbmSetCharge(armor, target);
                    charged = true;
                }
            }
        }

        if (!charged) {
            return InteractionResultHolder.pass(core);
        }
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                core.shrink(1);
            }
            level.playSound(null, player.blockPosition(), HbmSoundEvents.SUIT_BATTERY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(core, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.fusion_core.charge", BatteryPackItem.formatShortNumber(this.charge))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.fusion_core.requirement").withStyle(ChatFormatting.GRAY));
    }
}
