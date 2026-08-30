package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DungeonSpawnerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Hidden, unbreakable 1.7.10 dungeon encounter trigger. */
public final class DungeonSpawnerBlock extends Block implements EntityBlock {
    public DungeonSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.DUNGEON_SPAWNER.get()
                ? (tickLevel, tickPos, tickState, blockEntity) -> DungeonSpawnerBlockEntity.tick(
                tickLevel, tickPos, tickState, (DungeonSpawnerBlockEntity) blockEntity)
                : null;
    }
}
