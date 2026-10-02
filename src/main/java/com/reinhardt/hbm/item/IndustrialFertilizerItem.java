package com.reinhardt.hbm.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The 1.7.10 industrial fertilizer: one use attempts every growable block in
 * the 3x3x3 cube around the clicked position.  The clicked block is forced to
 * grow while the surrounding blocks retain their normal bonemeal chance.
 */
public final class IndustrialFertilizerItem extends Item {
    public IndustrialFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos origin = context.getClickedPos();
        if (context.getPlayer() != null
                && !context.getPlayer().mayUseItemAt(origin, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }

        boolean changed = fertilizeCube(stack, level, origin, context.getPlayer(), false);
        if (changed && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return changed ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    /** Dispenser entry point.  Consumption is performed here when requested. */
    public static boolean fertilizeCube(ItemStack stack, Level level, BlockPos origin,
                                        @Nullable Player player, boolean consume) {
        boolean foundTarget = false;
        for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
            boolean success = fertilize(stack, level, cursor.immutable(), player, cursor.equals(origin));
            foundTarget |= success;
            if (success && !level.isClientSide) {
                level.levelEvent(1505, cursor, 0);
            }
        }
        if (foundTarget && consume) {
            stack.shrink(1);
        }
        return foundTarget;
    }

    private static boolean fertilize(ItemStack stack, Level level, BlockPos pos,
                                     @Nullable Player player, boolean force) {
        BlockState state = level.getBlockState(pos);
        BonemealEvent event = new BonemealEvent(player, level, pos, state, stack);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return event.isSuccessful();
        }
        if (!(state.getBlock() instanceof BonemealableBlock growable)
                || (!event.isValidBonemealTarget() && !growable.isValidBonemealTarget(level, pos, state))) {
            return false;
        }
        if (level instanceof ServerLevel server
                && (force || growable.isBonemealSuccess(level, level.random, pos, state))) {
            growable.performBonemeal(server, level.random, pos, state);
        }
        // Legacy fertilizer counted a valid growable as a successful use even
        // when a surrounding block failed its random growth roll.
        return true;
    }
}
