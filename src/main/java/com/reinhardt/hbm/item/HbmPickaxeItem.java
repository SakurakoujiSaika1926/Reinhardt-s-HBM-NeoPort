package com.reinhardt.hbm.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.List;

public class HbmPickaxeItem extends PickaxeItem {
    protected final HbmToolProfile profile;

    public HbmPickaxeItem(HbmToolProfile profile) {
        this(profile, HbmToolBehavior.properties(profile));
    }

    public HbmPickaxeItem(HbmToolProfile profile, Item.Properties properties) {
        super(profile.tier(), properties);
        this.profile = profile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!canOperate(player.getItemInHand(usedHand))) {
            return super.use(level, player, usedHand);
        }
        InteractionResultHolder<ItemStack> result = HbmToolBehavior.use(level, player, usedHand, this.profile);
        return result.getResult().consumesAction() ? result : super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = HbmToolBehavior.useOn(context, this.profile);
        return result.consumesAction() ? result : super.useOn(context);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!canOperate(stack)) {
            return 1.0F;
        }
        float minerSpeed = HbmToolBehavior.minerDestroySpeed(stack, state, this.profile);
        return minerSpeed > 1.0F ? minerSpeed : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!canOperate(stack)) {
            return false;
        }
        return HbmToolBehavior.isCorrectMinerTool(state, this.profile.tier(), this.profile.miner())
                || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean operated = canOperate(stack);
        boolean result = super.mineBlock(stack, level, state, pos, miningEntity);
        if (operated) {
            HbmToolBehavior.afterMine(stack, level, state, pos, miningEntity, this.profile);
            if (!level.isClientSide && !state.isAir() && state.getDestroySpeed(level, pos) != 0.0F) {
                consumeOperation(stack);
            }
        }
        return result;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (canOperate(stack)) {
            HbmToolBehavior.postHurt(stack, target, attacker, this.profile);
            if (!attacker.level().isClientSide) {
                consumeOperation(stack);
            }
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    protected boolean canOperate(ItemStack stack) {
        return true;
    }

    public boolean canBreakDepthRock() {
        return this.profile.depthRockBreaker();
    }

    protected void consumeOperation(ItemStack stack) {
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
