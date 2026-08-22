package com.reinhardt.hbm.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class HbmSwordItem extends SwordItem {
    private final HbmToolProfile profile;

    public HbmSwordItem(HbmToolProfile profile) {
        super(profile.tier(), HbmToolBehavior.properties(profile));
        this.profile = profile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        InteractionResultHolder<ItemStack> result = HbmToolBehavior.use(level, player, usedHand, this.profile);
        return result.getResult().consumesAction() ? result : super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = HbmToolBehavior.useOn(context, this.profile);
        return result.consumesAction() ? result : super.useOn(context);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HbmToolBehavior.postHurt(stack, target, attacker, this.profile);
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HbmToolBehavior.addTooltip(stack, tooltip, flag, this.profile);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return HbmToolBehavior.isFoil(stack, this.profile) || super.isFoil(stack);
    }
}
