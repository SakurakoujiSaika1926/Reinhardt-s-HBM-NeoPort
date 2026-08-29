package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.VolcanoCoreBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/** The 1.7.10 volcano core; its five metadata modes are a block-state property. */
public final class VolcanoCoreBlock extends Block implements EntityBlock {
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 4);

    private final boolean radioactive;

    public VolcanoCoreBlock(Properties properties, boolean radioactive) {
        super(properties);
        this.radioactive = radioactive;
        registerDefaultState(stateDefinition.any().setValue(MODE, 0));
    }

    public boolean radioactive() {
        return radioactive;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanoCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.VOLCANO_CORE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> VolcanoCoreBlockEntity.tick(
                tickerLevel, pos, tickerState, (VolcanoCoreBlockEntity) blockEntity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }
}
