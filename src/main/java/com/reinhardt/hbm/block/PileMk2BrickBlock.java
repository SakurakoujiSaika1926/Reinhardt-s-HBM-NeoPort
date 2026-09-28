package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PileMk2CoreBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Current-upstream Chicago Pile MK2 construction brick. */
public final class PileMk2BrickBlock extends Block {
    public static final int MIN_VERTICAL_SIZE = 5;
    public static final int MIN_HORIZONTAL_SIZE = 5;
    public static final int MAX_VERTICAL_SIZE = 15;
    public static final int MAX_HORIZONTAL_SIZE = 15;

    public PileMk2BrickBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 30;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!ScrewdriverItem.isHandDrill(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!hit.getDirection().getAxis().isHorizontal()) {
            return ItemInteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        boolean formed = formPile(level, pos, hit.getDirection().getOpposite(), player);
        if (formed) {
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return ItemInteractionResult.CONSUME;
    }

    private static boolean formPile(Level level, BlockPos corePos, Direction inward, Player player) {
        Direction leftDirection = inward.getCounterClockWise();
        int negativeHeight = 0;
        int positiveHeight = 0;
        int left = 0;
        int right = 0;
        int depth = 0;

        for (int step = 1; step <= MAX_VERTICAL_SIZE - 1; step++) {
            if (!level.getBlockState(corePos.above(step)).is(HbmBlocks.PILE_BRICK.get())) break;
            positiveHeight = step;
        }
        for (int step = 1; step <= MAX_VERTICAL_SIZE - positiveHeight - 1; step++) {
            if (!level.getBlockState(corePos.below(step)).is(HbmBlocks.PILE_BRICK.get())) break;
            negativeHeight = step;
        }
        for (int step = 1; step <= MAX_HORIZONTAL_SIZE - 1; step++) {
            if (!level.getBlockState(corePos.relative(leftDirection, step)).is(HbmBlocks.PILE_BRICK.get())) break;
            left = step;
        }
        for (int step = 1; step <= MAX_HORIZONTAL_SIZE - left - 1; step++) {
            if (!level.getBlockState(corePos.relative(leftDirection.getOpposite(), step)).is(HbmBlocks.PILE_BRICK.get())) break;
            right = step;
        }
        for (int step = 1; step <= MAX_HORIZONTAL_SIZE; step++) {
            if (!level.getBlockState(corePos.relative(inward, step)).is(HbmBlocks.PILE_BRICK.get())) break;
            depth = step;
        }

        if (positiveHeight + negativeHeight + 1 < MIN_VERTICAL_SIZE) {
            error(player, "message.reinhardtshbm.pile_mk2.height", MIN_VERTICAL_SIZE);
            return false;
        }
        if (left + right + 1 < MIN_HORIZONTAL_SIZE) {
            error(player, "message.reinhardtshbm.pile_mk2.width", MIN_HORIZONTAL_SIZE);
            return false;
        }
        if (depth + 1 < MIN_HORIZONTAL_SIZE) {
            error(player, "message.reinhardtshbm.pile_mk2.depth", MIN_HORIZONTAL_SIZE);
            return false;
        }
        if (positiveHeight == 0 || negativeHeight == 0 || left == 0 || right == 0) {
            error(player, "message.reinhardtshbm.pile_mk2.core_edge");
            return false;
        }

        BlockPos min = corePos;
        BlockPos max = corePos;
        for (int vertical = -negativeHeight; vertical <= positiveHeight; vertical++) {
            for (int lateral = -left; lateral <= right; lateral++) {
                for (int forward = 0; forward <= depth; forward++) {
                    BlockPos check = offset(corePos, inward, leftDirection, vertical, lateral, forward);
                    if (!level.getBlockState(check).is(HbmBlocks.PILE_BRICK.get())) {
                        player.displayClientMessage(Component.translatable(
                                "message.reinhardtshbm.pile_mk2.missing",
                                check.getX(), check.getY(), check.getZ()), true);
                        return false;
                    }
                    min = new BlockPos(Math.min(min.getX(), check.getX()), Math.min(min.getY(), check.getY()),
                            Math.min(min.getZ(), check.getZ()));
                    max = new BlockPos(Math.max(max.getX(), check.getX()), Math.max(max.getY(), check.getY()),
                            Math.max(max.getZ(), check.getZ()));
                }
            }
        }

        for (int vertical = -negativeHeight; vertical <= positiveHeight; vertical++) {
            for (int lateral = -left; lateral <= right; lateral++) {
                for (int forward = 0; forward <= depth; forward++) {
                    BlockPos place = offset(corePos, inward, leftDirection, vertical, lateral, forward);
                    PileMk2Block.Kind kind;
                    if (place.equals(corePos)) {
                        kind = PileMk2Block.Kind.CORE;
                    } else {
                        int edgeCount = 0;
                        if (vertical == -negativeHeight || vertical == positiveHeight) edgeCount++;
                        if (lateral == -left || lateral == right) edgeCount++;
                        if (forward == 0 || forward == depth) edgeCount++;
                        kind = edgeCount > 1 ? PileMk2Block.Kind.EDGE : PileMk2Block.Kind.DUMMY;
                    }
                    level.setBlock(place, HbmBlocks.PILE_BLOCK.get().defaultBlockState()
                            .setValue(PileMk2Block.KIND, kind), Block.UPDATE_ALL);
                }
            }
        }

        if (!(level.getBlockEntity(corePos) instanceof PileMk2CoreBlockEntity core)) {
            // Never leave a half-formed pile if another mod vetoes creation of the core BE.
            for (BlockPos restore : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockState(restore).is(HbmBlocks.PILE_BLOCK.get())) {
                    level.setBlock(restore, HbmBlocks.PILE_BRICK.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            error(player, "message.reinhardtshbm.pile_mk2.core_missing");
            return false;
        }

        core.setupStructure(inward, positiveHeight, negativeHeight, left, right, depth + 1, min, max);
        level.playSound(null, corePos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 1.25F);
        player.displayClientMessage(Component.translatable(
                "message.reinhardtshbm.pile_mk2.formed",
                positiveHeight + negativeHeight + 1,
                left + right + 1,
                depth + 1), true);
        return true;
    }

    private static BlockPos offset(BlockPos origin, Direction inward, Direction leftDirection,
                                   int vertical, int lateral, int forward) {
        return origin.offset(
                -leftDirection.getStepX() * lateral + inward.getStepX() * forward,
                vertical,
                -leftDirection.getStepZ() * lateral + inward.getStepZ() * forward
        );
    }

    private static void error(Player player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }
}
