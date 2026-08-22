package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FissureBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public final class FissureBlock extends Block implements EntityBlock {
    public static final BooleanProperty CRATER = BooleanProperty.create("crater");

    public FissureBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CRATER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRATER);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, (state.getValue(CRATER) ? com.reinhardt.hbm.registry.HbmBlocks.RAD_LAVA_BLOCK : com.reinhardt.hbm.registry.HbmBlocks.VOLCANIC_LAVA_BLOCK).get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FissureBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.FISSURE.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> FissureBlockEntity.tick(
                tickerLevel, pos, tickerState, (FissureBlockEntity) blockEntity)
                : null;
    }
}
