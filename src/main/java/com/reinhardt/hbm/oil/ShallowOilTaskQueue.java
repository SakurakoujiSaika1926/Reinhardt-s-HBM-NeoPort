package com.reinhardt.hbm.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public final class ShallowOilTaskQueue {
    public static final long[] EMPTY = new long[0];

    private ShallowOilTaskQueue() {
    }

    public static long[] build(
            Level level,
            BlockPos startPos,
            int nodeLimit,
            Predicate<BlockState> searchBlock,
            Predicate<BlockState> oilDeposit
    ) {
        if (level == null || startPos == null || nodeLimit <= 0 || !level.isLoaded(startPos)) {
            return EMPTY;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<Long> tasks = new ArrayList<>();
        BlockPos start = startPos.immutable();
        queue.offer(start);
        visited.add(start);

        int nodesVisited = 0;
        while (!queue.isEmpty() && nodesVisited < nodeLimit) {
            BlockPos currentPos = queue.poll();
            nodesVisited++;
            if (!level.isLoaded(currentPos)) {
                continue;
            }

            BlockState currentState = level.getBlockState(currentPos);
            if (!searchBlock.test(currentState)) {
                continue;
            }
            if (oilDeposit.test(currentState)) {
                tasks.add(currentPos.asLong());
            }

            for (Direction direction : Direction.values()) {
                if (visited.size() >= nodeLimit) {
                    break;
                }
                BlockPos neighborPos = currentPos.relative(direction);
                if (visited.contains(neighborPos) || !level.isLoaded(neighborPos)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighborPos);
                if (searchBlock.test(neighborState)) {
                    BlockPos neighbor = neighborPos.immutable();
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        if (tasks.isEmpty()) {
            return EMPTY;
        }
        long[] packed = new long[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            packed[i] = tasks.get(i);
        }
        return packed;
    }
}
