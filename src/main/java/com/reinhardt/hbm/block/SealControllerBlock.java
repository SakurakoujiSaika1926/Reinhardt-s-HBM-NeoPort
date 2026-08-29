package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SealHatchBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 BlockSeal frame scan and hatch-fill behavior. */
public final class SealControllerBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private static final int MAX_FRAME_RADIUS = 6;

    public SealControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            toggle(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered) {
                toggle(level, pos, state);
            }
        }
    }

    public static boolean toggle(Level level, BlockPos controllerPos, BlockState controllerState) {
        int radius = frameRadius(level, controllerPos, controllerState.getValue(FACING));
        if (radius == 0) {
            return false;
        }
        BlockPos center = frameCenter(controllerPos, controllerState.getValue(FACING), radius);
        boolean closed = isClosed(level, center, radius);
        for (int x = -radius + 1; x <= radius - 1; x++) {
            for (int z = -radius + 1; z <= radius - 1; z++) {
                BlockPos target = center.offset(x, 0, z);
                if (closed) {
                    if (level.getBlockState(target).is(HbmBlocks.SEAL_HATCH.get())) {
                        level.removeBlock(target, false);
                    }
                } else if (level.getBlockState(target).isAir()) {
                    level.setBlock(target, HbmBlocks.SEAL_HATCH.get().defaultBlockState(), Block.UPDATE_ALL);
                    if (level.getBlockEntity(target) instanceof SealHatchBlockEntity hatch) {
                        hatch.setControllerPos(controllerPos);
                    }
                }
            }
        }
        return true;
    }

    private static int frameRadius(Level level, BlockPos controllerPos, Direction facing) {
        for (int radius = 1; radius <= MAX_FRAME_RADIUS; radius++) {
            BlockPos center = frameCenter(controllerPos, facing, radius);
            boolean valid = true;
            for (int delta = -radius; delta <= radius && valid; delta++) {
                valid = isFrame(level, center.offset(delta, 0, radius))
                        && isFrame(level, center.offset(delta, 0, -radius))
                        && isFrame(level, center.offset(radius, 0, delta))
                        && isFrame(level, center.offset(-radius, 0, delta));
            }
            if (valid) {
                return radius;
            }
        }
        return 0;
    }

    public static boolean hasValidFrame(Level level, BlockPos controllerPos) {
        BlockState state = level.getBlockState(controllerPos);
        return state.is(HbmBlocks.SEAL_CONTROLLER.get())
                && frameRadius(level, controllerPos, state.getValue(FACING)) != 0;
    }

    private static BlockPos frameCenter(BlockPos controllerPos, Direction facing, int radius) {
        return controllerPos.relative(facing, radius);
    }

    private static boolean isFrame(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(HbmBlocks.SEAL_FRAME.get()) || state.is(HbmBlocks.SEAL_CONTROLLER.get());
    }

    private static boolean isClosed(Level level, BlockPos center, int radius) {
        for (int x = -radius + 1; x <= radius - 1; x++) {
            for (int z = -radius + 1; z <= radius - 1; z++) {
                if (level.getBlockState(center.offset(x, 0, z)).is(HbmBlocks.SEAL_HATCH.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
}
