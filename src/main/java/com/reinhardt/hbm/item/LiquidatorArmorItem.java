package com.reinhardt.hbm.item;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/** 1.7.10 Liquidator suit, including its unrestricted helmet filter slot. */
public final class LiquidatorArmorItem extends ArmorFSBItem implements FilterableGasMask {
    private final boolean filterHelmet;

    public LiquidatorArmorItem(
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            boolean filterHelmet,
            Properties properties
    ) {
        super("liquidator", material, type, false, List.of(), properties);
        this.filterHelmet = filterHelmet;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.filterHelmet || !player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack filter = GasMaskItem.getInstalledFilter(stack, player.registryAccess());
        if (filter.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        GasMaskItem.removeFilter(stack);
        if (!level.isClientSide) {
            if (!player.getInventory().add(filter)) {
                player.drop(filter, false);
            }
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.filterHelmet) {
            ItemStack filter = GasMaskItem.getInstalledFilterForTooltip(stack);
            if (filter.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask.no_filter").withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask.installed_filter").withStyle(ChatFormatting.GOLD));
                int maxDamage = filter.getMaxDamage();
                if (maxDamage > 0) {
                    int percent = Math.max(0, (maxDamage - filter.getDamageValue()) * 100 / maxDamage);
                    tooltip.add(Component.literal("  ").append(filter.getHoverName()).append(" (" + percent + "%)").withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.literal("  ").append(filter.getHoverName()).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public Set<HbmArmorProtection.HazardClass> blacklist() {
        // ArmorLiquidatorMask returned an empty blacklist in 1.7.10.
        return Set.of();
    }

    @Override
    public boolean isFilterApplicable(ItemStack filter) {
        return this.filterHelmet && filter.getItem() instanceof GasMaskFilterItem;
    }
}
