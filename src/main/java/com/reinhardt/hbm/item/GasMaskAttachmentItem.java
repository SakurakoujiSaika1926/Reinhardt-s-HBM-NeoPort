package com.reinhardt.hbm.item;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/** Filter-bearing 1.7.10 gas-mask attachments installed in a helmet slot. */
public final class GasMaskAttachmentItem extends ArmorModItem implements FilterableGasMask {
    private final boolean mono;

    public GasMaskAttachmentItem(Item.Properties properties, boolean mono) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
        this.mono = mono;
    }

    @Override
    public Set<HbmArmorProtection.HazardClass> blacklist() {
        return this.mono
                ? Set.of(HbmArmorProtection.HazardClass.GAS_LUNG, HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.BACTERIA)
                : Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING);
    }

    @Override
    public boolean isFilterApplicable(ItemStack filter) {
        return filter.getItem() instanceof GasMaskFilterItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
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
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask_attachment").withStyle(ChatFormatting.GREEN));
        ItemStack filter = GasMaskItem.getInstalledFilter(stack, context.registries());
        if (filter.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask.no_filter").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask.installed_filter").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("  ").append(filter.getHoverName()).withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
