package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.HbmPickaxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Depth rock keeps the legacy depth-rock pickaxe bonus without making the block unbreakable in modern play. */
public class DepthRockBlock extends Block {
    public DepthRockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof HbmPickaxeItem pickaxe && pickaxe.canBreakDepthRock()) {
            return 1.0F / 50.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
