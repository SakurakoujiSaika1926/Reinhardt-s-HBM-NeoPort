package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SolarMirrorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SolarMirrorBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 0.125D, 0.875D),
            Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D),
            Shapes.box(0.0625D, 0.875D, 0.0625D, 0.9375D, 1.0D, 0.9375D)
    );

    public SolarMirrorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarMirrorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.SOLAR_MIRROR.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> SolarMirrorBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (SolarMirrorBlockEntity) blockEntity
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
