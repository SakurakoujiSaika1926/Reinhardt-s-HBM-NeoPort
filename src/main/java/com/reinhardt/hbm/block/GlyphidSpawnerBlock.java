package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.GlyphidSpawnerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import org.jetbrains.annotations.Nullable;

/** The three metadata variants of BlockGlyphidSpawner from 1.7.10. */
public final class GlyphidSpawnerBlock extends LegacyVariantBlock implements EntityBlock {
    public GlyphidSpawnerBlock(Properties properties) {
        super(properties, 2);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlyphidSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.GLYPHID_SPAWNER.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) ->
                GlyphidSpawnerBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                        (GlyphidSpawnerBlockEntity) entity);
    }
}
