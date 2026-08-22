package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class ChimneyBlock extends LargeMachineBlock implements EntityBlock {
    private final boolean industrial;

    public ChimneyBlock(Properties properties, boolean industrial) {
        super(properties, footprint(industrial), Shapes.block(), RotationBasis.MODERN_NORTH);
        this.industrial = industrial;
    }

    private static Footprint footprint(boolean industrial) {
        return Footprint.centered(1, industrial ? 23 : 13, 1);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChimneyBlockEntity(pos, state, industrial);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof ChimneyBlockEntity chimney) {
                ChimneyBlockEntity.tick(tickerLevel, pos, tickerState, chimney);
            }
        };
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

