package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class OreSlopperBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 3, -3, 3);
    private static final VoxelShape FULL_BLOCK = Shapes.block();
    private static final double[][] LEGACY_BOXES_1710 = {
            {-3.5D, 0.0D, -1.5D, 3.5D, 1.0D, 1.5D},
            {0.5D, 1.0D, -1.5D, 3.5D, 3.25D, 1.5D},
            {-2.25D, 1.0D, -1.5D, 0.25D, 3.25D, -0.75D},
            {-2.25D, 1.0D, 0.75D, 0.25D, 3.25D, 1.5D},
            {-2.25D, 1.0D, -1.5D, -2.0D, 3.25D, 1.5D},
            {0.0D, 1.0D, -1.5D, 0.25D, 3.25D, 1.5D},
            {-2.0D, 1.0D, -0.75D, 0.0D, 2.0D, 0.75D},
            {-3.25D, 1.0D, -1.0D, -2.25D, 3.0D, 1.0D}
    };

    public OreSlopperBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreSlopperBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK;
    }

    static VoxelShape shapeForWorldOffset(Direction facing, BlockPos worldOffset) {
        VoxelShape shape = Shapes.empty();
        for (double[] legacyBox : LEGACY_BOXES_1710) {
            double[] box = legacyAabbToNeoObjAabb(legacyBox);
            double[] rotated = rotateBoxLikeVerifiedTurbineFacing(box, facing);
            shape = Shapes.or(shape, Shapes.box(
                    rotated[0] + 0.5D - worldOffset.getX(),
                    rotated[1] - worldOffset.getY(),
                    rotated[2] + 0.5D - worldOffset.getZ(),
                    rotated[3] + 0.5D - worldOffset.getX(),
                    rotated[4] - worldOffset.getY(),
                    rotated[5] + 0.5D - worldOffset.getZ()
            ));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    public static Direction legacyDirFromFacing(Direction facing) {
        return facing;
    }

    private static double[] legacyAabbToNeoObjAabb(double[] box) {
        return new double[]{box[2], box[1], box[0], box[5], box[4], box[3]};
    }

    private static double[] rotateBoxLikeVerifiedTurbineFacing(double[] box, Direction facing) {
        return switch (facing) {
            case EAST -> new double[]{-box[5], box[1], box[0], -box[2], box[4], box[3]};
            case NORTH -> new double[]{-box[3], box[1], -box[5], -box[0], box[4], -box[2]};
            case WEST -> new double[]{box[2], box[1], -box[3], box[5], box[4], -box[0]};
            default -> new double[]{box[0], box[1], box[2], box[3], box[4], box[5]};
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof OreSlopperBlockEntity slopper) {
                OreSlopperBlockEntity.tick(tickerLevel, pos, tickerState, slopper);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

