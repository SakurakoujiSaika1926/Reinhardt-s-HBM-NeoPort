package com.reinhardt.hbm.util;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;

/** Persistent, synchronized per-machine muffler state. */
public final class MachineMuffler {
    public static final float VOLUME_MULTIPLIER = 0.1F;

    private MachineMuffler() {
    }

    public static BlockEntity resolveCore(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            blockEntity = level.getBlockEntity(dummy.getCorePos());
        } else if (blockEntity instanceof SatelliteDockDummyBlockEntity dummy) {
            blockEntity = level.getBlockEntity(dummy.corePos());
        }
        return blockEntity;
    }

    public static boolean isMuffled(BlockEntity blockEntity) {
        return blockEntity != null && blockEntity.getData(HbmDataAttachments.MUFFLED_MACHINE);
    }

    public static boolean install(BlockEntity blockEntity) {
        if (blockEntity == null
                || !ReinhardtsHBM.MOD_ID.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).getNamespace())
                || isMuffled(blockEntity)) {
            return false;
        }
        blockEntity.setData(HbmDataAttachments.MUFFLED_MACHINE, true);
        blockEntity.setChanged();
        Level level = blockEntity.getLevel();
        if (level != null) {
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(),
                    blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public static float attenuate(BlockEntity blockEntity, float volume) {
        return isMuffled(blockEntity) ? volume * VOLUME_MULTIPLIER : volume;
    }
}
