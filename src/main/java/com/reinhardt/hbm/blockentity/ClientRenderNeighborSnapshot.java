package com.reinhardt.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-only invalidation hook for immutable renderer snapshots that depend
 * on neighbouring blocks or capabilities.  Blocks and multiblock proxies call
 * this from the vanilla neighbour/BE lifecycle hooks; renderers only consume
 * the already-published value.
 */
public interface ClientRenderNeighborSnapshot {
    void refreshClientRenderNeighborSnapshot();

    static void refreshAt(LevelReader level, BlockPos pos) {
        if (!(level instanceof Level actual) || !actual.isClientSide || !actual.hasChunkAt(pos)) {
            return;
        }
        BlockEntity blockEntity = actual.getBlockEntity(pos);
        if (blockEntity instanceof ClientRenderNeighborSnapshot snapshot) {
            snapshot.refreshClientRenderNeighborSnapshot();
        }
    }

    static void refreshAtOrDummyCore(LevelReader level, BlockPos pos) {
        if (!(level instanceof Level actual) || !actual.isClientSide || !actual.hasChunkAt(pos)) {
            return;
        }
        BlockEntity blockEntity = actual.getBlockEntity(pos);
        if (blockEntity instanceof ClientRenderNeighborSnapshot snapshot) {
            snapshot.refreshClientRenderNeighborSnapshot();
        } else if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            refreshAt(actual, dummy.getCorePos());
        }
    }
}
