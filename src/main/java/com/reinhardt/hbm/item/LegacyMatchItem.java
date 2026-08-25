package com.reinhardt.hbm.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

/** Direct port of 1.7.10 ItemMatch. */
public final class LegacyMatchItem extends Item {
    public LegacyMatchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayUseItemAt(target, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && level.isEmptyBlock(target)) {
            level.playSound(null, target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                    1.0F, level.random.nextFloat() * 0.4F + 0.8F);
            level.setBlock(target, BaseFireBlock.getState(level, target), 11);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
