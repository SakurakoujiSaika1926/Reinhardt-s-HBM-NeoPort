package com.reinhardt.hbm.item;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class HbmAxeItem extends AxeItem {
    private final HbmToolProfile profile;

    public HbmAxeItem(HbmToolProfile profile) {
        super(profile.tier(), HbmToolBehavior.properties(profile));
        this.profile = profile;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (!level.isClientSide && selected
                && this.profile.specialBehavior() == HbmToolBehavior.SpecialBehavior.SHIMMER_AXE
                && entity instanceof Player player
                && player.getItemBySlot(EquipmentSlot.CHEST).is(HbmItems.JACKT2.get())) {
            HbmAdvancements.award(player, "fiend2");
        }
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
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean result = super.mineBlock(stack, level, state, pos, miningEntity);
        HbmToolBehavior.afterMine(stack, level, state, pos, miningEntity, this.profile);
        return result;
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
