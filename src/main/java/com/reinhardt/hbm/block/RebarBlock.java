package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.RebarBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 rebar frame. Its visual mesh is open, but the original BlockContainer
 * deliberately retained a full-cube selection and collision box.
 */
public final class RebarBlock extends Block implements EntityBlock {
    private static final VoxelShape FULL_CUBE = Shapes.block();

    public RebarBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_CUBE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_CUBE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RebarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.REBAR.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                RebarBlockEntity.tick(tickLevel, tickPos, tickState, (RebarBlockEntity) blockEntity);
    }
}
