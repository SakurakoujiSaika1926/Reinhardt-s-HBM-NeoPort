package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The red-key secret-room lock from 1.7.10. */
public final class ForgottenLockBlock extends LegacyPillarBlockBase {
    public ForgottenLockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean cracked = id.equals("key_red_cracked");
        if ((!id.equals("key_red") && !cracked) || hitResult.getDirection().getAxis().isVertical()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            if (cracked && !player.isCreative()) {
                stack.shrink(1);
            }
            generate(level, pos, hitResult.getDirection());
            level.playSound(null, pos, HbmSoundEvents.LOCK_OPEN.get(), net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void generate(Level level, BlockPos origin, Direction direction) {
        Direction rotation = direction.getClockWise();
        for (int width = -2; width <= 2; width++) {
            for (int height = -2; height <= 2; height++) {
                for (int depth = 0; depth < 15; depth++) {
                    boolean shell = width == -2 || width == 2 || height == -2 || height == 2 || depth == 14;
                    BlockPos target = origin.relative(direction, -depth)
                            .relative(rotation, width)
                            .above(height);
                    level.setBlock(target, shell
                            ? HbmBlocks.BRICK_FORGOTTEN.get().defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
