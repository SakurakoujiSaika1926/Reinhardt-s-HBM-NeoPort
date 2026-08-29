package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.HbmPickaxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The 1.7.10 depth-rock rule: only depth-rock tools can break it. */
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
        return 0.0F;
    }
}
